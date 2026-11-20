package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bot.BotNotificationChannels
import com.example.bot.BotReplyDecision
import com.example.bot.KalaOrderBotKeepAliveService
import com.example.bot.NotificationAccessHelper
import com.example.bot.WhatsAppAutoReplyEngine
import com.example.data.BotActivityLogRecord
import com.example.data.BotSettings
import com.example.data.CashflowEntryRecord
import com.example.data.DataResetSelection
import com.example.data.FinancePeriod
import com.example.data.FinanceRecapEngine
import com.example.data.FinanceSummaryState
import com.example.data.FinanceYearMonth
import com.example.data.KalaOrderRepository
import com.example.data.MenuCategory
import com.example.data.MenuItemRecord
import com.example.data.OrderChannel
import com.example.data.OrderLineItem
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    ANTREAN("Antrean"),
    MENU("Menu"),
    KEUANGAN("Keuangan"),
    RIWAYAT("Riwayat"),
    BOT("Bot WA"),
}

enum class QueueFilter(val label: String) {
    SEMUA("Semua"),
    MENUNGGU("Menunggu"),
    DIPROSES("Diproses"),
    SELESAI("Selesai"),
    DIBATALKAN("Dibatalkan"),
}

enum class MenuFilter(val label: String) {
    SEMUA("Semua"),
    MAKANAN("Makanan"),
    MINUMAN("Minuman"),
    CAMILAN("Camilan"),
    MENIPIS("Stok Menipis"),
}

enum class HistoryFilter(val label: String) {
    SEMUA("Semua"),
    SELESAI("Selesai"),
    DIBATALKAN("Dibatalkan"),
    WHATSAPP("WhatsApp"),
    KASIR("Kasir"),
}

data class FilterState(
    val queueSearch: String = "",
    val queueFilter: QueueFilter = QueueFilter.SEMUA,
    val menuSearch: String = "",
    val menuFilter: MenuFilter = MenuFilter.SEMUA,
    val historySearch: String = "",
    val historyFilter: HistoryFilter = HistoryFilter.SEMUA,
    val financePeriod: FinancePeriod = FinancePeriod.MONTHLY,
    val selectedFinanceMonth: FinanceYearMonth = FinanceYearMonth.current(),
    val financeSearch: String = "",
)

data class BackgroundBotReadiness(
    val notificationAccessGranted: Boolean = false,
    val postNotificationGranted: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false,
    val keepAliveServiceRunning: Boolean = false,
) {
    val missingSetupCount: Int
        get() = listOf(
            notificationAccessGranted,
            postNotificationGranted,
            batteryOptimizationIgnored,
        ).count { !it }

    val isFullyReady24h: Boolean
        get() = notificationAccessGranted &&
            postNotificationGranted &&
            batteryOptimizationIgnored &&
            keepAliveServiceRunning
}

data class KalaOrderUiState(
    val isInitialized: Boolean = false,
    val orders: List<OrderRecord> = emptyList(),
    val filteredQueueOrders: List<OrderRecord> = emptyList(),
    val menuItems: List<MenuItemRecord> = emptyList(),
    val filteredMenuItems: List<MenuItemRecord> = emptyList(),
    val filteredHistoryOrders: List<OrderRecord> = emptyList(),
    val botSettings: BotSettings = BotSettings(),
    val botActivityLogs: List<BotActivityLogRecord> = emptyList(),
    val cashflowEntries: List<CashflowEntryRecord> = emptyList(),
    val financeSummary: FinanceSummaryState = FinanceSummaryState(),
    val readiness: BackgroundBotReadiness = BackgroundBotReadiness(),
    val filters: FilterState = FilterState(),
    val activeQueueCount: Int = 0,
    val waitingCount: Int = 0,
    val processingCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0,
    val todayRevenue: Int = 0,
    val lowStockCount: Int = 0,
) {
    val isNotificationAccessGranted: Boolean
        get() = readiness.notificationAccessGranted
}

class KalaOrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KalaOrderRepository = KalaOrderRepository.fromContext(application)
    private val autoReplyEngine = WhatsAppAutoReplyEngine(repository)

    private val filterState = MutableStateFlow(FilterState())
    private val permissionSnapshot = MutableStateFlow(readSystemPermissions())

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val uiState: StateFlow<KalaOrderUiState>

    init {
        val coreDataFlow = combine(
            repository.orders,
            repository.menuItems,
            repository.botSettings,
            repository.botActivityLogs,
            repository.cashflowEntries,
        ) { orders, menuItems, settings, logs, cashflows ->
            CoreDataSnapshot(orders, menuItems, settings, logs, cashflows)
        }

        val readinessFlow = combine(
            permissionSnapshot,
            KalaOrderBotKeepAliveService.isRunning,
        ) { perms, serviceRunning ->
            BackgroundBotReadiness(
                notificationAccessGranted = perms.notificationAccessGranted,
                postNotificationGranted = perms.postNotificationGranted,
                batteryOptimizationIgnored = perms.batteryOptimizationIgnored,
                keepAliveServiceRunning = serviceRunning,
            )
        }

        uiState = combine(
            coreDataFlow,
            filterState,
            readinessFlow,
        ) { data, filters, readiness ->
            val orders = data.orders
            val menuItems = data.menuItems
            val settings = data.settings

            val waiting = orders.count { it.status == OrderStatus.MENUNGGU }
            val processing = orders.count { it.status == OrderStatus.DIPROSES }
            val completed = orders.count { it.status == OrderStatus.SELESAI }
            val cancelled = orders.count { it.status == OrderStatus.DIBATALKAN }
            val revenue = orders
                .filter { it.status == OrderStatus.SELESAI }
                .sumOf { it.totalAmount }

            val filteredQueue = orders.filter { order ->
                val matchesStatus = when (filters.queueFilter) {
                    QueueFilter.SEMUA -> true
                    QueueFilter.MENUNGGU -> order.status == OrderStatus.MENUNGGU
                    QueueFilter.DIPROSES -> order.status == OrderStatus.DIPROSES
                    QueueFilter.SELESAI -> order.status == OrderStatus.SELESAI
                    QueueFilter.DIBATALKAN -> order.status == OrderStatus.DIBATALKAN
                }
                val q = filters.queueSearch.trim()
                val matchesQuery = q.isEmpty() ||
                    order.orderCode.contains(q, ignoreCase = true) ||
                    order.customerName.contains(q, ignoreCase = true) ||
                    order.items.any {
                        it.menuName.contains(q, ignoreCase = true) ||
                            it.menuCode.contains(q, ignoreCase = true)
                    }
                matchesStatus && matchesQuery
            }

            val filteredMenu = menuItems.filter { item ->
                val matchesCat = when (filters.menuFilter) {
                    MenuFilter.SEMUA -> true
                    MenuFilter.MAKANAN -> item.category == MenuCategory.MAKANAN
                    MenuFilter.MINUMAN -> item.category == MenuCategory.MINUMAN
                    MenuFilter.CAMILAN -> item.category == MenuCategory.CAMILAN
                    MenuFilter.MENIPIS -> item.stock <= 5
                }
                val q = filters.menuSearch.trim()
                val matchesQuery = q.isEmpty() ||
                    item.code.contains(q, ignoreCase = true) ||
                    item.name.contains(q, ignoreCase = true)
                matchesCat && matchesQuery
            }

            val filteredHistory = orders.filter { order ->
                val matchesFilter = when (filters.historyFilter) {
                    HistoryFilter.SEMUA -> true
                    HistoryFilter.SELESAI -> order.status == OrderStatus.SELESAI
                    HistoryFilter.DIBATALKAN -> order.status == OrderStatus.DIBATALKAN
                    HistoryFilter.WHATSAPP -> order.channel == OrderChannel.WHATSAPP
                    HistoryFilter.KASIR -> order.channel == OrderChannel.KASIR
                }
                val q = filters.historySearch.trim()
                val matchesQuery = q.isEmpty() ||
                    order.orderCode.contains(q, ignoreCase = true) ||
                    order.customerName.contains(q, ignoreCase = true)
                matchesFilter && matchesQuery
            }

            val financeSummary = FinanceRecapEngine.computeSummary(
                orders = orders,
                cashflowEntries = data.cashflowEntries,
                period = filters.financePeriod,
                selectedMonth = filters.selectedFinanceMonth,
                searchQuery = filters.financeSearch,
            )

            KalaOrderUiState(
                isInitialized = true,
                orders = orders,
                filteredQueueOrders = filteredQueue,
                menuItems = menuItems,
                filteredMenuItems = filteredMenu,
                filteredHistoryOrders = filteredHistory,
                botSettings = settings,
                botActivityLogs = data.logs,
                cashflowEntries = data.cashflowEntries,
                financeSummary = financeSummary,
                readiness = readiness,
                filters = filters,
                activeQueueCount = waiting + processing,
                waitingCount = waiting,
                processingCount = processing,
                completedCount = completed,
                cancelledCount = cancelled,
                todayRevenue = revenue,
                lowStockCount = menuItems.count { it.stock <= 5 },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KalaOrderUiState(),
        )

        viewModelScope.launch {
            repository.ensureSeedData()
            BotNotificationChannels.ensureChannels(application)
            syncKeepAliveService()
        }
    }

    private fun readSystemPermissions(): PermissionSnapshot {
        val app = getApplication<Application>()
        return PermissionSnapshot(
            notificationAccessGranted = NotificationAccessHelper.isNotificationAccessEnabled(app),
            postNotificationGranted = NotificationAccessHelper.isPostNotificationsGranted(app),
            batteryOptimizationIgnored = NotificationAccessHelper.isIgnoringBatteryOptimizations(app),
        )
    }

    fun refreshSystemReadiness() {
        permissionSnapshot.value = readSystemPermissions()
        viewModelScope.launch {
            syncKeepAliveService()
        }
    }

    fun startOrRefreshKeepAliveService() {
        val app = getApplication<Application>()
        if (NotificationAccessHelper.isNotificationAccessEnabled(app)) {
            NotificationAccessHelper.requestListenerRebind(app)
        }
        val started = KalaOrderBotKeepAliveService.startOrUpdate(app)
        _snackbarMessages.tryEmit(
            if (started) "Layanan Siaga 24 Jam aktif & tersambung ke notifikasi"
            else "Tidak dapat menyalakan layanan siaga — pastikan izin notifikasi aktif"
        )
    }

    private suspend fun syncKeepAliveService() {
        val app = getApplication<Application>()
        val settings = repository.getBotSettingsSnapshot()
        val hasAccess = NotificationAccessHelper.isNotificationAccessEnabled(app)
        if (hasAccess && settings.botActive) {
            KalaOrderBotKeepAliveService.startOrUpdate(app)
            NotificationAccessHelper.requestListenerRebind(app)
        } else if (!settings.botActive && KalaOrderBotKeepAliveService.isRunning.value) {
            KalaOrderBotKeepAliveService.stop(app)
        }
    }

    // Filter updates
    fun onQueueSearchChange(query: String) = filterState.update { it.copy(queueSearch = query) }
    fun onQueueFilterSelect(filter: QueueFilter) = filterState.update { it.copy(queueFilter = filter) }
    fun onMenuSearchChange(query: String) = filterState.update { it.copy(menuSearch = query) }
    fun onMenuFilterSelect(filter: MenuFilter) = filterState.update { it.copy(menuFilter = filter) }
    fun onHistorySearchChange(query: String) = filterState.update { it.copy(historySearch = query) }
    fun onHistoryFilterSelect(filter: HistoryFilter) = filterState.update { it.copy(historyFilter = filter) }

    // Finance filters & cashflow actions
    fun onFinancePeriodSelect(period: FinancePeriod) =
        filterState.update { it.copy(financePeriod = period) }

    fun onShiftFinanceMonth(deltaMonths: Int) =
        filterState.update {
            it.copy(
                financePeriod = FinancePeriod.MONTHLY,
                selectedFinanceMonth = it.selectedFinanceMonth.shiftMonths(deltaMonths),
            )
        }

    fun onSelectCurrentFinanceMonth() =
        filterState.update {
            it.copy(
                financePeriod = FinancePeriod.MONTHLY,
                selectedFinanceMonth = FinanceYearMonth.current(),
            )
        }

    fun onFinanceSearchChange(query: String) =
        filterState.update { it.copy(financeSearch = query) }

    fun saveCashflowEntry(entry: CashflowEntryRecord, isEdit: Boolean) {
        viewModelScope.launch {
            repository.saveCashflowEntry(entry)
            val verb = if (isEdit) "diperbarui" else "dicatat"
            _snackbarMessages.tryEmit("${entry.type.label} \"${entry.title}\" berhasil $verb")
        }
    }

    fun deleteCashflowEntry(entry: CashflowEntryRecord) {
        viewModelScope.launch {
            repository.deleteCashflowEntry(entry.id)
            _snackbarMessages.tryEmit("Catatan kas \"${entry.title}\" dihapus")
        }
    }

    fun buildCurrentRecapText(): String {
        val state = uiState.value
        return FinanceRecapEngine.buildWhatsAppRecapText(
            summary = state.financeSummary,
            storeName = state.botSettings.storeName,
        )
    }

    fun buildCurrentRecapCsv(): String {
        val state = uiState.value
        return FinanceRecapEngine.buildExcelCsvContent(
            summary = state.financeSummary,
            storeName = state.botSettings.storeName,
        )
    }

    fun suggestedCsvFileName(): String =
        FinanceRecapEngine.suggestedCsvFileName(uiState.value.financeSummary)

    // Order actions
    fun updateOrderStatus(order: OrderRecord, newStatus: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, newStatus)
            val msg = when (newStatus) {
                OrderStatus.DIPROSES -> "Pesanan ${order.orderCode} mulai diproses dapur"
                OrderStatus.SELESAI -> "Pesanan ${order.orderCode} selesai • Siap diambil pelanggan"
                OrderStatus.DIBATALKAN -> "Pesanan ${order.orderCode} dibatalkan"
                OrderStatus.MENUNGGU -> "Pesanan ${order.orderCode} dikembalikan ke antrean"
            }
            _snackbarMessages.tryEmit(msg)
        }
    }

    fun simulateWhatsAppNotification(
        senderName: String,
        senderPhone: String,
        incomingMessage: String,
        isGroupChat: Boolean,
        onResult: (BotReplyDecision) -> Unit,
    ) {
        viewModelScope.launch {
            val resolvedSender = senderName.ifBlank { "Pelanggan Uji" }
            val decision = autoReplyEngine.processIncomingMessage(
                senderName = resolvedSender,
                senderPhoneOrTag = senderPhone.ifBlank { "+62 812-0000-0000" },
                sourceAppLabel = "Simulasi WA",
                incomingMessage = incomingMessage,
                isGroupChat = isGroupChat,
                bypassDuplicateCheck = true,
            )
            onResult(decision)
            if (decision.createdOrderCode != null && decision.createdOrderTotal != null) {
                BotNotificationChannels.notifyNewOrder(
                    context = getApplication(),
                    orderCode = decision.createdOrderCode,
                    customerName = resolvedSender,
                    totalAmount = decision.createdOrderTotal,
                    itemSummary = decision.createdOrderSummary.orEmpty(),
                )
                _snackbarMessages.tryEmit(
                    "Order ${decision.createdOrderCode} masuk antrean & notifikasi berbunyi"
                )
            }
        }
    }

    fun submitNewOrder(
        customerName: String,
        customerPhone: String,
        channel: OrderChannel,
        items: List<OrderLineItem>,
        note: String,
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val code = repository.createOrder(
                customerName = customerName,
                customerPhone = customerPhone,
                channel = channel,
                items = items,
                note = note,
                existingOrderCount = uiState.value.orders.size,
            )
            val channelLabel = if (channel == OrderChannel.WHATSAPP) "WhatsApp" else "Kasir"
            _snackbarMessages.tryEmit("Pesanan baru $code ($channelLabel) masuk ke antrean")
        }
    }

    // Menu actions
    fun saveMenuItem(item: MenuItemRecord, isEdit: Boolean) {
        viewModelScope.launch {
            repository.saveMenuItem(item)
            val verb = if (isEdit) "diperbarui" else "ditambahkan"
            _snackbarMessages.tryEmit("Menu ${item.code} • ${item.name} berhasil $verb")
        }
    }

    fun saveMenuItemsBulk(items: List<MenuItemRecord>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.saveMenuItemsBulk(items)
            _snackbarMessages.tryEmit("${items.size} menu berhasil ditambahkan ke katalog")
        }
    }

    fun completeInitialSetup(
        storeName: String,
        whatsappNumber: String,
        initialCapitalAmount: Int,
        initialMenus: List<MenuItemRecord>,
    ) {
        viewModelScope.launch {
            repository.completeInitialSetup(
                storeName = storeName,
                whatsappNumber = whatsappNumber,
                initialCapitalAmount = initialCapitalAmount,
                initialMenus = initialMenus,
            )
            val summaryNote = if (initialMenus.isNotEmpty()) {
                "Toko siap digunakan dengan ${initialMenus.size} menu!"
            } else {
                "Toko siap digunakan dalam keadaan kosong bersih!"
            }
            _snackbarMessages.tryEmit(summaryNote)
        }
    }

    fun skipInitialSetup() {
        val current = uiState.value.botSettings
        viewModelScope.launch {
            repository.saveBotSettings(current.copy(setupCompleted = true))
            _snackbarMessages.tryEmit("Siap pakai! Kamu bisa mengisi menu kapan saja di tab Menu")
        }
    }

    fun resetSelectedData(selection: DataResetSelection) {
        if (!selection.hasAnySelection) return
        viewModelScope.launch {
            repository.resetSelectedData(selection)
            val clearedParts = buildList {
                if (selection.clearOrders) add("pesanan")
                if (selection.clearCashflow) add("catatan kas")
                if (selection.clearMenu) add("katalog menu")
                if (selection.clearBotLogs) add("log bot")
            }
            _snackbarMessages.tryEmit("Berhasil mengosongkan ${clearedParts.joinToString(", ")}")
        }
    }

    fun adjustMenuStock(code: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustMenuStock(code, delta)
        }
    }

    fun deleteMenuItem(item: MenuItemRecord) {
        viewModelScope.launch {
            repository.deleteMenuItem(item.code)
            _snackbarMessages.tryEmit("Menu ${item.code} • ${item.name} dihapus")
        }
    }

    // Store & Bot settings actions
    fun toggleStoreOpen() {
        val current = uiState.value.botSettings
        val next = current.copy(storeOpen = !current.storeOpen)
        viewModelScope.launch {
            repository.saveBotSettings(next)
            syncKeepAliveService()
            _snackbarMessages.tryEmit(
                if (next.storeOpen) "Status toko diubah ke TOKO BUKA"
                else "Status toko diubah ke TOKO TUTUP"
            )
        }
    }

    fun toggleBotActive() {
        val current = uiState.value.botSettings
        val next = current.copy(botActive = !current.botActive)
        viewModelScope.launch {
            repository.saveBotSettings(next)
            syncKeepAliveService()
            _snackbarMessages.tryEmit(
                if (next.botActive) "Bot WhatsApp & Siaga 24 Jam diaktifkan"
                else "Bot WhatsApp dijeda sementara"
            )
        }
    }

    fun updateBotSettings(updated: BotSettings, notifyMessage: String? = null) {
        viewModelScope.launch {
            repository.saveBotSettings(updated)
            syncKeepAliveService()
            if (notifyMessage != null) {
                _snackbarMessages.tryEmit(notifyMessage)
            }
        }
    }

    fun clearBotLogs() {
        viewModelScope.launch {
            repository.clearBotActivityLogs()
            _snackbarMessages.tryEmit("Log aktivitas bot dibersihkan")
        }
    }

    fun resetContactCooldowns() {
        viewModelScope.launch {
            repository.clearContactCooldowns()
            _snackbarMessages.tryEmit("Jeda salam semua kontak direset")
        }
    }

    private data class PermissionSnapshot(
        val notificationAccessGranted: Boolean,
        val postNotificationGranted: Boolean,
        val batteryOptimizationIgnored: Boolean,
    )

    private data class CoreDataSnapshot(
        val orders: List<OrderRecord>,
        val menuItems: List<MenuItemRecord>,
        val settings: BotSettings,
        val logs: List<BotActivityLogRecord>,
        val cashflowEntries: List<CashflowEntryRecord>,
    )
}
