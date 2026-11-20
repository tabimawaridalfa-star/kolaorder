package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OrderChannel
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.RupiahFormatter
import com.example.ui.components.BotStatusBannerCard
import com.example.ui.components.KalaDestructiveOutlineButton
import com.example.ui.components.KalaEmptyState
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaOrderCard
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSearchField
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.components.MonospaceCodeTag
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.StoreStatusPill
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimarySoft
import com.example.ui.theme.StatusDiprosesText
import com.example.ui.theme.StatusMenungguText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.KalaOrderUiState
import com.example.viewmodel.QueueFilter

@Composable
fun QueueScreen(
    uiState: KalaOrderUiState,
    onToggleStoreOpen: () -> Unit,
    onToggleBotActive: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBotTab: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (QueueFilter) -> Unit,
    onUpdateStatus: (OrderRecord, OrderStatus) -> Unit,
    onSelectOrder: (OrderRecord) -> Unit,
    onOpenManualOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: Store identity + TOKO BUKA toggle pill
        item(key = "queue_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = KalaOrderShapes.Medium,
                        color = PrimaryEmerald,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = "Ikon KalaOrder",
                                tint = SurfaceWhite,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                        Text(
                            text = "${uiState.botSettings.storeName} • Kasir",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                StoreStatusPill(
                    isOpen = uiState.botSettings.storeOpen,
                    onToggle = onToggleStoreOpen,
                )
            }
        }

        // Bot WhatsApp live operational banner card
        item(key = "bot_banner") {
            BotStatusBannerCard(
                botActive = uiState.botSettings.botActive,
                storeOpen = uiState.botSettings.storeOpen,
                isNotificationAccessGranted = uiState.readiness.notificationAccessGranted,
                missingBackgroundPermissions = uiState.readiness.missingSetupCount,
                waitingCount = uiState.waitingCount,
                onToggleBot = onToggleBotActive,
                onOpenPermissionSettings = onOpenNotificationSettings,
                onBannerClick = onOpenBotTab,
            )
        }

        // 2-column dashboard metric cards (12dp gutter)
        item(key = "queue_metrics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricSummaryCard(
                    label = stringResource(R.string.metric_active_queue),
                    value = uiState.activeQueueCount.toString(),
                    caption = "${uiState.waitingCount} menunggu • ${uiState.processingCount} proses",
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = StatusMenungguText,
                    modifier = Modifier.weight(1f),
                )
                MetricSummaryCard(
                    label = stringResource(R.string.metric_today_revenue),
                    value = RupiahFormatter.format(uiState.todayRevenue),
                    caption = "${uiState.completedCount} pesanan selesai",
                    icon = Icons.Outlined.Payments,
                    iconTint = PrimaryEmerald,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Section title + manual POS shortcut
        item(key = "queue_controls_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Antrean Dapur",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Surface(
                    onClick = onOpenManualOrder,
                    shape = KalaOrderShapes.Medium,
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.testTag("manual_pos_order_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Order Kasir",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryEmerald,
                        )
                    }
                }
            }
        }

        // Search input
        item(key = "queue_search") {
            KalaSearchField(
                value = uiState.filters.queueSearch,
                onValueChange = onSearchChange,
                placeholder = stringResource(R.string.search_order_placeholder),
                modifier = Modifier.testTag("queue_search_input"),
            )
        }

        // Filter chips row
        item(key = "queue_filters") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(QueueFilter.entries) { filter ->
                    val count = when (filter) {
                        QueueFilter.SEMUA -> uiState.orders.size
                        QueueFilter.MENUNGGU -> uiState.waitingCount
                        QueueFilter.DIPROSES -> uiState.processingCount
                        QueueFilter.SELESAI -> uiState.completedCount
                        QueueFilter.DIBATALKAN -> uiState.cancelledCount
                    }
                    KalaFilterChip(
                        label = filter.label,
                        selected = uiState.filters.queueFilter == filter,
                        count = count,
                        onClick = { onFilterSelect(filter) },
                        modifier = Modifier.testTag("queue_filter_${filter.name.lowercase()}"),
                    )
                }
            }
        }

        // Order Queue Cards or Empty State
        if (uiState.filteredQueueOrders.isEmpty()) {
            item(key = "queue_empty") {
                KalaEmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(R.string.empty_queue_title),
                    description = stringResource(R.string.empty_queue_desc),
                )
            }
        } else {
            items(
                items = uiState.filteredQueueOrders,
                key = { it.id },
            ) { order ->
                OrderQueueCard(
                    order = order,
                    onClick = { onSelectOrder(order) },
                    onProcessOrder = { onUpdateStatus(order, OrderStatus.DIPROSES) },
                    onCompleteOrder = { onUpdateStatus(order, OrderStatus.SELESAI) },
                    onCancelOrder = { onUpdateStatus(order, OrderStatus.DIBATALKAN) },
                )
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    label: String,
    value: String,
    caption: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    KalaOrderCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = KalaOrderMonoStyles.MetricLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OrderQueueCard(
    order: OrderRecord,
    onClick: () -> Unit,
    onProcessOrder: () -> Unit,
    onCompleteOrder: () -> Unit,
    onCancelOrder: () -> Unit,
) {
    KalaOrderCard(
        modifier = Modifier.testTag("order_card_${order.orderCode}"),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        // Top row: Order code (JetBrains Mono) + Channel + Timestamp + Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MonospaceCodeTag(
                    code = order.orderCode,
                    emphasized = order.status == OrderStatus.MENUNGGU || order.status == OrderStatus.DIPROSES,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = order.timestampLabel,
                        style = KalaOrderMonoStyles.CodeSmall,
                        color = TextSecondary,
                    )
                }
            }
            OrderStatusBadge(status = order.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Customer details + Channel badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = order.customerPhone,
                    style = KalaOrderMonoStyles.CodeSmall,
                    color = TextMuted,
                    maxLines = 1,
                )
            }
            Surface(
                shape = KalaOrderShapes.Small,
                color = if (order.channel == OrderChannel.WHATSAPP) PrimarySoft else FillSubtle,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (order.channel == OrderChannel.WHATSAPP) {
                            Icons.Outlined.ChatBubbleOutline
                        } else {
                            Icons.Outlined.PointOfSale
                        },
                        contentDescription = null,
                        tint = if (order.channel == OrderChannel.WHATSAPP) PrimaryEmerald else StatusDiprosesText,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = order.channel.label,
                        style = KalaOrderMonoStyles.BadgeLabel,
                        color = if (order.channel == OrderChannel.WHATSAPP) PrimaryEmerald else StatusDiprosesText,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Bold item counts & menu names
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "${item.quantity}x",
                            style = KalaOrderMonoStyles.CodeMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryEmerald,
                        )
                        Text(
                            text = item.menuName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = RupiahFormatter.format(item.subtotal),
                        style = KalaOrderMonoStyles.CodeSmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        if (order.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FillSubtle, KalaOrderShapes.Small)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Catatan: ${order.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom row: Total amount left, action buttons flush right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "${order.totalItemCount} item",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                Text(
                    text = RupiahFormatter.format(order.totalAmount),
                    style = KalaOrderMonoStyles.CodeLarge,
                    color = TextPrimary,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (order.status) {
                    OrderStatus.MENUNGGU -> {
                        KalaDestructiveOutlineButton(
                            text = stringResource(R.string.action_tolak),
                            onClick = onCancelOrder,
                            modifier = Modifier.testTag("cancel_order_${order.orderCode}"),
                        )
                        KalaPrimaryButton(
                            text = stringResource(R.string.action_proses_pesanan),
                            onClick = onProcessOrder,
                            modifier = Modifier.testTag("process_order_${order.orderCode}"),
                        )
                    }
                    OrderStatus.DIPROSES -> {
                        KalaSecondaryButton(
                            text = stringResource(R.string.action_detail),
                            onClick = onClick,
                        )
                        KalaPrimaryButton(
                            text = stringResource(R.string.action_tandai_selesai),
                            onClick = onCompleteOrder,
                            icon = Icons.Rounded.Check,
                            modifier = Modifier.testTag("complete_order_${order.orderCode}"),
                        )
                    }
                    OrderStatus.SELESAI, OrderStatus.DIBATALKAN -> {
                        KalaSecondaryButton(
                            text = stringResource(R.string.action_lihat_struk),
                            onClick = onClick,
                        )
                    }
                }
            }
        }
    }
}
