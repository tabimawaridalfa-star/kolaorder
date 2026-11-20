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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bot.WhatsAppAutoReplyEngine
import com.example.data.BotActivityLogRecord
import com.example.data.BotLogOutcome
import com.example.data.BotSettings
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaOrderCard
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.components.SemanticPillBadge
import com.example.ui.components.StoreStatusPill
import com.example.ui.components.rememberPulsingAlpha
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

@Composable
fun BotSettingsScreen(
    uiState: KalaOrderUiState,
    onToggleStoreOpen: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onUpdateSettings: (BotSettings, String?) -> Unit,
    onClearBotLogs: () -> Unit,
    onResetCooldowns: () -> Unit,
    onOpenWhatsAppSimulator: () -> Unit,
    onOpenManualOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = uiState.botSettings
    val hasAccess = uiState.isNotificationAccessGranted

    var storeNameDraft by remember { mutableStateOf(settings.storeName) }
    var welcomeDraft by remember { mutableStateOf(settings.welcomeMessageTemplate) }
    var orderConfirmDraft by remember { mutableStateOf(settings.greetingTemplate) }
    var closedDraft by remember { mutableStateOf(settings.closedStoreTemplate) }

    LaunchedEffect(
        settings.storeName,
        settings.welcomeMessageTemplate,
        settings.greetingTemplate,
        settings.closedStoreTemplate,
    ) {
        storeNameDraft = settings.storeName
        welcomeDraft = settings.welcomeMessageTemplate
        orderConfirmDraft = settings.greetingTemplate
        closedDraft = settings.closedStoreTemplate
    }

    val isFullyLive = hasAccess && settings.botActive && settings.storeOpen
    val pulseAlpha = rememberPulsingAlpha(enabled = isFullyLive)

    val livePreviewSettings = settings.copy(
        storeName = storeNameDraft.ifBlank { settings.storeName },
        welcomeMessageTemplate = welcomeDraft.ifBlank { settings.welcomeMessageTemplate },
    )
    val liveReplyPreview = remember(livePreviewSettings, uiState.menuItems) {
        WhatsAppAutoReplyEngine.buildWelcomeAndMenuReply(livePreviewSettings, uiState.menuItems)
    }

    val cooldownOptions = listOf(
        15 to "15 mnt",
        30 to "30 mnt",
        60 to "60 mnt",
        180 to "3 jam",
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item(key = "bot_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bot Notifikasi WA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Baca & balas notifikasi WA otomatis tanpa langganan",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                StoreStatusPill(
                    isOpen = settings.storeOpen,
                    onToggle = onToggleStoreOpen,
                )
            }
        }

        // Hero Card: Android Notification Access Permission + Live Connection Status
        item(key = "bot_permission_hero") {
            val heroBg = if (isFullyLive) PrimarySoft else StatusMenungguBg
            val heroBorder = if (isFullyLive) StatusSelesaiBorder else StatusMenungguBorder
            val heroAccent = if (isFullyLive) PrimaryEmerald else StatusMenungguText

            KalaOrderCard(
                containerColor = heroBg,
                borderColor = heroBorder,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = KalaOrderShapes.Medium,
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, heroBorder),
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (hasAccess) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsOff
                                },
                                contentDescription = "Status Akses Notifikasi",
                                tint = heroAccent,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(pulseAlpha)
                                    .background(heroAccent, KalaOrderShapes.Pill)
                            )
                            Text(
                                text = when {
                                    !hasAccess -> "Akses Notifikasi Belum Aktif"
                                    !settings.botActive -> "Izin Aktif • Bot Dijeda"
                                    !settings.storeOpen -> "Izin Aktif • Mode Toko Tutup"
                                    else -> "Terhubung • Mendengarkan Notif WA"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasAccess) {
                                "Pastikan notifikasi WA tidak dibisukan agar Direct Reply berjalan otomatis."
                            } else {
                                "Aktifkan izin Akses Notifikasi Android untuk KalaOrder agar bot bisa membalas chat WA secara gratis."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasAccess) {
                    KalaPrimaryButton(
                        text = stringResource(R.string.btn_atur_izin),
                        onClick = onOpenNotificationSettings,
                        icon = Icons.Outlined.NotificationsActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_notification_access_button"),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KalaPrimaryButton(
                        text = "Tes Simulasi WA",
                        onClick = onOpenWhatsAppSimulator,
                        icon = Icons.Outlined.ChatBubbleOutline,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bot_screen_simulator_button"),
                    )
                    KalaSecondaryButton(
                        text = if (hasAccess) stringResource(R.string.btn_pengaturan_izin) else "Order Kasir",
                        onClick = if (hasAccess) onOpenNotificationSettings else onOpenManualOrder,
                        icon = if (hasAccess) Icons.Outlined.Security else Icons.Outlined.PointOfSale,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bot_screen_secondary_action"),
                    )
                }
            }
        }

        // Target WhatsApp Apps & Anti-Spam Filters
        item(key = "bot_targets_and_filters") {
            KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
                Text(
                    text = "Aplikasi Target & Filter Anti-Spam",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(10.dp))

                BotToggleRow(
                    title = "Aktifkan Bot Auto-Balas",
                    subtitle = "Balas salam + daftar kode menu dan proses order otomatis",
                    checked = settings.botActive,
                    onCheckedChange = {
                        onUpdateSettings(
                            settings.copy(botActive = it),
                            if (it) "Bot Notifikasi WA diaktifkan" else "Bot Notifikasi WA dijeda",
                        )
                    },
                    testTag = "switch_bot_active",
                )
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

                BotToggleRow(
                    title = "WhatsApp Biasa",
                    subtitle = "Baca & balas notifikasi dari com.whatsapp",
                    checked = settings.listenWhatsApp,
                    onCheckedChange = {
                        onUpdateSettings(settings.copy(listenWhatsApp = it), null)
                    },
                    testTag = "switch_listen_wa",
                )
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

                BotToggleRow(
                    title = "WhatsApp Business",
                    subtitle = "Baca & balas notifikasi dari com.whatsapp.w4b",
                    checked = settings.listenWhatsAppBusiness,
                    onCheckedChange = {
                        onUpdateSettings(settings.copy(listenWhatsAppBusiness = it), null)
                    },
                    testTag = "switch_listen_wa_business",
                )
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

                BotToggleRow(
                    title = "Abaikan Chat Grup",
                    subtitle = "Bot hanya membalas chat pribadi pelanggan, tidak membalas grup",
                    checked = settings.ignoreGroupChats,
                    onCheckedChange = {
                        onUpdateSettings(settings.copy(ignoreGroupChats = it), null)
                    },
                    testTag = "switch_ignore_groups",
                )
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jeda Salam per Kontak (Cooldown)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Pesan kode produk & kata MENU tetap langsung dibalas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    TextButton(
                        onClick = onResetCooldowns,
                        modifier = Modifier.testTag("reset_cooldown_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reset cooldown",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryEmerald,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cooldownOptions) { (minutes, label) ->
                        KalaFilterChip(
                            label = label,
                            selected = settings.welcomeCooldownMinutes == minutes,
                            onClick = {
                                onUpdateSettings(
                                    settings.copy(welcomeCooldownMinutes = minutes),
                                    "Jeda salam diatur ke $label",
                                )
                            },
                            modifier = Modifier.testTag("cooldown_chip_$minutes"),
                        )
                    }
                }
            }
        }

        // Live Dynamic WhatsApp Welcome + Menu Reply Preview
        item(key = "bot_live_preview") {
            KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
                Text(
                    text = "Pratinjau Live Balasan Selamat Datang & Menu",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Daftar kode produk & harga otomatis tersinkron dari tab Menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = KalaOrderShapes.Card,
                    color = PrimarySoft,
                    border = BorderStroke(1.dp, StatusSelesaiBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = liveReplyPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        // Editable Templates & Store Identity
        item(key = "bot_templates") {
            KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Template Balasan Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storeNameDraft,
                    onValueChange = { storeNameDraft = it },
                    label = { Text("Nama Warung ({TOKO})") },
                    singleLine = true,
                    shape = KalaOrderShapes.Medium,
                    colors = botFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_name_input"),
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = welcomeDraft,
                    onValueChange = { welcomeDraft = it },
                    label = { Text("Pesan Selamat Datang (sebelum daftar menu)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = KalaOrderShapes.Medium,
                    colors = botFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_template_input"),
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = orderConfirmDraft,
                    onValueChange = { orderConfirmDraft = it },
                    label = { Text("Konfirmasi Order Masuk (#{KODE})") },
                    minLines = 2,
                    maxLines = 3,
                    shape = KalaOrderShapes.Medium,
                    colors = botFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("greeting_template_input"),
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = closedDraft,
                    onValueChange = { closedDraft = it },
                    label = { Text("Pesan Saat Status TOKO TUTUP") },
                    minLines = 2,
                    maxLines = 3,
                    shape = KalaOrderShapes.Medium,
                    colors = botFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("closed_template_input"),
                )
                Spacer(modifier = Modifier.height(12.dp))

                KalaPrimaryButton(
                    text = "Simpan Pengaturan Bot",
                    onClick = {
                        onUpdateSettings(
                            settings.copy(
                                storeName = storeNameDraft.trim().ifBlank { settings.storeName },
                                welcomeMessageTemplate = welcomeDraft.trim().ifBlank { settings.welcomeMessageTemplate },
                                greetingTemplate = orderConfirmDraft.trim().ifBlank { settings.greetingTemplate },
                                closedStoreTemplate = closedDraft.trim().ifBlank { settings.closedStoreTemplate },
                            ),
                            "Pengaturan & template bot berhasil disimpan",
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_bot_settings_button"),
                )
            }
        }

        // Real-Time Bot Activity Log
        item(key = "bot_activity_logs") {
            KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Aktivitas Bot",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${uiState.botActivityLogs.size} aktivitas notifikasi & simulasi terakhir",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    if (uiState.botActivityLogs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearBotLogs,
                            modifier = Modifier.testTag("clear_bot_logs_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Bersihkan log",
                                tint = StatusDibatalkanText,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.btn_bersihkan_log),
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusDibatalkanText,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.botActivityLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FillSubtle, KalaOrderShapes.Medium)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "Belum ada notifikasi yang diproses. Aktifkan Akses Notifikasi atau tekan \"Tes Simulasi WA\" untuk mencoba balasan otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.botActivityLogs.take(15).forEach { log ->
                            BotActivityLogRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotActivityLogRow(log: BotActivityLogRecord) {
    val (bg, fg, border) = when (log.outcome) {
        BotLogOutcome.CREATED_ORDER -> Triple(StatusSelesaiBg, StatusSelesaiText, StatusSelesaiBorder)
        BotLogOutcome.SENT_WELCOME -> Triple(StatusDiprosesBg, StatusDiprosesText, StatusDiprosesBorder)
        BotLogOutcome.OUT_OF_STOCK, BotLogOutcome.SENT_CLOSED, BotLogOutcome.IGNORED_COOLDOWN ->
            Triple(StatusMenungguBg, StatusMenungguText, StatusMenungguBorder)
        BotLogOutcome.IGNORED_GROUP -> Triple(FillSubtle, TextSecondary, BorderSubtle)
        BotLogOutcome.FAILED_REPLY -> Triple(StatusDibatalkanBg, StatusDibatalkanText, StatusDibatalkanBorder)
    }

    Surface(
        shape = KalaOrderShapes.Medium,
        color = FillSubtle.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
                        text = log.timestampLabel,
                        style = KalaOrderMonoStyles.CodeSmall,
                        color = TextMuted,
                    )
                    Text(
                        text = log.senderName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "• ${log.sourceApp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                    )
                }
                SemanticPillBadge(
                    text = log.outcome.label,
                    backgroundColor = bg,
                    textColor = fg,
                    borderColor = border,
                )
            }

            Text(
                text = "Masuk: \"${log.incomingMessage}\"",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = log.replyPreview.lineSequence().firstOrNull().orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BotToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = SurfaceWhite,
                checkedTrackColor = PrimaryEmerald,
                uncheckedThumbColor = SurfaceWhite,
                uncheckedTrackColor = BorderStrong,
                uncheckedBorderColor = BorderStrong,
            ),
        )
    }
}

@Composable
private fun botFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryEmerald,
    unfocusedBorderColor = BorderStrong,
    focusedLabelColor = PrimaryEmerald,
    cursorColor = PrimaryEmerald,
)
