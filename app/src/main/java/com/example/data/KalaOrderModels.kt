package com.example.data

import java.text.NumberFormat
import java.util.Locale

enum class OrderStatus(val label: String) {
    MENUNGGU("Menunggu"),
    DIPROSES("Diproses"),
    SELESAI("Selesai"),
    DIBATALKAN("Dibatalkan");

    companion object {
        fun fromKey(key: String): OrderStatus =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: MENUNGGU
    }
}

enum class OrderChannel(val label: String) {
    WHATSAPP("WhatsApp"),
    KASIR("Kasir");

    companion object {
        fun fromKey(key: String): OrderChannel =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: WHATSAPP
    }
}

enum class MenuCategory(val label: String) {
    MAKANAN("Makanan"),
    MINUMAN("Minuman"),
    CAMILAN("Camilan");

    companion object {
        fun fromKey(key: String): MenuCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: MAKANAN
    }
}

enum class BotLogOutcome(val label: String) {
    SENT_WELCOME("Balas Menu"),
    CREATED_ORDER("Order Masuk"),
    OUT_OF_STOCK("Stok Habis"),
    SENT_CLOSED("Toko Tutup"),
    IGNORED_GROUP("Abaikan Grup"),
    IGNORED_COOLDOWN("Cooldown"),
    FAILED_REPLY("Gagal Balas");

    companion object {
        fun fromKey(key: String): BotLogOutcome =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: SENT_WELCOME
    }
}

data class OrderLineItem(
    val menuCode: String,
    val menuName: String,
    val quantity: Int,
    val unitPrice: Int,
) {
    val subtotal: Int get() = quantity * unitPrice
}

data class OrderRecord(
    val id: Long,
    val orderCode: String,
    val customerName: String,
    val customerPhone: String,
    val channel: OrderChannel,
    val status: OrderStatus,
    val items: List<OrderLineItem>,
    val totalAmount: Int,
    val note: String,
    val timestampLabel: String,
    val createdAtMillis: Long,
) {
    val totalItemCount: Int get() = items.sumOf { it.quantity }
}

data class MenuItemRecord(
    val code: String,
    val name: String,
    val category: MenuCategory,
    val price: Int,
    val stock: Int,
    val isAvailable: Boolean,
) {
    val isLowStock: Boolean get() = stock in 1..5
    val isOutOfStock: Boolean get() = stock <= 0 || !isAvailable
}

data class BotSettings(
    val storeName: String = "Warung Nusantara",
    val storeOpen: Boolean = true,
    val botActive: Boolean = true,
    val autoReplyEnabled: Boolean = true,
    val autoNotifyReady: Boolean = true,
    val whatsappNumber: String = "+62 812-3456-7890",
    val greetingTemplate: String = "Terima kasih Kak! Pesanan #{KODE} sudah masuk antrean dapur {TOKO}. Mohon tunggu konfirmasi kami ya.",
    val listenWhatsApp: Boolean = true,
    val listenWhatsAppBusiness: Boolean = true,
    val ignoreGroupChats: Boolean = true,
    val welcomeCooldownMinutes: Int = 30,
    val welcomeMessageTemplate: String = "Selamat datang di {TOKO}! 👋\nSilakan pilih menu favorit Kakak hari ini:",
    val closedStoreTemplate: String = "Mohon maaf Kak, {TOKO} saat ini sedang TUTUP. Pesanan akan kami layani kembali saat toko buka ya!",
)

data class BotActivityLogRecord(
    val id: Long,
    val senderName: String,
    val sourceApp: String,
    val incomingMessage: String,
    val replyPreview: String,
    val outcome: BotLogOutcome,
    val timestampLabel: String,
    val createdAtMillis: Long,
)

object RupiahFormatter {
    private val indonesianLocale = Locale("in", "ID")

    fun format(amount: Int): String {
        val formatter = NumberFormat.getIntegerInstance(indonesianLocale)
        return "Rp ${formatter.format(amount)}"
    }

    fun formatCompact(amount: Int): String {
        return when {
            amount >= 1_000_000 -> {
                val jt = amount / 1_000_000.0
                "Rp ${String.format(indonesianLocale, "%.1f", jt)} jt"
            }
            else -> format(amount)
        }
    }
}
