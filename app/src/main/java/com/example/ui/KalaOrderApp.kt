package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bot.NotificationAccessHelper
import com.example.data.MenuItemRecord
import com.example.data.OrderChannel
import com.example.data.OrderRecord
import com.example.ui.screens.BotSettingsScreen
import com.example.ui.screens.DeleteMenuConfirmDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MenuEditorBottomSheet
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.NewOrderBottomSheet
import com.example.ui.screens.OrderDetailBottomSheet
import com.example.ui.screens.QueueScreen
import com.example.ui.theme.BgCanvas
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimarySoft
import com.example.ui.theme.ShadowEmerald
import com.example.ui.theme.ShadowNeutral
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.AppTab
import com.example.viewmodel.KalaOrderViewModel
import kotlinx.coroutines.launch

@Composable
fun KalaOrderApp(
    viewModel: KalaOrderViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableStateOf(AppTab.ANTREAN) }
    var selectedOrderForDetail by remember { mutableStateOf<OrderRecord?>(null) }
    var newOrderSheetChannel by remember { mutableStateOf<OrderChannel?>(null) }
    var showMenuEditor by remember { mutableStateOf(false) }
    var editingMenuItem by remember { mutableStateOf<MenuItemRecord?>(null) }
    var deletingMenuItem by remember { mutableStateOf<MenuItemRecord?>(null) }

    // Runtime permission launcher for Android 13+ POST_NOTIFICATIONS
    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshSystemReadiness()
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Izin notifikasi aktif • Order baru akan berbunyi & bergetar"
                else "Izin notifikasi ditolak • Aktifkan dari Info Aplikasi agar siaga 24 jam tampil"
            )
        }
    }

    // Automatically refresh Notification Access, POST_NOTIFICATIONS, Battery Exemption & FGS status on resume
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSystemReadiness()
    }

    // Back gesture from secondary tabs returns gracefully to Antrean home tab
    BackHandler(enabled = activeTab != AppTab.ANTREAN) {
        activeTab = AppTab.ANTREAN
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val openNotificationAccessSettings = remember(context) {
        { NotificationAccessHelper.openNotificationAccessSettings(context) }
    }

    val requestPostNotifications = remember(context, postNotificationLauncher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (NotificationAccessHelper.isPostNotificationsGranted(context)) {
                    NotificationAccessHelper.openAppDetailsSettings(context)
                } else {
                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                NotificationAccessHelper.openAppDetailsSettings(context)
            }
        }
    }

    val requestBatteryExemption = remember(context) {
        { NotificationAccessHelper.requestIgnoreBatteryOptimizations(context) }
    }

    val openAppSettings = remember(context) {
        { NotificationAccessHelper.openAppDetailsSettings(context) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas),
    ) {
        val isExpanded = maxWidth >= 840.dp

        val floatingSimulatorPill: @Composable () -> Unit = {
            if (activeTab == AppTab.ANTREAN) {
                Surface(
                    onClick = { newOrderSheetChannel = OrderChannel.WHATSAPP },
                    shape = KalaOrderShapes.Pill,
                    color = PrimaryEmerald,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("floating_wa_simulator_pill")
                        .shadow(
                            elevation = 8.dp,
                            shape = KalaOrderShapes.Pill,
                            ambientColor = ShadowEmerald,
                            spotColor = ShadowEmerald,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Simulasi Pesanan WhatsApp",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Simulasi WA",
                            style = MaterialTheme.typography.labelLarge,
                            color = SurfaceWhite,
                        )
                    }
                }
            }
        }

        val screenContent: @Composable (Modifier) -> Unit = { contentModifier ->
            Box(
                modifier = contentModifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 880.dp),
                ) {
                    Crossfade(
                        targetState = activeTab,
                        animationSpec = tween(durationMillis = 180),
                        label = "tab_crossfade",
                    ) { tab ->
                        when (tab) {
                            AppTab.ANTREAN -> QueueScreen(
                                uiState = uiState,
                                onToggleStoreOpen = viewModel::toggleStoreOpen,
                                onToggleBotActive = viewModel::toggleBotActive,
                                onOpenNotificationSettings = openNotificationAccessSettings,
                                onOpenBotTab = { activeTab = AppTab.BOT },
                                onSearchChange = viewModel::onQueueSearchChange,
                                onFilterSelect = viewModel::onQueueFilterSelect,
                                onUpdateStatus = viewModel::updateOrderStatus,
                                onSelectOrder = { selectedOrderForDetail = it },
                                onOpenManualOrder = { newOrderSheetChannel = OrderChannel.KASIR },
                            )

                            AppTab.MENU -> MenuScreen(
                                uiState = uiState,
                                onSearchChange = viewModel::onMenuSearchChange,
                                onFilterSelect = viewModel::onMenuFilterSelect,
                                onAddMenuClick = {
                                    editingMenuItem = null
                                    showMenuEditor = true
                                },
                                onEditMenuClick = { item ->
                                    editingMenuItem = item
                                    showMenuEditor = true
                                },
                                onDeleteMenuClick = { item ->
                                    deletingMenuItem = item
                                },
                                onAdjustStock = viewModel::adjustMenuStock,
                            )

                            AppTab.RIWAYAT -> HistoryScreen(
                                uiState = uiState,
                                onSearchChange = viewModel::onHistorySearchChange,
                                onFilterSelect = viewModel::onHistoryFilterSelect,
                                onSelectOrder = { selectedOrderForDetail = it },
                            )

                            AppTab.BOT -> BotSettingsScreen(
                                uiState = uiState,
                                onToggleStoreOpen = viewModel::toggleStoreOpen,
                                onOpenNotificationSettings = openNotificationAccessSettings,
                                onRequestPostNotifications = requestPostNotifications,
                                onRequestBatteryExemption = requestBatteryExemption,
                                onRefreshKeepAliveService = viewModel::startOrRefreshKeepAliveService,
                                onOpenAppSettings = openAppSettings,
                                onUpdateSettings = viewModel::updateBotSettings,
                                onClearBotLogs = viewModel::clearBotLogs,
                                onResetCooldowns = viewModel::resetContactCooldowns,
                                onOpenWhatsAppSimulator = { newOrderSheetChannel = OrderChannel.WHATSAPP },
                                onOpenManualOrder = { newOrderSheetChannel = OrderChannel.KASIR },
                            )
                        }
                    }
                }
            }
        }

        if (isExpanded) {
            Scaffold(
                containerColor = BgCanvas,
                contentWindowInsets = WindowInsets.safeDrawing,
                floatingActionButton = floatingSimulatorPill,
                snackbarHost = { KalaSnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavigationRail(
                        containerColor = SurfaceWhite,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PrimaryEmerald,
                                    selectedTextColor = PrimaryEmerald,
                                    indicatorColor = PrimarySoft,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                            )
                        }
                    }
                    VerticalDivider(color = BorderSubtle, thickness = 1.dp)
                    screenContent(Modifier.weight(1f))
                }
            }
        } else {
            Scaffold(
                containerColor = BgCanvas,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = floatingSimulatorPill,
                snackbarHost = { KalaSnackbarHost(snackbarHostState) },
                bottomBar = {
                    KalaOrderBottomBar(
                        activeTab = activeTab,
                        activeQueueCount = uiState.activeQueueCount,
                        onSelectTab = { activeTab = it },
                    )
                },
            ) { innerPadding ->
                screenContent(
                    Modifier
                        .statusBarsPadding()
                        .padding(innerPadding),
                )
            }
        }
    }

    // Modal Bottom Sheets & Confirmation Dialogs
    selectedOrderForDetail?.let { order ->
        val latest = uiState.orders.firstOrNull { it.id == order.id } ?: order
        OrderDetailBottomSheet(
            order = latest,
            botSettings = uiState.botSettings,
            onDismiss = { selectedOrderForDetail = null },
            onUpdateStatus = { status -> viewModel.updateOrderStatus(latest, status) },
            onSendWhatsAppStatus = { targetOrder, text ->
                val opened = NotificationAccessHelper.openWhatsAppChat(
                    context = context,
                    rawPhone = targetOrder.customerPhone,
                    message = text,
                )
                if (!opened) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Aplikasi WhatsApp tidak ditemukan di perangkat ini")
                    }
                }
            },
        )
    }

    newOrderSheetChannel?.let { channel ->
        NewOrderBottomSheet(
            initialChannel = channel,
            menuItems = uiState.menuItems,
            onDismiss = { newOrderSheetChannel = null },
            onSimulateNotification = viewModel::simulateWhatsAppNotification,
            onSubmitManualOrder = { name, phone, selectedChannel, items, note ->
                viewModel.submitNewOrder(name, phone, selectedChannel, items, note)
            },
        )
    }

    if (showMenuEditor) {
        MenuEditorBottomSheet(
            editingItem = editingMenuItem,
            onDismiss = {
                showMenuEditor = false
                editingMenuItem = null
            },
            onSave = { record, isEdit ->
                viewModel.saveMenuItem(record, isEdit)
            },
        )
    }

    deletingMenuItem?.let { item ->
        DeleteMenuConfirmDialog(
            item = item,
            onDismiss = { deletingMenuItem = null },
            onConfirm = { viewModel.deleteMenuItem(item) },
        )
    }
}

