package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val code: String,
    val name: String,
    val category: String,
    val price: Int,
    val stock: Int,
    val isAvailable: Boolean,
) {
    fun toRecord(): MenuItemRecord = MenuItemRecord(
        code = code,
        name = name,
        category = MenuCategory.fromKey(category),
        price = price,
        stock = stock,
        isAvailable = isAvailable,
    )

    companion object {
        fun fromRecord(record: MenuItemRecord): MenuItemEntity = MenuItemEntity(
            code = record.code.trim().uppercase(),
            name = record.name.trim(),
            category = record.category.name,
            price = record.price.coerceAtLeast(0),
            stock = record.stock.coerceAtLeast(0),
            isAvailable = record.isAvailable,
        )
    }
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderCode: String,
    val customerName: String,
    val customerPhone: String,
    val channel: String,
    val status: String,
    val itemsJson: String,
    val totalAmount: Int,
    val note: String,
    val timestampLabel: String,
    val createdAtMillis: Long,
) {
    fun toRecord(): OrderRecord = OrderRecord(
        id = id,
        orderCode = orderCode,
        customerName = customerName,
        customerPhone = customerPhone,
        channel = OrderChannel.fromKey(channel),
        status = OrderStatus.fromKey(status),
        items = decodeLineItems(itemsJson),
        totalAmount = totalAmount,
        note = note,
        timestampLabel = timestampLabel,
        createdAtMillis = createdAtMillis,
    )

    companion object {
        fun encodeLineItems(items: List<OrderLineItem>): String {
            val array = JSONArray()
            items.forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("code", item.menuCode)
                        put("name", item.menuName)
                        put("qty", item.quantity)
                        put("price", item.unitPrice)
                    }
                )
            }
            return array.toString()
        }

        fun decodeLineItems(raw: String): List<OrderLineItem> {
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(
                            OrderLineItem(
                                menuCode = obj.optString("code", ""),
                                menuName = obj.optString("name", ""),
                                quantity = obj.optInt("qty", 1),
                                unitPrice = obj.optInt("price", 0),
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}

@Entity(tableName = "bot_settings")
data class BotSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String,
    val storeOpen: Boolean,
    val botActive: Boolean,
    val autoReplyEnabled: Boolean,
    val autoNotifyReady: Boolean,
    val whatsappNumber: String,
    val greetingTemplate: String,
    val listenWhatsApp: Boolean = true,
    val listenWhatsAppBusiness: Boolean = true,
    val ignoreGroupChats: Boolean = true,
    val welcomeCooldownMinutes: Int = 30,
    val welcomeMessageTemplate: String = "Selamat datang di {TOKO}! 👋\nSilakan pilih menu favorit Kakak hari ini:",
    val closedStoreTemplate: String = "Mohon maaf Kak, {TOKO} saat ini sedang TUTUP. Pesanan akan kami layani kembali saat toko buka ya!",
) {
    fun toDomain(): BotSettings = BotSettings(
        storeName = storeName,
        storeOpen = storeOpen,
        botActive = botActive,
        autoReplyEnabled = autoReplyEnabled,
        autoNotifyReady = autoNotifyReady,
        whatsappNumber = whatsappNumber,
        greetingTemplate = greetingTemplate,
        listenWhatsApp = listenWhatsApp,
        listenWhatsAppBusiness = listenWhatsAppBusiness,
        ignoreGroupChats = ignoreGroupChats,
        welcomeCooldownMinutes = welcomeCooldownMinutes,
        welcomeMessageTemplate = welcomeMessageTemplate,
        closedStoreTemplate = closedStoreTemplate,
    )

    companion object {
        fun fromDomain(settings: BotSettings): BotSettingsEntity = BotSettingsEntity(
            id = 1,
            storeName = settings.storeName,
            storeOpen = settings.storeOpen,
            botActive = settings.botActive,
            autoReplyEnabled = settings.autoReplyEnabled,
            autoNotifyReady = settings.autoNotifyReady,
            whatsappNumber = settings.whatsappNumber,
            greetingTemplate = settings.greetingTemplate,
            listenWhatsApp = settings.listenWhatsApp,
            listenWhatsAppBusiness = settings.listenWhatsAppBusiness,
            ignoreGroupChats = settings.ignoreGroupChats,
            welcomeCooldownMinutes = settings.welcomeCooldownMinutes,
            welcomeMessageTemplate = settings.welcomeMessageTemplate,
            closedStoreTemplate = settings.closedStoreTemplate,
        )
    }
}

@Entity(tableName = "bot_contact_state")
data class BotContactStateEntity(
    @PrimaryKey val contactKey: String,
    val lastWelcomeAtMillis: Long,
    val lastMessageFingerprint: String,
    val lastMessageAtMillis: Long,
)

@Entity(tableName = "bot_activity_logs")
data class BotActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val sourceApp: String,
    val incomingMessage: String,
    val replyPreview: String,
    val outcome: String,
    val timestampLabel: String,
    val createdAtMillis: Long,
) {
    fun toRecord(): BotActivityLogRecord = BotActivityLogRecord(
        id = id,
        senderName = senderName,
        sourceApp = sourceApp,
        incomingMessage = incomingMessage,
        replyPreview = replyPreview,
        outcome = BotLogOutcome.fromKey(outcome),
        timestampLabel = timestampLabel,
        createdAtMillis = createdAtMillis,
    )
}
