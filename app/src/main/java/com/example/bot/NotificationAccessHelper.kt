package com.example.bot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object NotificationAccessHelper {

    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false

        val expectedComponent = ComponentName(
            context.packageName,
            KalaOrderNotificationListenerService::class.java.name,
        ).flattenToString()

        return enabledListeners.split(":").any { entry ->
            entry.equals(expectedComponent, ignoreCase = true) ||
                entry.contains(context.packageName, ignoreCase = true)
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openWhatsAppChat(context: Context, rawPhone: String, message: String): Boolean {
        val digits = rawPhone.filter { it.isDigit() }
        val normalizedPhone = when {
            digits.startsWith("0") -> "62${digits.drop(1)}"
            digits.startsWith("62") -> digits
            digits.length >= 9 -> "62$digits"
            else -> ""
        }
        val encodedText = Uri.encode(message)
        val uri = if (normalizedPhone.isNotEmpty()) {
            Uri.parse("https://wa.me/$normalizedPhone?text=$encodedText")
        } else {
            Uri.parse("https://wa.me/?text=$encodedText")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
