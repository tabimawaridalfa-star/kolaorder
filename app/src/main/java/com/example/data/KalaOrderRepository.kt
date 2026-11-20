package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KalaOrderRepository(
    private val menuItemDao: MenuItemDao,
    private val orderDao: OrderDao,
    private val botSettingsDao: BotSettingsDao,
    private val botContactStateDao: BotContactStateDao,
    private val botActivityLogDao: BotActivityLogDao,
) {
    val menuItems: Flow<List<MenuItemRecord>> = menuItemDao.observeAll().map { list ->
        list.map { it.toRecord() }
    }

    val orders: Flow<List<OrderRecord>> = orderDao.observeAll().map { list ->
        list.map { it.toRecord() }
    }

    val botSettings: Flow<BotSettings> = botSettingsDao.observe().map { entity ->
        entity?.toDomain() ?: BotSettings()
    }

    val botActivityLogs: Flow<List<BotActivityLogRecord>> = botActivityLogDao.observeRecent().map { list ->
        list.map { it.toRecord() }
    }

    suspend fun ensureSeedData() {
        if (botSettingsDao.get() == null) {
            botSettingsDao.save(BotSettingsEntity.fromDomain(BotSettings()))
        }

        if (menuItemDao.count() == 0) {
            menuItemDao.upsertAll(initialSeedMenu())
        }

        if (orderDao.count() == 0) {
            orderDao.insertAll(initialSeedOrders())
        }
    }

    suspend fun getBotSettingsSnapshot(): BotSettings {
        return botSettingsDao.get()?.toDomain() ?: BotSettings()
    }

    suspend fun getMenuSnapshot(): List<MenuItemRecord> {
        return menuItemDao.getAllSnapshot().map { it.toRecord() }
    }

    suspend fun getOrderCountSnapshot(): Int = orderDao.count()

    suspend fun getContactState(contactKey: String): BotContactStateEntity? =
        botContactStateDao.getByKey(contactKey)

    suspend fun saveContactState(state: BotContactStateEntity) =
        botContactStateDao.upsert(state)

    suspend fun clearContactCooldowns() = botContactStateDao.clearAll()

    suspend fun logBotActivity(
        senderName: String,
        sourceApp: String,
        incomingMessage: String,
        replyPreview: String,
        outcome: BotLogOutcome,
    ) {
        val now = System.currentTimeMillis()
        val timeLabel = SimpleDateFormat("HH:mm", Locale("in", "ID")).format(Date(now))
        botActivityLogDao.insert(
            BotActivityLogEntity(
                senderName = senderName.ifBlank { "Kontak WA" },
                sourceApp = sourceApp,
                incomingMessage = incomingMessage.take(160),
                replyPreview = replyPreview.take(240),
                outcome = outcome.name,
                timestampLabel = timeLabel,
                createdAtMillis = now,
            )
        )
    }

    suspend fun clearBotActivityLogs() {
        botActivityLogDao.clearAll()
    }

    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        orderDao.updateStatus(orderId, status.name)
    }

    suspend fun saveMenuItem(item: MenuItemRecord) {
        menuItemDao.upsert(MenuItemEntity.fromRecord(item))
    }

    suspend fun adjustMenuStock(code: String, delta: Int) {
        val current = menuItemDao.getByCode(code) ?: return
        val nextStock = (current.stock + delta).coerceAtLeast(0)
        menuItemDao.updateStock(code, nextStock)
    }

    suspend fun deleteMenuItem(code: String) {
        menuItemDao.deleteByCode(code)
    }

    suspend fun saveBotSettings(settings: BotSettings) {
        botSettingsDao.save(BotSettingsEntity.fromDomain(settings))
    }

    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        channel: OrderChannel,
        items: List<OrderLineItem>,
        note: String,
        existingOrderCount: Int? = null,
    ): String {
        val baseCount = existingOrderCount ?: orderDao.count()
        val nextNumber = 140 + baseCount + 1
        val orderCode = "KO-${nextNumber.toString().padStart(4, '0')}"
        val total = items.sumOf { it.subtotal }
        val now = System.currentTimeMillis()
        val timeLabel = SimpleDateFormat("HH:mm", Locale("in", "ID")).format(Date(now))

        val entity = OrderEntity(
            orderCode = orderCode,
            customerName = customerName.ifBlank { "Pelanggan Walk-in" },
            customerPhone = customerPhone.ifBlank { "-" },
            channel = channel.name,
            status = OrderStatus.MENUNGGU.name,
            itemsJson = OrderEntity.encodeLineItems(items),
            totalAmount = total,
            note = note.trim(),
            timestampLabel = timeLabel,
            createdAtMillis = now,
        )
        orderDao.insert(entity)

        // Decrement stock for ordered menu items
        items.forEach { line ->
            val menu = menuItemDao.getByCode(line.menuCode)
            if (menu != null) {
                val updatedStock = (menu.stock - line.quantity).coerceAtLeast(0)
                menuItemDao.updateStock(menu.code, updatedStock)
            }
        }
        return orderCode
    }

    /**
     * Parses Indonesian WhatsApp shorthand orders such as "2 A1, 1 B1" or "A2 1, B2 2".
     */
    fun parseWhatsAppMessage(
        message: String,
        catalog: List<MenuItemRecord>,
    ): List<OrderLineItem> {
        if (message.isBlank() || catalog.isEmpty()) return emptyList()
        val catalogByCode = catalog.associateBy { it.code.uppercase() }
        val quantities = linkedMapOf<String, Int>()

        val qtyFirstRegex = Regex("""(\d+)\s*[xX*]?\s*([A-Za-z]\d+)\b""")
        val codeFirstRegex = Regex("""\b([A-Za-z]\d+)\s*[xX*:\-]?\s*(\d+)\b""")

        val matchedSpans = mutableListOf<IntRange>()
        qtyFirstRegex.findAll(message).forEach { match ->
            val qty = match.groupValues[1].toIntOrNull()?.coerceIn(1, 99) ?: 1
            val code = match.groupValues[2].uppercase()
            if (catalogByCode.containsKey(code)) {
                quantities[code] = (quantities[code] ?: 0) + qty
                matchedSpans.add(match.range)
            }
        }

        codeFirstRegex.findAll(message).forEach { match ->
            val overlaps = matchedSpans.any { span ->
                match.range.first <= span.last && span.first <= match.range.last
            }
            if (!overlaps) {
                val code = match.groupValues[1].uppercase()
                val qty = match.groupValues[2].toIntOrNull()?.coerceIn(1, 99) ?: 1
                if (catalogByCode.containsKey(code)) {
                    quantities[code] = (quantities[code] ?: 0) + qty
                    matchedSpans.add(match.range)
                }
            }
        }

        // Fallback: bare menu codes like "A1 sama B1" -> quantity 1
        if (quantities.isEmpty()) {
            val bareCodeRegex = Regex("""\b([A-Za-z]\d+)\b""")
            bareCodeRegex.findAll(message).forEach { match ->
                val code = match.groupValues[1].uppercase()
                if (catalogByCode.containsKey(code)) {
                    quantities[code] = (quantities[code] ?: 0) + 1
                }
            }
        }

        return quantities.mapNotNull { (code, qty) ->
            val item = catalogByCode[code] ?: return@mapNotNull null
            OrderLineItem(
                menuCode = item.code,
                menuName = item.name,
                quantity = qty,
                unitPrice = item.price,
            )
        }
    }

    private fun initialSeedMenu(): List<MenuItemEntity> = listOf(
        MenuItemEntity("A1", "Nasi Goreng Spesial", MenuCategory.MAKANAN.name, 22000, 18, true),
        MenuItemEntity("A2", "Ayam Geprek Sambal Matah", MenuCategory.MAKANAN.name, 20000, 14, true),
        MenuItemEntity("A3", "Mie Goreng Jawa Kumplit", MenuCategory.MAKANAN.name, 18000, 4, true),
        MenuItemEntity("A4", "Soto Ayam Lamongan", MenuCategory.MAKANAN.name, 19000, 11, true),
        MenuItemEntity("B1", "Es Teh Manis Jumbo", MenuCategory.MINUMAN.name, 6000, 32, true),
        MenuItemEntity("B2", "Es Jeruk Peras Segar", MenuCategory.MINUMAN.name, 8000, 20, true),
        MenuItemEntity("B3", "Kopi Susu Gula Aren", MenuCategory.MINUMAN.name, 15000, 3, true),
        MenuItemEntity("C1", "Tahu Crispy Lada Garam", MenuCategory.CAMILAN.name, 12000, 15, true),
        MenuItemEntity("C2", "Pisang Goreng Cokelat Keju", MenuCategory.CAMILAN.name, 14000, 0, false),
    )

    private fun initialSeedOrders(): List<OrderEntity> {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        return listOf(
            OrderEntity(
                orderCode = "KO-0145",
                customerName = "Rina Kusuma",
                customerPhone = "+62 813-8821-0094",
                channel = OrderChannel.WHATSAPP.name,
                status = OrderStatus.MENUNGGU.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("A1", "Nasi Goreng Spesial", 2, 22000),
                        OrderLineItem("B1", "Es Teh Manis Jumbo", 2, 6000),
                    )
                ),
                totalAmount = 56000,
                note = "Pedas sedang, tanpa acar timun",
                timestampLabel = "12:18",
                createdAtMillis = now - 2 * minute,
            ),
            OrderEntity(
                orderCode = "KO-0144",
                customerName = "Budi Santoso",
                customerPhone = "+62 857-2104-5531",
                channel = OrderChannel.WHATSAPP.name,
                status = OrderStatus.MENUNGGU.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("A2", "Ayam Geprek Sambal Matah", 1, 20000),
                        OrderLineItem("C1", "Tahu Crispy Lada Garam", 1, 12000),
                        OrderLineItem("B2", "Es Jeruk Peras Segar", 1, 8000),
                    )
                ),
                totalAmount = 40000,
                note = "Sambal dipisah, ambil jam 12:35",
                timestampLabel = "12:14",
                createdAtMillis = now - 6 * minute,
            ),
            OrderEntity(
                orderCode = "KO-0143",
                customerName = "Dimas Pratama",
                customerPhone = "Meja 04",
                channel = OrderChannel.KASIR.name,
                status = OrderStatus.DIPROSES.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("A3", "Mie Goreng Jawa Kumplit", 2, 18000),
                        OrderLineItem("B3", "Kopi Susu Gula Aren", 1, 15000),
                    )
                ),
                totalAmount = 51000,
                note = "Makan di tempat, kuah dipisah",
                timestampLabel = "12:08",
                createdAtMillis = now - 12 * minute,
            ),
            OrderEntity(
                orderCode = "KO-0142",
                customerName = "Siti Rahmawati",
                customerPhone = "+62 812-9045-3110",
                channel = OrderChannel.WHATSAPP.name,
                status = OrderStatus.DIPROSES.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("A4", "Soto Ayam Lamongan", 3, 19000),
                        OrderLineItem("B1", "Es Teh Manis Jumbo", 3, 6000),
                    )
                ),
                totalAmount = 75000,
                note = "Koya dibanyakin ya kak",
                timestampLabel = "12:02",
                createdAtMillis = now - 18 * minute,
            ),
            OrderEntity(
                orderCode = "KO-0141",
                customerName = "Hendra Wijaya",
                customerPhone = "+62 818-4402-7721",
                channel = OrderChannel.WHATSAPP.name,
                status = OrderStatus.SELESAI.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("A1", "Nasi Goreng Spesial", 3, 22000),
                        OrderLineItem("A2", "Ayam Geprek Sambal Matah", 2, 20000),
                        OrderLineItem("B1", "Es Teh Manis Jumbo", 4, 6000),
                    )
                ),
                totalAmount = 130000,
                note = "Bungkus rapat untuk kantor",
                timestampLabel = "11:46",
                createdAtMillis = now - 34 * minute,
            ),
            OrderEntity(
                orderCode = "KO-0140",
                customerName = "Ayu Lestari",
                customerPhone = "Takeaway Kasir",
                channel = OrderChannel.KASIR.name,
                status = OrderStatus.SELESAI.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("A4", "Soto Ayam Lamongan", 2, 19000),
                        OrderLineItem("B2", "Es Jeruk Peras Segar", 2, 8000),
                    )
                ),
                totalAmount = 54000,
                note = "",
                timestampLabel = "11:25",
                createdAtMillis = now - 55 * minute,
            ),
            OrderEntity(
                orderCode = "KO-0139",
                customerName = "Fajar Nugroho",
                customerPhone = "+62 821-6619-8320",
                channel = OrderChannel.WHATSAPP.name,
                status = OrderStatus.DIBATALKAN.name,
                itemsJson = OrderEntity.encodeLineItems(
                    listOf(
                        OrderLineItem("C2", "Pisang Goreng Cokelat Keju", 2, 14000),
                    )
                ),
                totalAmount = 28000,
                note = "Stok pisang goreng habis",
                timestampLabel = "11:10",
                createdAtMillis = now - 70 * minute,
            ),
        )
    }

    companion object {
        fun fromContext(context: Context): KalaOrderRepository {
            val db = KalaOrderDatabase.getInstance(context)
            return KalaOrderRepository(
                menuItemDao = db.menuItemDao(),
                orderDao = db.orderDao(),
                botSettingsDao = db.botSettingsDao(),
                botContactStateDao = db.botContactStateDao(),
                botActivityLogDao = db.botActivityLogDao(),
            )
        }
    }
}
