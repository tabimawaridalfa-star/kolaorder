package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CashflowEntryRecord
import com.example.data.CashflowType
import com.example.data.CategoryExpenseBreakdown
import com.example.data.CreatorInfo
import com.example.data.DailyCashflowPoint
import com.example.data.FinanceLedgerItem
import com.example.data.FinancePeriod
import com.example.data.FinanceYearMonth
import com.example.data.OrderRecord
import com.example.data.RupiahFormatter
import com.example.ui.components.KalaEmptyState
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaMicroActionButton
import com.example.ui.components.KalaOrderCard
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSearchField
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.components.MonospaceCodeTag
import com.example.ui.components.SemanticPillBadge
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimarySoft
import com.example.ui.theme.StatusDibatalkanBg
import com.example.ui.theme.StatusDibatalkanBorder
import com.example.ui.theme.StatusDibatalkanText
import com.example.ui.theme.StatusDiprosesBg
import com.example.ui.theme.StatusDiprosesBorder
import com.example.ui.theme.StatusDiprosesText
import com.example.ui.theme.StatusMenungguBg
import com.example.ui.theme.StatusMenungguBorder
import com.example.ui.theme.StatusMenungguText
import com.example.ui.theme.StatusSelesaiBg
import com.example.ui.theme.StatusSelesaiBorder
import com.example.ui.theme.StatusSelesaiText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.KalaOrderUiState
import kotlin.math.roundToInt

