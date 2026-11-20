package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Computes period-filtered financial summaries, chart buckets, category breakdowns,
 * WhatsApp text recaps, and UTF-8 BOM CSV exports (Excel-ready) with creator attribution.
 */
object FinanceRecapEngine {
    private val localeId = Locale("in", "ID")
    private const val DAY_MILLIS = 86_400_000L

    fun computeSummary(
        orders: List<OrderRecord>,
        cashflowEntries: List<CashflowEntryRecord>,
        period: FinancePeriod,
        selectedMonth: FinanceYearMonth,
        searchQuery: String = "",
        nowMillis: Long = System.currentTimeMillis(),
    ): FinanceSummaryState {
        val (rangeStart, rangeEndExclusive, periodLabel) = resolveRange(
            period = period,
            selectedMonth = selectedMonth,
            nowMillis = nowMillis,
        )

        val completedOrdersInRange = orders
            .filter { it.status == OrderStatus.SELESAI }
            .filter { it.createdAtMillis in rangeStart until rangeEndExclusive }
            .sortedByDescending { it.createdAtMillis }

        val manualEntriesInRange = cashflowEntries
            .filter { it.createdAtMillis in rangeStart until rangeEndExclusive }
            .sortedByDescending { it.createdAtMillis }

        val totalOrderIncome = completedOrdersInRange.sumOf { it.totalAmount }
        val otherIncomes = manualEntriesInRange.filter { it.type == CashflowType.OTHER_INCOME }
        val expenses = manualEntriesInRange.filter { it.type == CashflowType.EXPENSE }
        val capitals = manualEntriesInRange.filter { it.type == CashflowType.CAPITAL }

        val totalOtherIncome = otherIncomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val totalCapital = capitals.sumOf { it.amount }

        val totalOperatingIncome = totalOrderIncome + totalOtherIncome
        val netProfit = totalOperatingIncome - totalExpense
        val netCashBalance = totalCapital + netProfit
        val profitMarginPercent = if (totalOperatingIncome > 0) {
            ((netProfit.toDouble() / totalOperatingIncome.toDouble()) * 100.0).roundToInt()
        } else {
            0
        }

        val expenseByCategory = expenses
            .groupBy { it.category.ifBlank { "Lainnya" } }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }
                CategoryExpenseBreakdown(
                    category = category,
                    amount = sum,
                    ratio = if (totalExpense > 0) (sum.toFloat() / totalExpense.toFloat()).coerceIn(0f, 1f) else 0f,
                )
            }
            .sortedByDescending { it.amount }

        val chartPoints = buildChartPoints(
            period = period,
            selectedMonth = selectedMonth,
            completedOrders = completedOrdersInRange,
            manualEntries = manualEntriesInRange,
            nowMillis = nowMillis,
        )

        val q = searchQuery.trim()
        val unifiedLedger = buildList {
            completedOrdersInRange.forEach { order ->
                if (q.isEmpty() ||
                    order.orderCode.contains(q, ignoreCase = true) ||
                    order.customerName.contains(q, ignoreCase = true) ||
                    order.items.any { it.menuName.contains(q, ignoreCase = true) }
                ) {
                    add(FinanceLedgerItem.OrderSaleItem(order))
                }
            }
            manualEntriesInRange.forEach { entry ->
                if (q.isEmpty() ||
                    entry.title.contains(q, ignoreCase = true) ||
                    entry.category.contains(q, ignoreCase = true) ||
                    entry.note.contains(q, ignoreCase = true) ||
                    entry.type.label.contains(q, ignoreCase = true)
                ) {
                    add(FinanceLedgerItem.ManualCashflowItem(entry))
                }
            }
        }.sortedByDescending { it.sortMillis }

        return FinanceSummaryState(
            period = period,
            selectedMonth = selectedMonth,
            periodLabel = periodLabel,
            totalOrderIncome = totalOrderIncome,
            completedOrderCount = completedOrdersInRange.size,
            totalOtherIncome = totalOtherIncome,
            otherIncomeCount = otherIncomes.size,
            totalExpense = totalExpense,
            expenseCount = expenses.size,
            totalCapital = totalCapital,
            capitalCount = capitals.size,
            netProfit = netProfit,
            netCashBalance = netCashBalance,
            profitMarginPercent = profitMarginPercent,
            completedOrders = completedOrdersInRange,
            manualEntries = manualEntriesInRange,
            expenseByCategory = expenseByCategory,
            chartPoints = chartPoints,
            ledgerItems = unifiedLedger,
        )
    }

    fun buildWhatsAppRecapText(
        summary: FinanceSummaryState,
        storeName: String,
    ): String {
        val printedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", localeId).format(Date())
        return buildString {
            appendLine("*REKAP KEUANGAN ${storeName.uppercase(localeId)}*")
            appendLine("Periode: ${summary.periodLabel}")
            appendLine("Dicetak: $printedAt")
            appendLine("────────────────────────")
            appendLine("• Pemasukan Order (${summary.completedOrderCount}x): ${RupiahFormatter.format(summary.totalOrderIncome)}")
            if (summary.totalOtherIncome > 0) {
                appendLine("• Pemasukan Lain (${summary.otherIncomeCount}x): ${RupiahFormatter.format(summary.totalOtherIncome)}")
            }
            appendLine("• Total Pengeluaran (${summary.expenseCount}x): ${RupiahFormatter.format(summary.totalExpense)}")
            appendLine("────────────────────────")
            appendLine("*Laba Bersih: ${RupiahFormatter.formatSigned(summary.netProfit)}*")
            appendLine("Margin Keuntungan: ${summary.profitMarginPercent}%")
            if (summary.totalCapital > 0) {
                appendLine("Modal Usaha Tercatat: ${RupiahFormatter.format(summary.totalCapital)}")
                appendLine("Saldo Kas Total: ${RupiahFormatter.format(summary.netCashBalance)}")
            }
            if (summary.expenseByCategory.isNotEmpty()) {
                appendLine()
                appendLine("*Rincian Pengeluaran Terbesar:*")
                summary.expenseByCategory.take(5).forEach { item ->
                    val pct = (item.ratio * 100f).roundToInt()
                    appendLine("- ${item.category}: ${RupiahFormatter.format(item.amount)} ($pct%)")
                }
            }
            appendLine()
            appendLine("────────────────────────")
            appendLine(CreatorInfo.ATTRIBUTION_FOOTER)
        }.trimEnd()
    }

    /**
     * Generates a UTF-8 BOM CSV file that opens cleanly in Microsoft Excel and Google Sheets.
     */
    fun buildExcelCsvContent(
        summary: FinanceSummaryState,
        storeName: String,
    ): String {
        val printedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", localeId).format(Date())
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", localeId)
        val timeFmt = SimpleDateFormat("HH:mm", localeId)

        return buildString {
            // UTF-8 BOM so Microsoft Excel detects UTF-8 encoding automatically
            append('\uFEFF')
            appendLine("LAPORAN REKAP KEUANGAN,${csv(storeName)}")
            appendLine("Periode,${csv(summary.periodLabel)}")
            appendLine("Tanggal Cetak,${csv(printedAt)}")
            appendLine("Pembuat Aplikasi,${csv(CreatorInfo.CREATOR_NAME)}")
            appendLine("Aplikasi,${csv(CreatorInfo.APP_DISPLAY_TITLE)}")
            appendLine()

            appendLine("RINGKASAN KEUANGAN,NILAI (RP),KETERANGAN")
            appendLine("Pemasukan Order Selesai,${summary.totalOrderIncome},${csv("${summary.completedOrderCount} pesanan selesai")}")
            appendLine("Pemasukan Lain,${summary.totalOtherIncome},${csv("${summary.otherIncomeCount} catatan")}")
            appendLine("Total Pemasukan Operasional,${summary.totalOperatingIncome},")
            appendLine("Total Pengeluaran,${summary.totalExpense},${csv("${summary.expenseCount} catatan")}")
            appendLine("Laba Bersih,${summary.netProfit},${csv("Margin ${summary.profitMarginPercent}%")}")
            appendLine("Modal Usaha,${summary.totalCapital},${csv("${summary.capitalCount} catatan modal")}")
            appendLine("Saldo Kas Total,${summary.netCashBalance},${csv("Modal + Laba Bersih")}")
            appendLine()

            appendLine("RINCIAN PENJUALAN ORDER SELESAI")
            appendLine("Tanggal,Waktu,Kode Order,Pelanggan,Kanal,Jumlah Item,Nominal (Rp),Catatan")
            if (summary.completedOrders.isEmpty()) {
                appendLine("Belum ada pesanan selesai pada periode ini,,,,,,,")
            } else {
                summary.completedOrders.forEach { order ->
                    val d = Date(order.createdAtMillis)
                    appendLine(
                        listOf(
                            csv(dateFmt.format(d)),
                            csv(timeFmt.format(d)),
                            csv(order.orderCode),
                            csv(order.customerName),
                            csv(order.channel.label),
                            order.totalItemCount.toString(),
                            order.totalAmount.toString(),
                            csv(order.note),
                        ).joinToString(",")
                    )
                }
            }
            appendLine()

            appendLine("RINCIAN PENGELUARAN MODAL DAN PEMASUKAN LAIN")
            appendLine("Tanggal,Jenis,Kategori,Keterangan,Nominal (Rp),Catatan")
            if (summary.manualEntries.isEmpty()) {
                appendLine("Belum ada catatan kas manual pada periode ini,,,,,")
            } else {
                summary.manualEntries.forEach { entry ->
                    appendLine(
                        listOf(
                            csv(dateFmt.format(Date(entry.createdAtMillis))),
                            csv(entry.type.label),
                            csv(entry.category),
                            csv(entry.title),
                            entry.amount.toString(),
                            csv(entry.note),
                        ).joinToString(",")
                    )
                }
            }
            appendLine()
            appendLine(csv(CreatorInfo.ATTRIBUTION_FOOTER))
        }
    }

    fun suggestedCsvFileName(summary: FinanceSummaryState): String {
        val slug = when (summary.period) {
            FinancePeriod.TODAY -> "HariIni_${SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())}"
            FinancePeriod.LAST_7_DAYS -> "7Hari_${SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())}"
            FinancePeriod.MONTHLY -> summary.selectedMonth.fileSlug()
            FinancePeriod.ALL_TIME -> "SemuaPeriode"
        }
        return "KalaOrder_Rekap_$slug.csv"
    }

    private fun resolveRange(
        period: FinancePeriod,
        selectedMonth: FinanceYearMonth,
        nowMillis: Long,
    ): Triple<Long, Long, String> {
        val todayStart = startOfDayMillis(nowMillis)
        val tomorrowStart = todayStart + DAY_MILLIS

        return when (period) {
            FinancePeriod.TODAY -> {
                val label = "Hari Ini (${SimpleDateFormat("dd MMM yyyy", localeId).format(Date(nowMillis))})"
                Triple(todayStart, tomorrowStart, label)
            }

            FinancePeriod.LAST_7_DAYS -> {
                val start = todayStart - 6 * DAY_MILLIS
                val fmt = SimpleDateFormat("dd MMM", localeId)
                val label = "7 Hari Terakhir (${fmt.format(Date(start))} – ${fmt.format(Date(nowMillis))})"
                Triple(start, tomorrowStart, label)
            }

            FinancePeriod.MONTHLY -> {
                Triple(
                    selectedMonth.startMillis(),
                    selectedMonth.endMillisExclusive(),
                    selectedMonth.displayLabel(),
                )
            }

            FinancePeriod.ALL_TIME -> {
                Triple(0L, Long.MAX_VALUE, "Semua Waktu")
            }
        }
    }

    private fun buildChartPoints(
        period: FinancePeriod,
        selectedMonth: FinanceYearMonth,
        completedOrders: List<OrderRecord>,
        manualEntries: List<CashflowEntryRecord>,
        nowMillis: Long,
    ): List<DailyCashflowPoint> {
        return when (period) {
            FinancePeriod.TODAY -> {
                // 4 operational time blocks: Pagi (00-11), Siang (11-15), Sore (15-19), Malam (19-24)
                val labels = listOf("Pagi", "Siang", "Sore", "Malam")
                val incomes = IntArray(4)
                val expenses = IntArray(4)
                val cal = Calendar.getInstance()

                fun blockIndex(millis: Long): Int {
                    cal.timeInMillis = millis
                    return when (cal.get(Calendar.HOUR_OF_DAY)) {
                        in 0..10 -> 0
                        in 11..14 -> 1
                        in 15..18 -> 2
                        else -> 3
                    }
                }

                completedOrders.forEach { incomes[blockIndex(it.createdAtMillis)] += it.totalAmount }
                manualEntries.forEach { entry ->
                    val idx = blockIndex(entry.createdAtMillis)
                    when (entry.type) {
                        CashflowType.OTHER_INCOME -> incomes[idx] += entry.amount
                        CashflowType.EXPENSE -> expenses[idx] += entry.amount
                        CashflowType.CAPITAL -> Unit
                    }
                }
                labels.indices.map { i -> DailyCashflowPoint(labels[i], incomes[i], expenses[i]) }
            }

            FinancePeriod.LAST_7_DAYS -> {
                val todayStart = startOfDayMillis(nowMillis)
                val dayFmt = SimpleDateFormat("EEE", localeId)
                (6 downTo 0).map { offset ->
                    val start = todayStart - offset * DAY_MILLIS
                    val end = start + DAY_MILLIS
                    val inc = completedOrders
                        .filter { it.createdAtMillis in start until end }
                        .sumOf { it.totalAmount } +
                        manualEntries
                            .filter { it.type == CashflowType.OTHER_INCOME && it.createdAtMillis in start until end }
                            .sumOf { it.amount }
                    val exp = manualEntries
                        .filter { it.type == CashflowType.EXPENSE && it.createdAtMillis in start until end }
                        .sumOf { it.amount }
                    DailyCashflowPoint(
                        label = dayFmt.format(Date(start)).take(3),
                        income = inc,
                        expense = exp,
                    )
                }
            }

            FinancePeriod.MONTHLY -> {
                // 4 weekly buckets inside the selected month
                val labels = listOf("Mg 1", "Mg 2", "Mg 3", "Mg 4")
                val incomes = IntArray(4)
                val expenses = IntArray(4)
                val cal = Calendar.getInstance()

                fun weekIndex(millis: Long): Int {
                    cal.timeInMillis = millis
                    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                    return ((dayOfMonth - 1) / 8).coerceIn(0, 3)
                }

                completedOrders.forEach { incomes[weekIndex(it.createdAtMillis)] += it.totalAmount }
                manualEntries.forEach { entry ->
                    val idx = weekIndex(entry.createdAtMillis)
                    when (entry.type) {
                        CashflowType.OTHER_INCOME -> incomes[idx] += entry.amount
                        CashflowType.EXPENSE -> expenses[idx] += entry.amount
                        CashflowType.CAPITAL -> Unit
                    }
                }
                labels.indices.map { i -> DailyCashflowPoint(labels[i], incomes[i], expenses[i]) }
            }

            FinancePeriod.ALL_TIME -> {
                // Last 6 months ending at current month
                val current = FinanceYearMonth.current()
                val shortMonthFmt = SimpleDateFormat("MMM", localeId)
                (5 downTo 0).map { offset ->
                    val ym = current.shiftMonths(-offset)
                    val start = ym.startMillis()
                    val end = ym.endMillisExclusive()
                    val inc = completedOrders
                        .filter { it.createdAtMillis in start until end }
                        .sumOf { it.totalAmount } +
                        manualEntries
                            .filter { it.type == CashflowType.OTHER_INCOME && it.createdAtMillis in start until end }
                            .sumOf { it.amount }
                    val exp = manualEntries
                        .filter { it.type == CashflowType.EXPENSE && it.createdAtMillis in start until end }
                        .sumOf { it.amount }
                    DailyCashflowPoint(
                        label = shortMonthFmt.format(Date(start)).take(3),
                        income = inc,
                        expense = exp,
                    )
                }
            }
        }
    }

    private fun startOfDayMillis(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun csv(raw: String): String {
        val cleaned = raw.replace("\r", " ").replace("\n", " ")
        return if (cleaned.contains(',') || cleaned.contains('"')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }
}
