package com.example.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

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
    val storeName: String = "Usaha Saya",
    val storeOpen: Boolean = true,
    val botActive: Boolean = true,
    val autoReplyEnabled: Boolean = true,
    val autoNotifyReady: Boolean = true,
    val whatsappNumber: String = "",
    val greetingTemplate: String = "Terima kasih Kak! Pesanan #{KODE} sudah masuk antrean dapur {TOKO}. Mohon tunggu konfirmasi kami ya.",
    val listenWhatsApp: Boolean = true,
    val listenWhatsAppBusiness: Boolean = true,
    val ignoreGroupChats: Boolean = true,
    val welcomeCooldownMinutes: Int = 30,
    val welcomeMessageTemplate: String = "Selamat datang di {TOKO}! 👋\nSilakan pilih menu favorit Kakak hari ini:",
    val closedStoreTemplate: String = "Mohon maaf Kak, {TOKO} saat ini sedang TUTUP. Pesanan akan kami layani kembali saat toko buka ya!",
    val setupCompleted: Boolean = false,
)

data class DataResetSelection(
    val clearOrders: Boolean = true,
    val clearCashflow: Boolean = true,
    val clearMenu: Boolean = false,
    val clearBotLogs: Boolean = true,
) {
    val hasAnySelection: Boolean
        get() = clearOrders || clearCashflow || clearMenu || clearBotLogs
}

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

object CreatorInfo {
    const val CREATOR_NAME = "Alfa Tabi Mawarid"
    const val APP_DISPLAY_TITLE = "KalaOrder Kasir UMKM"
    const val ATTRIBUTION_FOOTER = "Dibuat oleh Alfa Tabi Mawarid • KalaOrder Kasir UMKM"
    const val QRIS_MERCHANT_NAME = "KolaOrder, Service"
    const val QRIS_NMID = "ID1026644056826"
}

enum class CashflowType(val label: String) {
    EXPENSE("Pengeluaran"),
    CAPITAL("Modal Usaha"),
    OTHER_INCOME("Pemasukan Lain");

    companion object {
        fun fromKey(key: String): CashflowType =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: EXPENSE
    }
}

object CashflowCategories {
    val expenseCategories = listOf(
        "Bahan Baku",
        "Kemasan",
        "Gas & Listrik",
        "Operasional",
        "Gaji",
        "Lainnya",
    )

    val capitalCategories = listOf(
        "Modal Awal",
        "Tambah Modal",
        "Peralatan Dapur",
        "Cadangan Kas",
    )

    val otherIncomeCategories = listOf(
        "Catering / Pesanan Khusus",
        "Titipan Produk",
        "Pemasukan Lain",
    )

    fun forType(type: CashflowType): List<String> = when (type) {
        CashflowType.EXPENSE -> expenseCategories
        CashflowType.CAPITAL -> capitalCategories
        CashflowType.OTHER_INCOME -> otherIncomeCategories
    }
}

data class CashflowEntryRecord(
    val id: Long = 0L,
    val type: CashflowType,
    val category: String,
    val title: String,
    val amount: Int,
    val note: String,
    val dateLabel: String,
    val createdAtMillis: Long,
)

enum class FinancePeriod(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    MONTHLY("Per Bulan"),
    ALL_TIME("Semua"),
}

data class FinanceYearMonth(
    val year: Int,
    val monthZeroBased: Int,
) {
    fun shiftMonths(delta: Int): FinanceYearMonth {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        return FinanceYearMonth(
            year = cal.get(Calendar.YEAR),
            monthZeroBased = cal.get(Calendar.MONTH),
        )
    }

    fun startMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endMillisExclusive(): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, 1)
    }.timeInMillis

    fun displayLabel(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale("in", "ID")).format(cal.time)
    }

    fun fileSlug(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMyyyy", Locale.US).format(cal.time)
    }

    companion object {
        fun current(): FinanceYearMonth {
            val cal = Calendar.getInstance()
            return FinanceYearMonth(
                year = cal.get(Calendar.YEAR),
                monthZeroBased = cal.get(Calendar.MONTH),
            )
        }
    }
}

sealed interface FinanceLedgerItem {
    val sortMillis: Long
    val uniqueKey: String

    data class OrderSaleItem(
        val order: OrderRecord,
    ) : FinanceLedgerItem {
        override val sortMillis: Long get() = order.createdAtMillis
        override val uniqueKey: String get() = "order_${order.id}"
    }

    data class ManualCashflowItem(
        val entry: CashflowEntryRecord,
    ) : FinanceLedgerItem {
        override val sortMillis: Long get() = entry.createdAtMillis
        override val uniqueKey: String get() = "cashflow_${entry.id}"
    }
}

data class CategoryExpenseBreakdown(
    val category: String,
    val amount: Int,
    val ratio: Float,
)

data class DailyCashflowPoint(
    val label: String,
    val income: Int,
    val expense: Int,
)

data class FinanceSummaryState(
    val period: FinancePeriod = FinancePeriod.MONTHLY,
    val selectedMonth: FinanceYearMonth = FinanceYearMonth.current(),
    val periodLabel: String = "",
    val totalOrderIncome: Int = 0,
    val completedOrderCount: Int = 0,
    val totalOtherIncome: Int = 0,
    val otherIncomeCount: Int = 0,
    val totalExpense: Int = 0,
    val expenseCount: Int = 0,
    val totalCapital: Int = 0,
    val capitalCount: Int = 0,
    val netProfit: Int = 0,
    val netCashBalance: Int = 0,
    val profitMarginPercent: Int = 0,
    val completedOrders: List<OrderRecord> = emptyList(),
    val manualEntries: List<CashflowEntryRecord> = emptyList(),
    val expenseByCategory: List<CategoryExpenseBreakdown> = emptyList(),
    val chartPoints: List<DailyCashflowPoint> = emptyList(),
    val ledgerItems: List<FinanceLedgerItem> = emptyList(),
) {
    val totalOperatingIncome: Int get() = totalOrderIncome + totalOtherIncome
    val totalTransactionCount: Int get() = completedOrderCount + manualEntries.size
}

object RupiahFormatter {
    private val indonesianLocale = Locale("in", "ID")

    fun format(amount: Int): String {
        val formatter = NumberFormat.getIntegerInstance(indonesianLocale)
        return if (amount < 0) {
            "-Rp ${formatter.format(abs(amount.toLong()))}"
        } else {
            "Rp ${formatter.format(amount)}"
        }
    }

    fun formatSigned(amount: Int): String {
        val formatter = NumberFormat.getIntegerInstance(indonesianLocale)
        return when {
            amount > 0 -> "+Rp ${formatter.format(amount)}"
            amount < 0 -> "-Rp ${formatter.format(abs(amount.toLong()))}"
            else -> "Rp 0"
        }
    }

    fun formatCompact(amount: Int): String {
        val sign = if (amount < 0) "-" else ""
        val positive = abs(amount.toLong())
        return when {
            positive >= 1_000_000L -> {
                val jt = positive / 1_000_000.0
                "${sign}Rp ${String.format(indonesianLocale, "%.1f", jt)} jt"
            }
            else -> format(amount)
        }
    }
}
