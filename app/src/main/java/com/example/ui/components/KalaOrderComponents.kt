package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OrderStatus
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

/**
 * Level 1 flat card container: #FFFFFF fill, 1px #E2E8F0 border, 14dp radius.
 */
@Composable
fun KalaOrderCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceWhite,
    borderColor: Color = BorderSubtle,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clip(KalaOrderShapes.Card).clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = KalaOrderShapes.Card,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Tripartite semantic status pill badge (bg + text + border).
 */
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, border) = when (status) {
        OrderStatus.MENUNGGU -> Triple(StatusMenungguBg, StatusMenungguText, StatusMenungguBorder)
        OrderStatus.DIPROSES -> Triple(StatusDiprosesBg, StatusDiprosesText, StatusDiprosesBorder)
        OrderStatus.SELESAI -> Triple(StatusSelesaiBg, StatusSelesaiText, StatusSelesaiBorder)
        OrderStatus.DIBATALKAN -> Triple(StatusDibatalkanBg, StatusDibatalkanText, StatusDibatalkanBorder)
    }

    Surface(
        modifier = modifier,
        shape = KalaOrderShapes.Pill,
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            text = status.label,
            style = KalaOrderMonoStyles.BadgeLabel,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Generic tripartite pill badge for stock or channel status.
 */
@Composable
fun SemanticPillBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = KalaOrderShapes.Pill,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = text,
            style = KalaOrderMonoStyles.BadgeLabel,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/**
 * Monospace code tag for KO-0142 order tokens or A1/B3 inventory codes.
 */
@Composable
fun MonospaceCodeTag(
    code: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val bg = if (emphasized) PrimarySoft else FillSubtle
    val fg = if (emphasized) PrimaryEmerald else TextPrimary
    val border = if (emphasized) StatusSelesaiBorder else BorderSubtle

    Surface(
        modifier = modifier,
        shape = KalaOrderShapes.Small,
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            text = code,
            style = if (emphasized) KalaOrderMonoStyles.CodeLarge else KalaOrderMonoStyles.CodeMedium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * Animated operational mode toggle pill (TOKO BUKA / TOKO TUTUP).
 */
@Composable
fun StoreStatusPill(
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isOpen) StatusSelesaiBg else StatusDibatalkanBg
    val fg = if (isOpen) StatusSelesaiText else StatusDibatalkanText
    val border = if (isOpen) StatusSelesaiBorder else StatusDibatalkanBorder
    val label = if (isOpen) {
        stringResource(R.string.status_toko_buka)
    } else {
        stringResource(R.string.status_toko_tutup)
    }

    val pulseAlpha = rememberPulsingAlpha(enabled = isOpen)

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("store_status_toggle_pill")
            .clip(KalaOrderShapes.Pill)
            .clickable(onClick = onToggle),
        shape = KalaOrderShapes.Pill,
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulseAlpha)
                    .background(fg, CircleShape)
            )
            Text(
                text = label,
                style = KalaOrderMonoStyles.BadgeLabel.copy(fontWeight = FontWeight.Bold),
                color = fg,
                maxLines = 1,
            )
        }
    }
}

/**
 * Dedicated Bot Banner Card (#ECFDF5 fill, #A7F3D0 border, 14dp radius).
 * Surfaces Android Notification Access permission status when not yet granted.
 */