@Composable
fun FinanceScreen(
    uiState: KalaOrderUiState,
    onSelectPeriod: (FinancePeriod) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onResetToCurrentMonth: () -> Unit,
    onSearchChange: (String) -> Unit,
    onAddCashflowClick: () -> Unit,
    onEditCashflowClick: (CashflowEntryRecord) -> Unit,
    onDeleteCashflowClick: (CashflowEntryRecord) -> Unit,
    onSelectOrder: (OrderRecord) -> Unit,
    onOpenRecapPreview: () -> Unit,
    onExportCsvClick: () -> Unit,
    onShareWhatsAppClick: () -> Unit,
    onOpenDonationSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.financeSummary
    val currentMonth = FinanceYearMonth.current()
    val isCurrentMonthSelected = summary.selectedMonth == currentMonth

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item(key = "finance_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.finance_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.finance_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                KalaPrimaryButton(
                    text = stringResource(R.string.finance_action_record),
                    onClick = onAddCashflowClick,
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.testTag("finance_add_cashflow_button"),
                )
            }
        }

        // Period Filter Pills
        item(key = "finance_period_chips") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(FinancePeriod.entries) { period ->
                    KalaFilterChip(
                        label = period.label,
                        selected = summary.period == period,
                        onClick = { onSelectPeriod(period) },
                        modifier = Modifier.testTag("finance_period_${period.name.lowercase()}"),
                    )
                }
            }
        }

        // Monthly Selector Bar when MONTHLY period is active
        if (summary.period == FinancePeriod.MONTHLY) {
            item(key = "finance_month_navigator") {
                KalaOrderCard(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = { onShiftMonth(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("finance_prev_month_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronLeft,
                                contentDescription = "Bulan sebelumnya",
                                tint = TextPrimary,
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = summary.selectedMonth.displayLabel(),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            if (!isCurrentMonthSelected) {
                                Text(
                                    text = "Kembali ke bulan ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryEmerald,
                                    modifier = Modifier
                                        .testTag("finance_jump_current_month")
                                        .clickable(onClick = onResetToCurrentMonth)
                                        .padding(top = 2.dp),
                                )
                            } else {
                                Text(
                                    text = "Rekap periode berjalan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }
                        }

                        IconButton(
                            onClick = { onShiftMonth(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("finance_next_month_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Bulan berikutnya",
                                tint = TextPrimary,
                            )
                        }
                    }
                }
            }
        }

        // Net Profit Hero Card
        item(key = "finance_profit_hero") {
            val isProfitable = summary.netProfit >= 0
            val heroBg = if (isProfitable) PrimarySoft else StatusDibatalkanBg.copy(alpha = 0.65f)
            val heroBorder = if (isProfitable) StatusSelesaiBorder else StatusDibatalkanBorder
            val heroAccent = if (isProfitable) PrimaryEmerald else StatusDibatalkanText

            KalaOrderCard(
                containerColor = heroBg,
                borderColor = heroBorder,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (isProfitable) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = heroAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.finance_net_profit),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                        )
                    }
                    SemanticPillBadge(
                        text = "Margin ${summary.profitMarginPercent}%",
                        backgroundColor = SurfaceWhite,
                        textColor = heroAccent,
                        borderColor = heroBorder,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = RupiahFormatter.formatSigned(summary.netProfit),
                    style = KalaOrderMonoStyles.MetricLarge.copy(fontSize = 26.sp, lineHeight = 32.sp),
                    color = heroAccent,
                    modifier = Modifier.testTag("finance_net_profit_value"),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${summary.periodLabel} • ${summary.totalTransactionCount} transaksi tercatat",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = heroBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.finance_cash_balance),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = "Modal + Laba Bersih",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Text(
                        text = RupiahFormatter.format(summary.netCashBalance),
                        style = KalaOrderMonoStyles.CodeLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                }
            }
        }

        // 2x2 Metric Grid (Order Income, Expense, Capital, Other Income)
        item(key = "finance_metric_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FinanceMetricCard(
                        label = stringResource(R.string.finance_order_income),
                        amount = RupiahFormatter.formatCompact(summary.totalOrderIncome),
                        subtext = "${summary.completedOrderCount} order selesai",
                        accentColor = PrimaryEmerald,
                        badgeBg = StatusSelesaiBg,
                        modifier = Modifier.weight(1f),
                    )
                    FinanceMetricCard(
                        label = stringResource(R.string.finance_total_expense),
                        amount = RupiahFormatter.formatCompact(summary.totalExpense),
                        subtext = "${summary.expenseCount} pengeluaran",
                        accentColor = StatusDibatalkanText,
                        badgeBg = StatusDibatalkanBg,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FinanceMetricCard(
                        label = stringResource(R.string.finance_capital),
                        amount = RupiahFormatter.formatCompact(summary.totalCapital),
                        subtext = "${summary.capitalCount} catatan modal",
                        accentColor = StatusDiprosesText,
                        badgeBg = StatusDiprosesBg,
                        modifier = Modifier.weight(1f),
                    )
                    FinanceMetricCard(
                        label = stringResource(R.string.finance_other_income),
                        amount = RupiahFormatter.formatCompact(summary.totalOtherIncome),
                        subtext = "${summary.otherIncomeCount} pemasukan lain",
                        accentColor = PrimaryEmerald,
                        badgeBg = PrimarySoft,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Recap & Export Quick Actions Card
        item(key = "finance_recap_actions") {
            KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rekap & Ekspor Laporan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Simpan arsip akhir bulan ke Excel (CSV) atau kirim ke WhatsApp",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    TextButton(
                        onClick = onOpenRecapPreview,
                        modifier = Modifier.testTag("finance_open_recap_preview"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "Pratinjau rekap",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pratinjau",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryEmerald,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KalaPrimaryButton(
                        text = "Ekspor CSV",
                        onClick = onExportCsvClick,
                        icon = Icons.Outlined.Download,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finance_export_csv_button"),
                    )
                    KalaSecondaryButton(
                        text = "Bagikan WA",
                        onClick = onShareWhatsAppClick,
                        icon = Icons.Outlined.Share,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finance_share_wa_button"),
                    )
                }
            }
        }

        // Cashflow Comparison Bar Chart
        item(key = "finance_cashflow_chart") {
            CashflowBarChartCard(
                points = summary.chartPoints,
                period = summary.period,
            )
        }

        // Expense Category Breakdown (shown when there are expenses in period)
        if (summary.expenseByCategory.isNotEmpty()) {
            item(key = "finance_expense_breakdown") {
                ExpenseCategoryBreakdownCard(categories = summary.expenseByCategory)
            }
        }

        // Unified Cashbook Ledger Header & Search
        item(key = "finance_ledger_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Buku Kas Gabungan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "${summary.ledgerItems.size} item",
                        style = KalaOrderMonoStyles.CodeSmall,
                        color = TextMuted,
                    )
                }
                KalaSearchField(
                    value = uiState.filters.financeSearch,
                    onValueChange = onSearchChange,
                    placeholder = stringResource(R.string.finance_search_placeholder),
                    modifier = Modifier.testTag("finance_search_field"),
                )
            }
        }

        if (summary.ledgerItems.isEmpty()) {
            item(key = "finance_ledger_empty") {
                KalaEmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = stringResource(R.string.empty_finance_title),
                    description = stringResource(R.string.empty_finance_desc),
                )
            }
        } else {
            items(
                items = summary.ledgerItems,
                key = { it.uniqueKey },
            ) { item ->
                when (item) {
                    is FinanceLedgerItem.OrderSaleItem -> OrderSaleLedgerRow(
                        order = item.order,
                        onClick = { onSelectOrder(item.order) },
                    )

                    is FinanceLedgerItem.ManualCashflowItem -> ManualCashflowLedgerRow(
                        entry = item.entry,
                        onEdit = { onEditCashflowClick(item.entry) },
                        onDelete = { onDeleteCashflowClick(item.entry) },
                    )
                }
            }
        }

        // Creator Attribution Footer & Subtle Donation Button
        item(key = "finance_creator_footer") {
            Surface(
                shape = KalaOrderShapes.Medium,
                color = SurfaceWhite,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = CreatorInfo.ATTRIBUTION_FOOTER,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onOpenDonationSheet,
                        shape = KalaOrderShapes.Pill,
                        color = StatusMenungguBg,
                        border = BorderStroke(1.dp, StatusMenungguBorder),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("finance_footer_donation_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Coffee,
                                contentDescription = stringResource(R.string.donation_chip_label),
                                tint = StatusMenungguText,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(R.string.donation_chip_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StatusMenungguText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceMetricCard(
    label: String,
    amount: String,
    subtext: String,
    accentColor: Color,
    badgeBg: Color,
    modifier: Modifier = Modifier,
) {
    KalaOrderCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = amount,
            style = KalaOrderMonoStyles.CodeLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = KalaOrderShapes.Small,
            color = badgeBg,
        ) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CashflowBarChartCard(
    points: List<DailyCashflowPoint>,
    period: FinancePeriod,
) {
    val maxVal = points.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    val subtitle = when (period) {
        FinancePeriod.TODAY -> "Perbandingan arus kas per sesi hari ini"
        FinancePeriod.LAST_7_DAYS -> "Arus kas 7 hari terakhir"
        FinancePeriod.MONTHLY -> "Ringkasan mingguan pada bulan terpilih"
        FinancePeriod.ALL_TIME -> "Tren 6 bulan terakhir"
    }

    KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Grafik Arus Kas",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChartLegendDot(color = PrimaryEmerald, label = "Masuk")
                ChartLegendDot(color = StatusDibatalkanText, label = "Keluar")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (points.isEmpty()) {
            Text(
                text = "Belum ada data grafik untuk periode ini.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp),
            ) {
                val width = size.width
                val height = size.height
                val gridLines = 3
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines.toFloat())
                    drawLine(
                        color = BorderSubtle,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i < gridLines) dashEffect else null,
                    )
                }

                val slotWidth = width / points.size.coerceAtLeast(1)
                val barWidth = (slotWidth * 0.24f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                val gap = 4.dp.toPx()
                val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                val maxBarHeight = height - 8.dp.toPx()

                points.forEachIndexed { index, point ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val incRatio = (point.income.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val expRatio = (point.expense.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

                    val incHeight = if (point.income > 0) (incRatio * maxBarHeight).coerceAtLeast(4.dp.toPx()) else 0f
                    val expHeight = if (point.expense > 0) (expRatio * maxBarHeight).coerceAtLeast(4.dp.toPx()) else 0f

                    if (incHeight > 0f) {
                        drawRoundRect(
                            color = PrimaryEmerald,
                            topLeft = Offset(centerX - barWidth - gap / 2f, height - incHeight),
                            size = Size(barWidth, incHeight),
                            cornerRadius = radius,
                        )
                    } else {
                        drawRoundRect(
                            color = FillSubtle,
                            topLeft = Offset(centerX - barWidth - gap / 2f, height - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = radius,
                        )
                    }

                    if (expHeight > 0f) {
                        drawRoundRect(
                            color = StatusDibatalkanText,
                            topLeft = Offset(centerX + gap / 2f, height - expHeight),
                            size = Size(barWidth, expHeight),
                            cornerRadius = radius,
                        )
                    } else {
                        drawRoundRect(
                            color = FillSubtle,
                            topLeft = Offset(centerX + gap / 2f, height - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = radius,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { point ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = point.label,
                            style = KalaOrderMonoStyles.CodeSmall,
                            color = TextMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun ExpenseCategoryBreakdownCard(
    categories: List<CategoryExpenseBreakdown>,
) {
    KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
        Text(
            text = "Rincian Pengeluaran Terbesar",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.take(5).forEach { item ->
                val pct = (item.ratio * 100f).roundToInt()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "$pct%",
                                style = KalaOrderMonoStyles.CodeSmall,
                                color = TextMuted,
                            )
                            Text(
                                text = RupiahFormatter.format(item.amount),
                                style = KalaOrderMonoStyles.CodeMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = StatusDibatalkanText,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(KalaOrderShapes.Pill)
                            .background(FillSubtle),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.ratio.coerceIn(0.04f, 1f))
                                .height(6.dp)
                                .clip(KalaOrderShapes.Pill)
                                .background(StatusDibatalkanText),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSaleLedgerRow(
    order: OrderRecord,
    onClick: () -> Unit,
) {
    KalaOrderCard(
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.testTag("finance_ledger_order_${order.orderCode}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = KalaOrderShapes.Medium,
                color = PrimarySoft,
                border = BorderStroke(1.dp, StatusSelesaiBorder),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = "Pemasukan Order",
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MonospaceCodeTag(code = order.orderCode, emphasized = true)
                    SemanticPillBadge(
                        text = "Auto Order",
                        backgroundColor = StatusSelesaiBg,
                        textColor = StatusSelesaiText,
                        borderColor = StatusSelesaiBorder,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${order.customerName} • ${order.totalItemCount} item (${order.channel.label})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Jam ${order.timestampLabel} • Ketuk untuk lihat struk",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                )
            }

            Text(
                text = RupiahFormatter.formatSigned(order.totalAmount),
                style = KalaOrderMonoStyles.CodeLarge.copy(fontWeight = FontWeight.Bold),
                color = PrimaryEmerald,
            )
        }
    }
}

@Composable
private fun ManualCashflowLedgerRow(
    entry: CashflowEntryRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val style = when (entry.type) {
        CashflowType.EXPENSE -> CashflowRowStyle(
            bg = StatusDibatalkanBg,
            fg = StatusDibatalkanText,
            border = StatusDibatalkanBorder,
            amountText = RupiahFormatter.formatSigned(-entry.amount),
        )
        CashflowType.CAPITAL -> CashflowRowStyle(
            bg = StatusDiprosesBg,
            fg = StatusDiprosesText,
            border = StatusDiprosesBorder,
            amountText = RupiahFormatter.format(entry.amount),
        )
        CashflowType.OTHER_INCOME -> CashflowRowStyle(
            bg = StatusSelesaiBg,
            fg = StatusSelesaiText,
            border = StatusSelesaiBorder,
            amountText = RupiahFormatter.formatSigned(entry.amount),
        )
    }

    KalaOrderCard(
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.testTag("finance_ledger_manual_${entry.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SemanticPillBadge(
                        text = entry.type.label,
                        backgroundColor = style.bg,
                        textColor = style.fg,
                        borderColor = style.border,
                    )
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.dateLabel,
                    style = KalaOrderMonoStyles.CodeSmall,
                    color = TextMuted,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = style.amountText,
                style = KalaOrderMonoStyles.CodeLarge.copy(fontWeight = FontWeight.Bold),
                color = style.fg,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KalaMicroActionButton(
                text = stringResource(R.string.action_sunting),
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_cashflow_${entry.id}"),
            )
            KalaMicroActionButton(
                text = stringResource(R.string.action_hapus),
                onClick = onDelete,
                destructive = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("delete_cashflow_${entry.id}"),
            )
        }
    }
}

private data class CashflowRowStyle(
    val bg: Color,
    val fg: Color,
    val border: Color,
    val amountText: String,
)
