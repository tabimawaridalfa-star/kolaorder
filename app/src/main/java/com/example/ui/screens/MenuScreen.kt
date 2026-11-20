package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MenuCategory
import com.example.data.MenuItemRecord
import com.example.data.RupiahFormatter
import com.example.ui.components.KalaEmptyState
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaMicroActionButton
import com.example.ui.components.KalaOrderCard
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSearchField
import com.example.ui.components.MonospaceCodeTag
import com.example.ui.components.SemanticPillBadge
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.StatusDibatalkanBg
import com.example.ui.theme.StatusDibatalkanBorder
import com.example.ui.theme.StatusDibatalkanText
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
import com.example.viewmodel.MenuFilter

@Composable
fun MenuScreen(
    uiState: KalaOrderUiState,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (MenuFilter) -> Unit,
    onAddMenuClick: () -> Unit,
    onEditMenuClick: (MenuItemRecord) -> Unit,
    onDeleteMenuClick: (MenuItemRecord) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 3 else 2
        val rows = uiState.filteredMenuItems.chunked(columns)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header + Add Menu CTA
            item(key = "menu_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Katalog Menu",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${uiState.menuItems.size} menu • ${uiState.lowStockCount} stok menipis",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    KalaPrimaryButton(
                        text = stringResource(R.string.action_tambah_menu),
                        onClick = onAddMenuClick,
                        icon = Icons.Rounded.Add,
                        modifier = Modifier.testTag("add_menu_button"),
                    )
                }
            }

            // Search bar
            item(key = "menu_search") {
                KalaSearchField(
                    value = uiState.filters.menuSearch,
                    onValueChange = onSearchChange,
                    placeholder = stringResource(R.string.search_menu_placeholder),
                    modifier = Modifier.testTag("menu_search_input"),
                )
            }

            // Category filter chips
            item(key = "menu_filters") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(MenuFilter.entries) { filter ->
                        val count = when (filter) {
                            MenuFilter.SEMUA -> uiState.menuItems.size
                            MenuFilter.MAKANAN -> uiState.menuItems.count { it.category == MenuCategory.MAKANAN }
                            MenuFilter.MINUMAN -> uiState.menuItems.count { it.category == MenuCategory.MINUMAN }
                            MenuFilter.CAMILAN -> uiState.menuItems.count { it.category == MenuCategory.CAMILAN }
                            MenuFilter.MENIPIS -> uiState.lowStockCount
                        }
                        KalaFilterChip(
                            label = filter.label,
                            selected = uiState.filters.menuFilter == filter,
                            count = count,
                            onClick = { onFilterSelect(filter) },
                            modifier = Modifier.testTag("menu_filter_${filter.name.lowercase()}"),
                        )
                    }
                }
            }

            // Empty state or 2-column grid (12dp gutter)
            if (uiState.filteredMenuItems.isEmpty()) {
                item(key = "menu_empty") {
                    KalaEmptyState(
                        icon = Icons.Outlined.RestaurantMenu,
                        title = stringResource(R.string.empty_menu_title),
                        description = stringResource(R.string.empty_menu_desc),
                    )
                }
            } else {
                items(
                    items = rows,
                    key = { row -> row.joinToString("_") { it.code } },
                ) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { item ->
                            MenuInventoryCard(
                                item = item,
                                onDecrement = { onAdjustStock(item.code, -1) },
                                onIncrement = { onAdjustStock(item.code, 1) },
                                onEdit = { onEditMenuClick(item) },
                                onDelete = { onDeleteMenuClick(item) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuInventoryCard(
    item: MenuItemRecord,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (badgeLabel, badgeBg, badgeFg, badgeBorder) = when {
        item.isOutOfStock -> Quad("Habis", StatusDibatalkanBg, StatusDibatalkanText, StatusDibatalkanBorder)
        item.isLowStock -> Quad("Menipis", StatusMenungguBg, StatusMenungguText, StatusMenungguBorder)
        else -> Quad("Tersedia", StatusSelesaiBg, StatusSelesaiText, StatusSelesaiBorder)
    }

    KalaOrderCard(
        modifier = modifier.testTag("menu_card_${item.code}"),
        contentPadding = PaddingValues(12.dp),
    ) {
        // Top row: Inventory code tag (A1) + Stock status pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonospaceCodeTag(code = item.code, emphasized = !item.isOutOfStock)
            SemanticPillBadge(
                text = badgeLabel,
                backgroundColor = badgeBg,
                textColor = badgeFg,
                borderColor = badgeBorder,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Menu name & category
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.category.label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Monospace tabular Rupiah price
        Text(
            text = RupiahFormatter.format(item.price),
            style = KalaOrderMonoStyles.CodeLarge,
            color = PrimaryEmerald,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick stock adjustment stepper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FillSubtle, KalaOrderShapes.Medium)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StockStepButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Kurangi stok ${item.name}",
                enabled = item.stock > 0,
                onClick = onDecrement,
                modifier = Modifier.testTag("stock_minus_${item.code}"),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Stok",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = TextMuted,
                )
                Text(
                    text = item.stock.toString(),
                    style = KalaOrderMonoStyles.CodeMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (item.isOutOfStock) StatusDibatalkanText else TextPrimary,
                )
            }
            StockStepButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Tambah stok ${item.name}",
                enabled = true,
                onClick = onIncrement,
                modifier = Modifier.testTag("stock_plus_${item.code}"),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        // Micro actions: Sunting & Hapus
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KalaMicroActionButton(
                text = stringResource(R.string.action_sunting),
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_menu_${item.code}"),
            )
            KalaMicroActionButton(
                text = stringResource(R.string.action_hapus),
                onClick = onDelete,
                destructive = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("delete_menu_${item.code}"),
            )
        }
    }
}

@Composable
private fun StockStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(KalaOrderShapes.Small)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = KalaOrderShapes.Small,
            color = SurfaceWhite,
            border = BorderStroke(1.dp, if (enabled) BorderStrong else BorderSubtle),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) TextPrimary else TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
