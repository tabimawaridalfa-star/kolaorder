package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OrderRecord
import com.example.data.RupiahFormatter
import com.example.ui.components.KalaEmptyState
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaOrderCard
import com.example.ui.components.KalaSearchField
import com.example.ui.components.OrderStatusBadge
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.StatusDibatalkanText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.HistoryFilter
import com.example.viewmodel.KalaOrderUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    uiState: KalaOrderUiState,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (HistoryFilter) -> Unit,
    onSelectOrder: (OrderRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "history_header") {
            Column {
                Text(
                    text = "Riwayat Transaksi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "Rekap pesanan hari ini • Tap baris untuk melihat struk",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        // 2-column summary metrics (12dp gutter)
        item(key = "history_kpis") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HistoryStatTile(
                    label = stringResource(R.string.metric_today_revenue),
                    value = RupiahFormatter.format(uiState.todayRevenue),
                    accentColor = PrimaryEmerald,
                    sublabel = "${uiState.completedCount} transaksi selesai",
                    modifier = Modifier.weight(1f),
                )
                HistoryStatTile(
                    label = stringResource(R.string.metric_total_orders),
                    value = uiState.orders.size.toString(),
                    accentColor = TextPrimary,
                    sublabel = "${uiState.cancelledCount} dibatalkan",
                    sublabelColor = if (uiState.cancelledCount > 0) StatusDibatalkanText else TextMuted,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Search
        item(key = "history_search") {
            KalaSearchField(
                value = uiState.filters.historySearch,
                onValueChange = onSearchChange,
                placeholder = stringResource(R.string.search_history_placeholder),
                modifier = Modifier.testTag("history_search_input"),
            )
        }

        // Filter chips
        item(key = "history_filters") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(HistoryFilter.entries) { filter ->
                    KalaFilterChip(
                        label = filter.label,
                        selected = uiState.filters.historyFilter == filter,
                        onClick = { onFilterSelect(filter) },
                        modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}"),
                    )
                }
            }
        }

        // Structured Data Table with sticky header & zebra striping
        if (uiState.filteredHistoryOrders.isEmpty()) {
            item(key = "history_empty") {
                KalaEmptyState(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.empty_history_title),
                    description = stringResource(R.string.empty_history_desc),
                )
            }
        } else {
            stickyHeader(key = "history_table_header") {
                Surface(
                    shape = KalaOrderShapes.Small,
                    color = FillSubtle,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.table_col_kode),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1.05f),
                        )
                        Text(
                            text = stringResource(R.string.table_col_pelanggan),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1.35f),
                        )
                        Text(
                            text = stringResource(R.string.table_col_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.0f),
                        )
                        Text(
                            text = stringResource(R.string.table_col_status),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.05f),
                        )
                    }
                }
            }

            itemsIndexed(
                items = uiState.filteredHistoryOrders,
                key = { _, order -> order.id },
            ) { index, order ->
                val rowBg = if (index % 2 == 0) SurfaceWhite else BgCanvas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg, KalaOrderShapes.Small)
                        .testTag("history_row_${order.orderCode}")
                        .clickable { onSelectOrder(order) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1.05f)) {
                            Text(
                                text = order.orderCode,
                                style = KalaOrderMonoStyles.CodeMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                maxLines = 1,
                            )
                            Text(
                                text = order.timestampLabel,
                                style = KalaOrderMonoStyles.CodeSmall,
                                color = TextMuted,
                                maxLines = 1,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1.35f)
                                .padding(end = 6.dp),
                        ) {
                            Text(
                                text = order.customerName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${order.totalItemCount} item • ${order.channel.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = RupiahFormatter.format(order.totalAmount),
                            style = KalaOrderMonoStyles.CodeMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1.0f)
                                .padding(end = 6.dp),
                        )
                        Row(
                            modifier = Modifier.weight(1.05f),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            OrderStatusBadge(status = order.status)
                        }
                    }
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun HistoryStatTile(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    sublabel: String,
    modifier: Modifier = Modifier,
    sublabelColor: androidx.compose.ui.graphics.Color = TextMuted,
) {
    KalaOrderCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = KalaOrderMonoStyles.MetricLarge,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = sublabelColor,
            maxLines = 1,
        )
    }
}
