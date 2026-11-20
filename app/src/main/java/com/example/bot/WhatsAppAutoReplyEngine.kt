package com.example.bot

import com.example.data.BotContactStateEntity
import com.example.data.BotLogOutcome
import com.example.data.BotSettings
import com.example.data.KalaOrderRepository
import com.example.data.MenuCategory
import com.example.data.MenuItemRecord
import com.example.data.OrderChannel
import com.example.data.OrderLineItem
import com.example.data.RupiahFormatter
import java.util.Locale

data class BotReplyDecision(
    val shouldReply: Boolean,
    val replyText: String? = null,
    val outcome: BotLogOutcome? = null,
    val createdOrderCode: String? = null,
    val createdOrderTotal: Int? = null,
    val createdOrderSummary: String? = null,
)

class WhatsAppAutoReplyEngine(
    private val repository: KalaOrderRepository,
) {
    companion object {
        private const val DUPLICATE_WINDOW_MS = 15_000L

        private val MENU_KEYWORDS = listOf(
            "menu", "daftar", "harga", "katalog", "list", "kode",
        )

        fun buildWelcomeAndMenuReply(
            settings: BotSettings,
            menuItems: List<MenuItemRecord>,
        ): String {
            val welcome = settings.welcomeMessageTemplate
                .replace("{TOKO}", settings.storeName)
                .trim()

            val sb = StringBuilder()
            sb.appendLine(welcome)
            sb.appendLine()

            val grouped = MenuCategory.entries.mapNotNull { cat ->
                val items = menuItems.filter { it.category == cat }
                if (items.isEmpty()) null else cat to items
            }

            grouped.forEach { (category, items) ->
                sb.appendLine("*${category.label.uppercase(Locale("in", "ID"))}*")
                items.forEach { item ->
                    val statusTag = if (item.isOutOfStock) " _(HABIS)_" else ""
                    sb.appendLine(
                        "• [${item.code}] ${item.name} — ${RupiahFormatter.format(item.price)}$statusTag"
                    )
                }
                sb.appendLine()
            }

            sb.appendLine("📝 *Cara Pesan:*")
            sb.appendLine("Balas dengan jumlah & kode produk.")
            sb.append("Contoh: *2 A1, 2 B1* (boleh tambah catatan pedas/bungkus)")
            return sb.toString().trim()
        }

        fun buildOrderReceiptReply(
            settings: BotSettings,
            orderCode: String,
            items: List<OrderLineItem>,
            totalAmount: Int,
        ): String {
            val header = settings.greetingTemplate
                .replace("#{KODE}", orderCode)
                .replace("{KODE}", orderCode)
                .replace("{TOKO}", settings.storeName)
                .trim()

            val sb = StringBuilder()
            sb.appendLine(header)
            sb.appendLine()
            sb.appendLine("🧾 *Kode Antrean:* $orderCode")
            items.forEach { line ->
                sb.appendLine(
                    "• ${line.quantity}x ${line.menuName} (${line.menuCode}) = ${RupiahFormatter.format(line.subtotal)}"
                )
            }
            sb.appendLine("💰 *Total:* ${RupiahFormatter.format(totalAmount)}")
            sb.append("Ketik *MENU* kapan saja untuk melihat daftar menu terbaru.")
            return sb.toString().trim()
        }

        fun buildClosedStoreReply(settings: BotSettings): String {
            return settings.closedStoreTemplate
                .replace("{TOKO}", settings.storeName)
                .trim()
        }

        fun buildOutOfStockReply(
            settings: BotSettings,
            unavailableItems: List<Pair<OrderLineItem, MenuItemRecord>>,
            availableMenu: List<MenuItemRecord>,
        ): String {
            val sb = StringBuilder()
            sb.appendLine("Mohon maaf Kak, stok di *${settings.storeName}* belum mencukupi untuk item berikut:")
            unavailableItems.forEach { (req, menu) ->
                if (menu.isOutOfStock) {
                    sb.appendLine("• [${menu.code}] ${menu.name} — *Stok habis*")
                } else {
                    sb.appendLine("• [${menu.code}] ${menu.name} — diminta ${req.quantity}, tersisa *${menu.stock}*")
                }
            }
            sb.appendLine()
            val ready = availableMenu.filter { !it.isOutOfStock }.take(6)
            if (ready.isNotEmpty()) {
                sb.appendLine("Menu yang masih tersedia:")
                ready.forEach { item ->
                    sb.appendLine("• [${item.code}] ${item.name} (${RupiahFormatter.format(item.price)})")
                }
                sb.append("Silakan kirim ulang kode pesanan ya Kak!")
            }
            return sb.toString().trim()
        }
    }

    suspend fun processIncomingMessage(
        senderName: String,
        senderPhoneOrTag: String,
        sourceAppLabel: String,
        incomingMessage: String,
        isGroupChat: Boolean,
        bypassDuplicateCheck: Boolean = false,
    ): BotReplyDecision {
        repository.ensureSeedData()
        val settings = repository.getBotSettingsSnapshot()
        val trimmedMessage = incomingMessage.trim()
        if (trimmedMessage.isEmpty()) {
            return BotReplyDecision(shouldReply = false)
        }

        // 1. Bot paused check
        if (!settings.botActive || !settings.autoReplyEnabled) {
            return BotReplyDecision(shouldReply = false)
        }

        // 2. Group chat anti-spam filter
        if (isGroupChat && settings.ignoreGroupChats) {
            repository.logBotActivity(
                senderName = senderName,
                sourceApp = sourceAppLabel,
                incomingMessage = trimmedMessage,
                replyPreview = "Diabaikan otomatis (chat grup)",
                outcome = BotLogOutcome.IGNORED_GROUP,
            )
            return BotReplyDecision(
                shouldReply = false,
                outcome = BotLogOutcome.IGNORED_GROUP,
            )
        }

        val now = System.currentTimeMillis()
        val contactKey = "${sourceAppLabel.lowercase()}:${senderName.trim().lowercase()}"
        val fingerprint = trimmedMessage.lowercase()
        val prevState = repository.getContactState(contactKey)

        // 3. Duplicate notification guard (prevents double-processing when WhatsApp updates stacked notifications)
        if (!bypassDuplicateCheck &&
            prevState != null &&
            prevState.lastMessageFingerprint == fingerprint &&
            (now - prevState.lastMessageAtMillis) < DUPLICATE_WINDOW_MS
        ) {
            return BotReplyDecision(shouldReply = false)
        }

        val cooldownMs = settings.welcomeCooldownMinutes.coerceAtLeast(1) * 60_000L
        val isWithinCooldown = prevState != null &&
            prevState.lastWelcomeAtMillis > 0L &&
            (now - prevState.lastWelcomeAtMillis) < cooldownMs

        // 4. Store closed check
        if (!settings.storeOpen) {
            if (isWithinCooldown) {
                repository.saveContactState(
                    BotContactStateEntity(
                        contactKey = contactKey,
                        lastWelcomeAtMillis = prevState?.lastWelcomeAtMillis ?: 0L,
                        lastMessageFingerprint = fingerprint,
                        lastMessageAtMillis = now,
                    )
                )
                repository.logBotActivity(
                    senderName = senderName,
                    sourceApp = sourceAppLabel,
                    incomingMessage = trimmedMessage,
                    replyPreview = "Masih dalam jeda cooldown (${settings.welcomeCooldownMinutes} mnt)",
                    outcome = BotLogOutcome.IGNORED_COOLDOWN,
                )
                return BotReplyDecision(shouldReply = false, outcome = BotLogOutcome.IGNORED_COOLDOWN)
            }

            val closedReply = buildClosedStoreReply(settings)
            repository.saveContactState(
                BotContactStateEntity(
                    contactKey = contactKey,
                    lastWelcomeAtMillis = now,
                    lastMessageFingerprint = fingerprint,
                    lastMessageAtMillis = now,
                )
            )
            repository.logBotActivity(
                senderName = senderName,
                sourceApp = sourceAppLabel,
                incomingMessage = trimmedMessage,
                replyPreview = closedReply,
                outcome = BotLogOutcome.SENT_CLOSED,
            )
            return BotReplyDecision(
                shouldReply = true,
                replyText = closedReply,
                outcome = BotLogOutcome.SENT_CLOSED,
            )
        }

        val menuItems = repository.getMenuSnapshot()

        // 5. Order code detection (always processed, never blocked by welcome cooldown)
        val parsedItems = repository.parseWhatsAppMessage(trimmedMessage, menuItems)
        if (parsedItems.isNotEmpty()) {
            val catalogByCode = menuItems.associateBy { it.code.uppercase() }
            val unavailable = parsedItems.mapNotNull { line ->
                val menu = catalogByCode[line.menuCode.uppercase()] ?: return@mapNotNull null
                if (menu.isOutOfStock || menu.stock < line.quantity) line to menu else null
            }

            if (unavailable.isNotEmpty()) {
                val stockReply = buildOutOfStockReply(settings, unavailable, menuItems)
                repository.saveContactState(
                    BotContactStateEntity(
                        contactKey = contactKey,
                        lastWelcomeAtMillis = prevState?.lastWelcomeAtMillis ?: 0L,
                        lastMessageFingerprint = fingerprint,
                        lastMessageAtMillis = now,
                    )
                )
                repository.logBotActivity(
                    senderName = senderName,
                    sourceApp = sourceAppLabel,
                    incomingMessage = trimmedMessage,
                    replyPreview = stockReply,
                    outcome = BotLogOutcome.OUT_OF_STOCK,
                )
                return BotReplyDecision(
                    shouldReply = true,
                    replyText = stockReply,
                    outcome = BotLogOutcome.OUT_OF_STOCK,
                )
            }

            val orderCode = repository.createOrder(
                customerName = senderName,
                customerPhone = senderPhoneOrTag,
                channel = OrderChannel.WHATSAPP,
                items = parsedItems,
                note = trimmedMessage,
            )
            val total = parsedItems.sumOf { it.subtotal }
            val receiptReply = buildOrderReceiptReply(settings, orderCode, parsedItems, total)

            repository.saveContactState(
                BotContactStateEntity(
                    contactKey = contactKey,
                    lastWelcomeAtMillis = now,
                    lastMessageFingerprint = fingerprint,
                    lastMessageAtMillis = now,
                )
            )
            repository.logBotActivity(
                senderName = senderName,
                sourceApp = sourceAppLabel,
                incomingMessage = trimmedMessage,
                replyPreview = "Order $orderCode (${RupiahFormatter.format(total)}) • Struk dikirim",
                outcome = BotLogOutcome.CREATED_ORDER,
            )
            val itemSummary = parsedItems.joinToString(", ") { "${it.quantity}x ${it.menuName}" }
            return BotReplyDecision(
                shouldReply = true,
                replyText = receiptReply,
                outcome = BotLogOutcome.CREATED_ORDER,
                createdOrderCode = orderCode,
                createdOrderTotal = total,
                createdOrderSummary = itemSummary,
            )
        }

        // 6. Explicit MENU keyword check (always replies even inside cooldown)
        val lower = trimmedMessage.lowercase(Locale("in", "ID"))
        val asksForMenu = MENU_KEYWORDS.any { keyword ->
            Regex("""\b$keyword\b""").containsMatchIn(lower)
        }

        if (!asksForMenu && isWithinCooldown) {
            repository.saveContactState(
                BotContactStateEntity(
                    contactKey = contactKey,
                    lastWelcomeAtMillis = prevState?.lastWelcomeAtMillis ?: 0L,
                    lastMessageFingerprint = fingerprint,
                    lastMessageAtMillis = now,
                )
            )
            repository.logBotActivity(
                senderName = senderName,
                sourceApp = sourceAppLabel,
                incomingMessage = trimmedMessage,
                replyPreview = "Diabaikan • Cooldown salam ${settings.welcomeCooldownMinutes} mnt aktif",
                outcome = BotLogOutcome.IGNORED_COOLDOWN,
            )
            return BotReplyDecision(
                shouldReply = false,
                outcome = BotLogOutcome.IGNORED_COOLDOWN,
            )
        }

        // 7. Send Welcome + Dynamic Menu & Product Codes
        val welcomeReply = buildWelcomeAndMenuReply(settings, menuItems)
        repository.saveContactState(
            BotContactStateEntity(
                contactKey = contactKey,
                lastWelcomeAtMillis = now,
                lastMessageFingerprint = fingerprint,
                lastMessageAtMillis = now,
            )
        )
        repository.logBotActivity(
            senderName = senderName,
            sourceApp = sourceAppLabel,
            incomingMessage = trimmedMessage,
            replyPreview = welcomeReply,
            outcome = BotLogOutcome.SENT_WELCOME,
        )
        return BotReplyDecision(
            shouldReply = true,
            replyText = welcomeReply,
            outcome = BotLogOutcome.SENT_WELCOME,
        )
    }
}
