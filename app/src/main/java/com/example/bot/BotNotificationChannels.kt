package com.example.bot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.RupiahFormatter

object BotNotificationChannels {
    const val CHANNEL_KEEP_ALIVE = "kalaorder_bot_keepalive"
    const val CHANNEL_NEW_ORDERS = "kalaorder_new_orders"
    const val KEEP_ALIVE_NOTIFICATION_ID = 1001

    private val ORDER_VIBRATION_PATTERN = longArrayOf(0L, 260L, 120L, 320L)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. Silent low-priority channel for 24h persistent Foreground Service
        val keepAliveChannel = NotificationChannel(
            CHANNEL_KEEP_ALIVE,
            "Siaga Bot WhatsApp 24 Jam",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Menjaga Bot Notifikasi WA tetap aktif di luar aplikasi dan saat layar HP mati."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        // 2. High-priority sound + vibration channel for incoming WA orders
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val orderChannel = NotificationChannel(
            CHANNEL_NEW_ORDERS,
            "Pesanan WhatsApp Masuk",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Bunyi dan getar setiap kali pesanan WhatsApp baru masuk ke antrean dapur."
            enableVibration(true)
            vibrationPattern = ORDER_VIBRATION_PATTERN
            enableLights(true)
            setSound(defaultSoundUri, audioAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(keepAliveChannel)
        manager.createNotificationChannel(orderChannel)
    }

    fun buildKeepAliveNotification(
        context: Context,
        storeName: String,
        storeOpen: Boolean,
        botActive: Boolean,
    ): Notification {
        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when {
            !botActive -> "KalaOrder Bot WA Dijeda"
            !storeOpen -> "KalaOrder Siaga • TOKO TUTUP"
            else -> "KalaOrder Bot WA Siaga 24 Jam"
        }
        val subtitle = when {
            !botActive -> "$storeName • Ketuk untuk mengaktifkan kembali"
            !storeOpen -> "$storeName • Otomatis balas pesan toko tutup"
            else -> "$storeName • Membaca & membalas notif WA otomatis"
        }

        return NotificationCompat.Builder(context, CHANNEL_KEEP_ALIVE)
            .setSmallIcon(R.drawable.ic_stat_kalaorder)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    fun notifyNewOrder(
        context: Context,
        orderCode: String,
        customerName: String,
        totalAmount: Int,
        itemSummary: String,
    ) {
        if (!NotificationAccessHelper.isPostNotificationsGranted(context)) return
        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            orderCode.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val formattedTotal = RupiahFormatter.format(totalAmount)

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_kalaorder)
            .setContentTitle("Order Baru $orderCode • $formattedTotal")
            .setContentText("$customerName: $itemSummary")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Pelanggan: $customerName\n$itemSummary\nTotal: $formattedTotal")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSoundUri)
            .setVibrate(ORDER_VIBRATION_PATTERN)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(
                2000 + (System.currentTimeMillis() % 10_000).toInt(),
                notification,
            )
        }
    }
}