@Composable
fun BotStatusBannerCard(
    botActive: Boolean,
    storeOpen: Boolean,
    isNotificationAccessGranted: Boolean,
    waitingCount: Int,
    onToggleBot: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFullyOperational = isNotificationAccessGranted && botActive && storeOpen
    val bg = if (isFullyOperational) PrimarySoft else StatusMenungguBg
    val border = if (isFullyOperational) StatusSelesaiBorder else StatusMenungguBorder
    val accent = if (isFullyOperational) PrimaryEmerald else StatusMenungguText
    val pulseAlpha = rememberPulsingAlpha(enabled = isFullyOperational)

    KalaOrderCard(
        modifier = modifier.testTag("bot_banner_card"),
        containerColor = bg,
        borderColor = border,
        contentPadding = PaddingValues(14.dp),
        onClick = onBannerClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceWhite, KalaOrderShapes.Medium)
                    .border(1.dp, border, KalaOrderShapes.Medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "Status Bot WhatsApp",
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(8.dp)
                        .alpha(pulseAlpha)
                        .background(accent, CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        !isNotificationAccessGranted -> "Izin Notifikasi WA Belum Aktif"
                        botActive -> "Bot Notifikasi WA Aktif"
                        else -> stringResource(R.string.bot_paused_title)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        !isNotificationAccessGranted -> "Aktifkan Akses Notifikasi agar bot otomatis membalas WA"
                        !storeOpen -> "Toko tutup • Bot otomatis balas pesan toko tutup"
                        botActive && waitingCount > 0 -> "$waitingCount pesanan menunggu • Baca & balas notif WA aktif"
                        botActive -> "Mendengarkan notif WhatsApp & WA Business"
                        else -> stringResource(R.string.bot_paused_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("bot_banner_toggle_button")
                    .clip(KalaOrderShapes.Medium)
                    .clickable(
                        onClick = if (!isNotificationAccessGranted) onOpenPermissionSettings else onToggleBot
                    ),
                shape = KalaOrderShapes.Medium,
                color = SurfaceWhite,
                border = BorderStroke(1.dp, border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isNotificationAccessGranted) {
                        Icon(
                            imageVector = if (botActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (botActive) "Jeda bot" else "Aktifkan bot",
                            tint = accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = when {
                            !isNotificationAccessGranted -> "Izinkan"
                            botActive -> stringResource(R.string.btn_jeda)
                            else -> stringResource(R.string.btn_aktifkan)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
            }
        }
    }
}

/**
 * Filter chip pill (36dp visual inside 48dp accessible touch target).
 */
@Composable
fun KalaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val bg = if (selected) PrimaryEmerald else SurfaceWhite
    val fg = if (selected) SurfaceWhite else TextSecondary
    val border = if (selected) PrimaryEmerald else BorderSubtle

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .height(36.dp)
                .clip(KalaOrderShapes.Pill)
                .clickable(onClick = onClick),
            shape = KalaOrderShapes.Pill,
            color = bg,
            border = BorderStroke(1.dp, border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    maxLines = 1,
                )
                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = KalaOrderMonoStyles.CodeSmall,
                        color = if (selected) PrimarySoft else TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Search field with 44dp+ height, 10dp radius, #CBD5E1 border, emerald focus ring.
 */
@Composable
fun KalaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) PrimaryEmerald else BorderStrong
    val borderWidth = if (isFocused) 1.5.dp else 1.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp),
        shape = KalaOrderShapes.Medium,
        color = SurfaceWhite,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Cari",
                tint = if (isFocused) PrimaryEmerald else TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(PrimaryEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Hapus pencarian",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun KalaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = KalaOrderShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryEmerald,
            contentColor = SurfaceWhite,
            disabledContainerColor = BorderSubtle,
            disabledContentColor = TextMuted,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
fun KalaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = KalaOrderShapes.Medium,
        border = BorderStroke(1.dp, BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
fun KalaDestructiveOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = KalaOrderShapes.Medium,
        border = BorderStroke(1.dp, StatusDibatalkanBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = StatusDibatalkanText,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

/**
 * Micro action button for card-level Sunting / Hapus controls (6dp radius, 32dp visual height).
 */
@Composable
fun KalaMicroActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val borderColor = if (destructive) StatusDibatalkanBorder else BorderStrong
    val textColor = if (destructive) StatusDibatalkanText else TextPrimary
    val bgColor = if (destructive) StatusDibatalkanBg.copy(alpha = 0.35f) else SurfaceWhite

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(KalaOrderShapes.Small)
                .clickable(onClick = onClick),
            shape = KalaOrderShapes.Small,
            color = bgColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun KalaBottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(BorderStrong, KalaOrderShapes.Pill)
        )
    }
}

@Composable
fun KalaEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    KalaOrderCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(FillSubtle, KalaOrderShapes.Card),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun rememberPulsingAlpha(enabled: Boolean): Float {
    if (!enabled) return 1f
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )
    return alpha
}
