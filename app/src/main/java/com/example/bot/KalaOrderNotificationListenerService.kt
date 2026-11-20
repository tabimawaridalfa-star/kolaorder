package com.example.bot

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput as CompatRemoteInput
import com.example.data.BotLogOutcome
import com.example.data.KalaOrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KalaOrderNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository by lazy { KalaOrderRepository.fromContext(applicationContext) }
    private val autoReplyEngine by lazy { WhatsAppAutoReplyEngine(repository) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListener connected — ensuring 24h keep-alive service")
        serviceScope.launch {
            runCatching {
                val settings = repository.getBotSettingsSnapshot()
                if (settings.botActive) {
                    KalaOrderBotKeepAliveService.startOrUpdate(applicationContext)
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "NotificationListener disconnected — requesting automatic rebind")
        NotificationAccessHelper.requestListenerRebind(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val pkg = statusBarNotification.packageName ?: return

        if (pkg != PACKAGE_WHATSAPP && pkg != PACKAGE_WHATSAPP_BUSINESS) {
            return
        }

        val notification = statusBarNotification.notification ?: return

        // Skip ongoing notifications (calls/media) and summary notifications ("3 messages from 2 chats")
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val replyAction = findDirectReplyAction(notification) ?: return
        val extracted = extractIncomingMessage(notification) ?: return

        val sourceLabel = if (pkg == PACKAGE_WHATSAPP_BUSINESS) "WA Business" else "WhatsApp"

        // Acquire a brief partial WakeLock so screen-off Doze does not interrupt DB write or Direct Reply
        val wakeLock = acquireBriefWakeLock()

        serviceScope.launch {
            try {
                val settings = repository.getBotSettingsSnapshot()
                val packageEnabled = when (pkg) {
                    PACKAGE_WHATSAPP -> settings.listenWhatsApp
                    PACKAGE_WHATSAPP_BUSINESS -> settings.listenWhatsAppBusiness
                    else -> false
                }
                if (!packageEnabled) return@launch

                val decision = autoReplyEngine.processIncomingMessage(
                    senderName = extracted.senderName,
                    senderPhoneOrTag = sourceLabel,
                    sourceAppLabel = sourceLabel,
                    incomingMessage = extracted.messageText,
                    isGroupChat = extracted.isGroupChat,
                )

                if (decision.shouldReply && !decision.replyText.isNullOrBlank()) {
                    val sent = dispatchDirectReply(replyAction, decision.replyText)
                    if (!sent) {
                        repository.logBotActivity(
                            senderName = extracted.senderName,
                            sourceApp = sourceLabel,
                            incomingMessage = extracted.messageText,
                            replyPreview = "Gagal mengirim Direct Reply (notifikasi mungkin sudah ditutup)",
                            outcome = BotLogOutcome.FAILED_REPLY,
                        )
                    }
                }

                // Sound + vibrate alert for new orders even while the phone screen is off
                if (decision.createdOrderCode != null && decision.createdOrderTotal != null) {
                    BotNotificationChannels.notifyNewOrder(
                        context = applicationContext,
                        orderCode = decision.createdOrderCode,
                        customerName = extracted.senderName,
                        totalAmount = decision.createdOrderTotal,
                        itemSummary = decision.createdOrderSummary.orEmpty(),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling WhatsApp notification", e)
            } finally {
                releaseWakeLockSafely(wakeLock)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun acquireBriefWakeLock(): PowerManager.WakeLock? {
        return runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
            pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "KalaOrder::WhatsAppReplyWakeLock",
            ).apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        }.getOrNull()
    }

    private fun releaseWakeLockSafely(wakeLock: PowerManager.WakeLock?) {
        runCatching {
            if (wakeLock != null && wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    /**
     * Checks both standard Notification.actions and WearableExtender actions, because
     * WhatsApp sometimes exposes its Direct Reply RemoteInput only in WearableExtender
     * when the phone screen is locked or notifications are bundled.
     */
    private fun findDirectReplyAction(notification: Notification): ReplyTarget? {
        notification.actions?.forEach { action ->
            val inputs = action.remoteInputs
            val pendingIntent = action.actionIntent
            if (inputs != null && inputs.isNotEmpty() && pendingIntent != null) {
                return ReplyTarget.Framework(pendingIntent, inputs)
            }
        }

        val wearableActions = NotificationCompat.WearableExtender(notification).actions
        wearableActions.forEach { action ->
            val inputs = action.remoteInputs
            val pendingIntent = action.actionIntent
            if (inputs != null && inputs.isNotEmpty() && pendingIntent != null) {
                return ReplyTarget.Compat(pendingIntent, inputs)
            }
        }

        return null
    }

    private fun extractIncomingMessage(notification: Notification): ExtractedNotification? {
        val extras = notification.extras ?: return null

        val isGroupFlag = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()

        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val latestStyleMessage = messagingStyle?.messages?.lastOrNull()

        val messageText = latestStyleMessage?.text?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: return null

        if (messageText.isBlank()) return null

        val lowerText = messageText.lowercase()
        if (lowerText.contains("pesan baru") && lowerText.contains("chat")) return null
        if (lowerText.startsWith("anda:") || lowerText.startsWith("you:")) return null

        val isGroupByColon = rawTitle.contains(":") && !rawTitle.startsWith("+")
        val isGroupByStyle = messagingStyle?.isGroupConversation == true
        val isGroup = isGroupFlag || isGroupByStyle || !conversationTitle.isNullOrBlank() || isGroupByColon

        val senderName = when {
            !latestStyleMessage?.person?.name.isNullOrBlank() -> latestStyleMessage?.person?.name.toString().trim()
            rawTitle.isNotBlank() -> rawTitle.substringBefore(" (").trim()
            else -> "Pelanggan WA"
        }

        if (senderName.equals("Anda", ignoreCase = true) || senderName.equals("You", ignoreCase = true)) {
            return null
        }

        return ExtractedNotification(
            senderName = senderName,
            messageText = messageText,
            isGroupChat = isGroup,
        )
    }

    private fun dispatchDirectReply(target: ReplyTarget, replyText: String): Boolean {
        return runCatching {
            val replyIntent = Intent()
            val resultsBundle = Bundle()
            when (target) {
                is ReplyTarget.Framework -> {
                    for (input in target.remoteInputs) {
                        resultsBundle.putCharSequence(input.resultKey, replyText)
                    }
                    RemoteInput.addResultsToIntent(target.remoteInputs, replyIntent, resultsBundle)
                    target.pendingIntent.send(applicationContext, 0, replyIntent)
                }
                is ReplyTarget.Compat -> {
                    for (input in target.remoteInputs) {
                        resultsBundle.putCharSequence(input.resultKey, replyText)
                    }
                    CompatRemoteInput.addResultsToIntent(target.remoteInputs, replyIntent, resultsBundle)
                    target.pendingIntent.send(applicationContext, 0, replyIntent)
                }
            }
            true
        }.onFailure { err ->
            Log.e(TAG, "Direct reply dispatch failed", err)
        }.getOrDefault(false)
    }

    private sealed class ReplyTarget {
        data class Framework(
            val pendingIntent: PendingIntent,
            val remoteInputs: Array<RemoteInput>,
        ) : ReplyTarget()

        data class Compat(
            val pendingIntent: PendingIntent,
            val remoteInputs: Array<CompatRemoteInput>,
        ) : ReplyTarget()
    }

    private data class ExtractedNotification(
        val senderName: String,
        val messageText: String,
        val isGroupChat: Boolean,
    )

    companion object {
        private const val TAG = "KalaOrderNotifBot"
        private const val WAKE_LOCK_TIMEOUT_MS = 8_000L
        const val PACKAGE_WHATSAPP = "com.whatsapp"
        const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    }
}
