package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bot.BotReplyDecision
import com.example.data.BotLogOutcome
import com.example.data.BotSettings
import com.example.data.MenuCategory
import com.example.data.MenuItemRecord
import com.example.data.OrderChannel
import com.example.data.OrderLineItem
import com.example.data.OrderRecord
import com.example.data.OrderStatus
import com.example.data.RupiahFormatter
import com.example.ui.components.KalaBottomSheetDragHandle
import com.example.ui.components.KalaDestructiveOutlineButton
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.components.MonospaceCodeTag
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.SemanticPillBadge
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimarySoft
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailBottomSheet(
    order: OrderRecord,
    botSettings: BotSettings,
    onDismiss: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit,
    onSendWhatsAppStatus: (OrderRecord, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val statusReplyMessage = remember(order, botSettings) {
        when (order.status) {
            OrderStatus.SELESAI ->
                "Halo Kak ${order.customerName}, pesanan *${order.orderCode}* (${RupiahFormatter.format(order.totalAmount)}) sudah *SIAP DIAMBIL* di ${botSettings.storeName}. Terima kasih!"
            OrderStatus.DIPROSES ->
                "Halo Kak ${order.customerName}, pesanan *${order.orderCode}* sedang *DIPROSES* dapur ${botSettings.storeName}. Mohon ditunggu ya!"
            else ->
                botSettings.greetingTemplate
                    .replace("#{KODE}", order.orderCode)
                    .replace("{KODE}", order.orderCode)
                    .replace("{TOKO}", botSettings.storeName)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = KalaOrderShapes.Sheet,
        containerColor = SurfaceWhite,
        dragHandle = { KalaBottomSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MonospaceCodeTag(code = order.orderCode, emphasized = true)
                        Text(
                            text = order.timestampLabel,
                            style = KalaOrderMonoStyles.CodeMedium,
                            color = TextSecondary,
                        )
                    }
                    OrderStatusBadge(status = order.status)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "${order.channel.label} • ${order.customerPhone}",
                        style = KalaOrderMonoStyles.CodeSmall,
                        color = TextSecondary,
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                // Receipt items
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rincian Pesanan",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                    )
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                MonospaceCodeTag(code = item.menuCode)
                                Column {
                                    Text(
                                        text = item.menuName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = "${item.quantity} x ${RupiahFormatter.format(item.unitPrice)}",
                                        style = KalaOrderMonoStyles.CodeSmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                            Text(
                                text = RupiahFormatter.format(item.subtotal),
                                style = KalaOrderMonoStyles.CodeMedium,
                                color = TextPrimary,
                            )
                        }
                    }
                }

                if (order.note.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FillSubtle, KalaOrderShapes.Medium)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "Catatan pelanggan: ${order.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = RupiahFormatter.format(order.totalAmount),
                        style = KalaOrderMonoStyles.MetricLarge,
                        color = PrimaryEmerald,
                    )
                }

                if (order.channel == OrderChannel.WHATSAPP) {
                    Surface(
                        shape = KalaOrderShapes.Medium,
                        color = PrimarySoft,
                        border = BorderStroke(1.dp, StatusSelesaiBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Pesan Status WhatsApp Pelanggan",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryEmerald,
                            )
                            Text(
                                text = statusReplyMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                            KalaSecondaryButton(
                                text = stringResource(R.string.btn_kirim_wa),
                                onClick = { onSendWhatsAppStatus(order, statusReplyMessage) },
                                icon = Icons.AutoMirrored.Outlined.Send,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_whatsapp_status_button"),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Docked full-width action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (order.status) {
                    OrderStatus.MENUNGGU -> {
                        KalaDestructiveOutlineButton(
                            text = stringResource(R.string.action_tolak),
                            onClick = {
                                onUpdateStatus(OrderStatus.DIBATALKAN)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        KalaPrimaryButton(
                            text = stringResource(R.string.action_proses_pesanan),
                            onClick = {
                                onUpdateStatus(OrderStatus.DIPROSES)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.5f),
                        )
                    }
                    OrderStatus.DIPROSES -> {
                        KalaSecondaryButton(
                            text = stringResource(R.string.action_tutup),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        KalaPrimaryButton(
                            text = stringResource(R.string.action_tandai_selesai),
                            onClick = {
                                onUpdateStatus(OrderStatus.SELESAI)
                                onDismiss()
                            },
                            icon = Icons.Rounded.Check,
                            modifier = Modifier.weight(1.5f),
                        )
                    }
                    OrderStatus.SELESAI, OrderStatus.DIBATALKAN -> {
                        KalaPrimaryButton(
                            text = stringResource(R.string.action_tutup),
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderBottomSheet(
    initialChannel: OrderChannel,
    menuItems: List<MenuItemRecord>,
    onDismiss: () -> Unit,
    onSimulateNotification: (String, String, String, Boolean, (BotReplyDecision) -> Unit) -> Unit,
    onSubmitManualOrder: (String, String, OrderChannel, List<OrderLineItem>, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var channel by remember { mutableStateOf(initialChannel) }
    var customerName by remember {
        mutableStateOf(if (initialChannel == OrderChannel.WHATSAPP) "Dewi Anggraini" else "Pelanggan Meja 02")
    }
    var customerPhone by remember {
        mutableStateOf(if (initialChannel == OrderChannel.WHATSAPP) "+62 812-7740-9912" else "Meja 02")
    }
    var waMessage by remember { mutableStateOf("Halo kak, ada menu apa hari ini?") }
    var simulateGroupChat by remember { mutableStateOf(false) }
    var lastBotDecision by remember { mutableStateOf<BotReplyDecision?>(null) }

    var note by remember { mutableStateOf("Pedas sedang, sambal dipisah") }
    val manualQtyMap = remember { mutableStateMapOf<String, Int>() }

    val manualItems = menuItems.mapNotNull { menu ->
        val qty = manualQtyMap[menu.code] ?: 0
        if (qty > 0) OrderLineItem(menu.code, menu.name, qty, menu.price) else null
    }
    val manualTotalAmount = manualItems.sumOf { it.subtotal }

    val quickWaPresets = listOf(
        "Sapaan (Menu)" to "Halo kak, ada menu apa hari ini?",
        "Order 2 A1, 2 B1" to "Pesan 2 A1 pedas sedang, 2 B1 es teh",
        "Kata MENU" to "MENU",
        "Stok Habis (C2)" to "Kak pesan 2 C2 ya",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = KalaOrderShapes.Sheet,
        containerColor = SurfaceWhite,
        dragHandle = { KalaBottomSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (channel == OrderChannel.WHATSAPP) {
                        "Simulasi Notifikasi & Bot WA"
                    } else {
                        "Buat Pesanan Kasir"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )

                // Channel switcher chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KalaFilterChip(
                        label = "Simulasi Notif WA",
                        selected = channel == OrderChannel.WHATSAPP,
                        onClick = { channel = OrderChannel.WHATSAPP },
                        modifier = Modifier.testTag("sheet_mode_whatsapp"),
                    )
                    KalaFilterChip(
                        label = "Kasir Manual",
                        selected = channel == OrderChannel.KASIR,
                        onClick = { channel = OrderChannel.KASIR },
                        modifier = Modifier.testTag("sheet_mode_kasir"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nama Pengirim") },
                        singleLine = true,
                        shape = KalaOrderShapes.Medium,
                        colors = kalaFieldColors(),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("order_customer_name_input"),
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text(if (channel == OrderChannel.WHATSAPP) "No. WA" else "Meja / Ket") },
                        singleLine = true,
                        shape = KalaOrderShapes.Medium,
                        colors = kalaFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }

                if (channel == OrderChannel.WHATSAPP) {
                    Text(
                        text = "Skenario Uji Cepat (Mesin Bot Asli)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickWaPresets) { (label, msg) ->
                            Surface(
                                onClick = { waMessage = msg },
                                shape = KalaOrderShapes.Pill,
                                color = PrimarySoft,
                                border = BorderStroke(1.dp, StatusSelesaiBorder),
                            ) {
                                Text(
                                    text = label,
                                    style = KalaOrderMonoStyles.CodeSmall,
                                    color = PrimaryEmerald,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = waMessage,
                        onValueChange = { waMessage = it },
                        label = { Text("Isi Notifikasi WhatsApp Masuk") },
                        minLines = 2,
                        maxLines = 3,
                        shape = KalaOrderShapes.Medium,
                        colors = kalaFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wa_simulator_message_input"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulasikan Sebagai Chat Grup",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                            )
                            Text(
                                text = "Uji filter anti-grup agar bot tidak membalas grup",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                        Switch(
                            checked = simulateGroupChat,
                            onCheckedChange = { simulateGroupChat = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurfaceWhite,
                                checkedTrackColor = PrimaryEmerald,
                            ),
                        )
                    }

                    // Live bot reply output bubble
                    lastBotDecision?.let { decision ->
                        val outcome = decision.outcome
                        val (bg, fg, border) = when (outcome) {
                            BotLogOutcome.CREATED_ORDER -> Triple(StatusSelesaiBg, StatusSelesaiText, StatusSelesaiBorder)
                            BotLogOutcome.SENT_WELCOME -> Triple(StatusDiprosesBg, StatusDiprosesText, StatusDiprosesBorder)
                            BotLogOutcome.OUT_OF_STOCK, BotLogOutcome.SENT_CLOSED, BotLogOutcome.IGNORED_COOLDOWN ->
                                Triple(StatusMenungguBg, StatusMenungguText, StatusMenungguBorder)
                            else -> Triple(FillSubtle, TextSecondary, BorderSubtle)
                        }

                        Surface(
                            shape = KalaOrderShapes.Card,
                            color = if (decision.shouldReply) PrimarySoft else FillSubtle,
                            border = BorderStroke(1.dp, if (decision.shouldReply) StatusSelesaiBorder else BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wa_simulator_reply_preview"),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = PrimaryEmerald,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = if (decision.shouldReply) {
                                                "Balasan Direct Reply Bot"
                                            } else {
                                                "Tidak Dibalas Otomatis"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (decision.shouldReply) PrimaryEmerald else TextSecondary,
                                        )
                                    }
                                    if (outcome != null) {
                                        SemanticPillBadge(
                                            text = outcome.label,
                                            backgroundColor = bg,
                                            textColor = fg,
                                            borderColor = border,
                                        )
                                    }
                                }

                                Text(
                                    text = decision.replyText ?: when (outcome) {
                                        BotLogOutcome.IGNORED_GROUP ->
                                            "Chat grup diabaikan sesuai pengaturan anti-spam."
                                        BotLogOutcome.IGNORED_COOLDOWN ->
                                            "Kontak masih dalam masa jeda cooldown salam. Coba ketik \"MENU\" atau kode order seperti \"2 A1, 2 B1\"."
                                        else ->
                                            "Bot sedang dijeda atau tidak membalas pesan ini."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Pilih Menu",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    val availableMenus = menuItems.filter { !it.isOutOfStock }
                    if (availableMenus.isEmpty()) {
                        Surface(
                            shape = KalaOrderShapes.Medium,
                            color = FillSubtle,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Belum ada menu yang tersedia. Silakan isi daftar menu terlebih dahulu di tab Menu (bisa tambah satuan, isi banyak sekaligus, atau pilih template).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableMenus.forEach { menu ->
                                val qty = manualQtyMap[menu.code] ?: 0
                                Surface(
                                    shape = KalaOrderShapes.Medium,
                                    color = if (qty > 0) PrimarySoft else SurfaceWhite,
                                    border = BorderStroke(1.dp, if (qty > 0) StatusSelesaiBorder else BorderSubtle),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        MonospaceCodeTag(code = menu.code)
                                        Column {
                                            Text(
                                                text = menu.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                            )
                                            Text(
                                                text = "${RupiahFormatter.format(menu.price)} • Stok ${menu.stock}",
                                                style = KalaOrderMonoStyles.CodeSmall,
                                                color = TextSecondary,
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        MiniStepperButton(
                                            icon = Icons.Rounded.Remove,
                                            enabled = qty > 0,
                                            contentDescription = "Kurangi ${menu.name}",
                                            onClick = { manualQtyMap[menu.code] = (qty - 1).coerceAtLeast(0) },
                                        )
                                        Text(
                                            text = qty.toString(),
                                            style = KalaOrderMonoStyles.CodeMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                        )
                                        MiniStepperButton(
                                            icon = Icons.Rounded.Add,
                                            enabled = qty < menu.stock,
                                            contentDescription = "Tambah ${menu.name}",
                                            onClick = { manualQtyMap[menu.code] = qty + 1 },
                                        )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan Dapur (opsional)") },
                        singleLine = true,
                        shape = KalaOrderShapes.Medium,
                        colors = kalaFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            if (channel == OrderChannel.WHATSAPP) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KalaSecondaryButton(
                        text = stringResource(R.string.action_tutup),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    KalaPrimaryButton(
                        text = "Kirim ke Bot WA",
                        enabled = waMessage.isNotBlank(),
                        icon = Icons.AutoMirrored.Outlined.Send,
                        onClick = {
                            onSimulateNotification(
                                customerName,
                                customerPhone,
                                waMessage,
                                simulateGroupChat,
                            ) { decision ->
                                lastBotDecision = decision
                            }
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("submit_new_order_button"),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "${manualItems.sumOf { it.quantity }} item dipilih",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                        Text(
                            text = RupiahFormatter.format(manualTotalAmount),
                            style = KalaOrderMonoStyles.MetricLarge,
                            color = PrimaryEmerald,
                        )
                    }
                    KalaPrimaryButton(
                        text = "Masukkan Antrean",
                        enabled = manualItems.isNotEmpty(),
                        onClick = {
                            onSubmitManualOrder(customerName, customerPhone, OrderChannel.KASIR, manualItems, note)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("submit_new_order_button"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuEditorBottomSheet(
    editingItem: MenuItemRecord?,
    onDismiss: () -> Unit,
    onSave: (MenuItemRecord, Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEdit = editingItem != null

    var code by remember { mutableStateOf(editingItem?.code ?: "") }
    var name by remember { mutableStateOf(editingItem?.name ?: "") }
    var category by remember { mutableStateOf(editingItem?.category ?: MenuCategory.MAKANAN) }
    var priceText by remember { mutableStateOf(editingItem?.price?.toString() ?: "") }
    var stockText by remember { mutableStateOf(editingItem?.stock?.toString() ?: "10") }
    var isAvailable by remember { mutableStateOf(editingItem?.isAvailable ?: true) }

    val parsedPrice = priceText.filter { it.isDigit() }.toIntOrNull() ?: 0
    val parsedStock = stockText.filter { it.isDigit() }.toIntOrNull() ?: 0
    val canSave = code.trim().isNotEmpty() && name.trim().isNotEmpty() && parsedPrice > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = KalaOrderShapes.Sheet,
        containerColor = SurfaceWhite,
        dragHandle = { KalaBottomSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isEdit) "Sunting Menu" else "Tambah Menu Baru",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (!isEdit) code = it.uppercase().take(4) },
                    enabled = !isEdit,
                    label = { Text("Kode (mis. A5)") },
                    singleLine = true,
                    shape = KalaOrderShapes.Medium,
                    colors = kalaFieldColors(),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("menu_code_input"),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Menu") },
                    singleLine = true,
                    shape = KalaOrderShapes.Medium,
                    colors = kalaFieldColors(),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("menu_name_input"),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuCategory.entries.forEach { cat ->
                        KalaFilterChip(
                            label = cat.label,
                            selected = category == cat,
                            onClick = { category = cat },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Harga (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = KalaOrderShapes.Medium,
                    colors = kalaFieldColors(),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("menu_price_input"),
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Stok") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = KalaOrderShapes.Medium,
                    colors = kalaFieldColors(),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("menu_stock_input"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Tersedia untuk Dipesan",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Tampilkan menu di katalog Bot WhatsApp",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = isAvailable,
                    onCheckedChange = { isAvailable = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceWhite,
                        checkedTrackColor = PrimaryEmerald,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KalaSecondaryButton(
                    text = stringResource(R.string.action_batal),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                KalaPrimaryButton(
                    text = "Simpan Menu",
                    enabled = canSave,
                    onClick = {
                        onSave(
                            MenuItemRecord(
                                code = code.trim().uppercase(),
                                name = name.trim(),
                                category = category,
                                price = parsedPrice,
                                stock = parsedStock,
                                isAvailable = isAvailable && parsedStock > 0,
                            ),
                            isEdit,
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("save_menu_button"),
                )
            }
        }
    }
}

@Composable
fun DeleteMenuConfirmDialog(
    item: MenuItemRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = KalaOrderShapes.Card,
        title = {
            Text(
                text = "Hapus Menu ${item.code}?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        },
        text = {
            Text(
                text = "\"${item.name}\" akan dihapus dari katalog kasir dan tidak lagi dikenali oleh Bot WhatsApp.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_batal),
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_delete_menu_button"),
            ) {
                Text(
                    text = stringResource(R.string.action_hapus),
                    color = StatusDibatalkanText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}

@Composable
private fun MiniStepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
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

@Composable
private fun kalaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryEmerald,
    unfocusedBorderColor = BorderStrong,
    focusedLabelColor = PrimaryEmerald,
    cursorColor = PrimaryEmerald,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
)
