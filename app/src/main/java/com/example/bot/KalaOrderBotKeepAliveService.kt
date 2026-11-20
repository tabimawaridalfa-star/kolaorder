package com.example.bot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.data.KalaOrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Persistent 24-hour Foreground Service (specialUse) that keeps KalaOrder alive
 * outside the app, across Recent Apps swipes, and while the phone screen is off.
 */
class KalaOrderBotKeepAliveService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { KalaOrderRepository.fromContext(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        BotNotificationChannels.ensureChannels(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        // Immediately enter foreground to satisfy Android 5-second FGS contract
        promoteToForeground(
            storeName = "Warung Nusantara",
            storeOpen = true,
            botActive = true,
        )
        _isRunning.value = true

        // Update notification text with live Room settings & ensure NotificationListener stays bound
        serviceScope.launch {
            runCatching {
                val settings = repository.getBotSettingsSnapshot()
                promoteToForeground(
                    storeName = settings.storeName,
                    storeOpen = settings.storeOpen,
                    botActive = settings.botActive,
                )
                if (NotificationAccessHelper.isNotificationAccessEnabled(applicationContext)) {
                    NotificationAccessHelper.requestListenerRebind(applicationContext)
                }
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Re-request listener binding if user swipes the app away from Recents
        if (NotificationAccessHelper.isNotificationAccessEnabled(applicationContext)) {
            NotificationAccessHelper.requestListenerRebind(applicationContext)
        }
    }

    override fun onDestroy() {
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground(
        storeName: String,
        storeOpen: Boolean,
        botActive: Boolean,
    ) {
        val notification = BotNotificationChannels.buildKeepAliveNotification(
            context = applicationContext,
            storeName = storeName,
            storeOpen = storeOpen,
            botActive = botActive,
        )
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this,
                BotNotificationChannels.KEEP_ALIVE_NOTIFICATION_ID,
                notification,
                fgsType,
            )
        }.onFailure { err ->
            Log.e(TAG, "Failed to promote keep-alive service to foreground", err)
        }
    }

    private fun stopSelfCleanly() {
        _isRunning.value = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "KalaOrderKeepAliveFGS"
        private const val ACTION_START = "com.example.bot.ACTION_START_KEEP_ALIVE"
        private const val ACTION_STOP = "com.example.bot.ACTION_STOP_KEEP_ALIVE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startOrUpdate(context: Context): Boolean {
            return runCatching {
                val intent = Intent(context.applicationContext, KalaOrderBotKeepAliveService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context.applicationContext, intent)
                true
            }.onFailure { err ->
                Log.w(TAG, "Unable to start foreground keep-alive service right now", err)
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            runCatching {
                val intent = Intent(context.applicationContext, KalaOrderBotKeepAliveService::class.java).apply {
                    action = ACTION_STOP
                }
                context.applicationContext.startService(intent)
            }
            _isRunning.value = false
        }
    }
}
