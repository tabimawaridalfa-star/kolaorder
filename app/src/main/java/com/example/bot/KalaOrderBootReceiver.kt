package com.example.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Automatically restores the Notification Listener binding and 24-hour Keep-Alive service
 * after the device reboots or the app package is updated.
 */
class KalaOrderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        Log.i(TAG, "Received $action — restoring KalaOrder WA Notification Bot")
        if (NotificationAccessHelper.isNotificationAccessEnabled(context)) {
            NotificationAccessHelper.requestListenerRebind(context)
            KalaOrderBotKeepAliveService.startOrUpdate(context)
        }
    }

    companion object {
        private const val TAG = "KalaOrderBootReceiver"
    }
}