/**
 * Elevated Level 2 Bottom Navigation Bar: #FFFFFF, top 1px #E2E8F0 hairline,
 * ambient shadow, and explicit navigationBarsPadding() so system gesture pills never overlap.
 */
@Composable
private fun KalaOrderBottomBar(
    activeTab: AppTab,
    activeQueueCount: Int,
    onSelectTab: (AppTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                ambientColor = ShadowNeutral,
                spotColor = ShadowNeutral,
            ),
        color = SurfaceWhite,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    val contentColor = if (selected) PrimaryEmerald else TextMuted

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(KalaOrderShapes.Medium)
                            .testTag("nav_tab_${tab.name.lowercase()}")
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (selected) PrimarySoft else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = KalaOrderShapes.Pill,
                                    )
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = tabIcon(tab, selected),
                                    contentDescription = tab.label,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            if (tab == AppTab.ANTREAN && activeQueueCount > 0) {
                                Surface(
                                    shape = KalaOrderShapes.Pill,
                                    color = PrimaryEmerald,
                                    border = BorderStroke(1.5.dp, SurfaceWhite),
                                ) {
                                    Text(
                                        text = activeQueueCount.toString(),
                                        style = KalaOrderMonoStyles.CodeSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SurfaceWhite,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KalaSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = TextPrimary,
            contentColor = SurfaceWhite,
            shape = KalaOrderShapes.Medium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean): ImageVector = when (tab) {
    AppTab.ANTREAN -> if (selected) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
    AppTab.MENU -> if (selected) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu
    AppTab.RIWAYAT -> if (selected) Icons.Filled.History else Icons.Outlined.History
    AppTab.BOT -> if (selected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
}
