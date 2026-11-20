package com.example.bot

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
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
            } catch (e: Exception) {
                Log.e(TAG, "Error handling WhatsApp notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun findDirectReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        return actions.firstOrNull { action ->
            val inputs = action.remoteInputs
            inputs != null && inputs.isNotEmpty() && action.actionIntent != null
        }
    }

    private fun extractIncomingMessage(notification: Notification): ExtractedNotification? {
        val extras = notification.extras ?: return null

        val isGroupFlag = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()

        // Prefer MessagingStyle latest message for accuracy on stacked WhatsApp notifications
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val latestStyleMessage = messagingStyle?.messages?.lastOrNull()

        val messageText = latestStyleMessage?.text?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: return null

        if (messageText.isBlank()) return null

        // Ignore WhatsApp self/system placeholder messages
        val lowerText = messageText.lowercase()
        if (lowerText.contains("pesan baru") && lowerText.contains("chat")) return null
        if (lowerText.startsWith("anda:") || lowerText.startsWith("you:")) return null

        // Detect group chats reliably across WhatsApp versions
        val isGroupByColon = rawTitle.contains(":") && !rawTitle.startsWith("+")
        val isGroupByStyle = messagingStyle?.isGroupConversation == true
        val isGroup = isGroupFlag || isGroupByStyle || !conversationTitle.isNullOrBlank() || isGroupByColon

        val senderName = when {
            !latestStyleMessage?.person?.name.isNullOrBlank() -> latestStyleMessage?.person?.name.toString().trim()
            rawTitle.isNotBlank() -> rawTitle.substringBefore(" (").trim()
            else -> "Pelanggan WA"
        }

        // Ignore own reply echoed back by WhatsApp
        if (senderName.equals("Anda", ignoreCase = true) || senderName.equals("You", ignoreCase = true)) {
            return null
        }

        return ExtractedNotification(
            senderName = senderName,
            messageText = messageText,
            isGroupChat = isGroup,
        )
    }

    private fun dispatchDirectReply(action: Notification.Action, replyText: String): Boolean {
        val remoteInputs = action.remoteInputs ?: return false
        val pendingIntent = action.actionIntent ?: return false
        return runCatching {
            val replyIntent = Intent()
            val resultsBundle = Bundle()
            for (input in remoteInputs) {
                resultsBundle.putCharSequence(input.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, replyIntent, resultsBundle)
            pendingIntent.send(applicationContext, 0, replyIntent)
            true
        }.onFailure { err ->
            Log.e(TAG, "Direct reply dispatch failed", err)
        }.getOrDefault(false)
    }

    private data class ExtractedNotification(
        val senderName: String,
        val messageText: String,
        val isGroupChat: Boolean,
    )

    companion object {
        private const val TAG = "KalaOrderNotifBot"
        const val PACKAGE_WHATSAPP = "com.whatsapp"
        const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    }
}
