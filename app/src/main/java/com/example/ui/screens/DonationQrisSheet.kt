package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CreatorInfo
import com.example.ui.components.KalaBottomSheetDragHandle
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimarySoft
import com.example.ui.theme.StatusMenungguBg
import com.example.ui.theme.StatusMenungguBorder
import com.example.ui.theme.StatusMenungguText
import com.example.ui.theme.StatusSelesaiBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationQrisBottomSheet(
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShareQris: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Warm Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = KalaOrderShapes.Medium,
                    color = StatusMenungguBg,
                    border = BorderStroke(1.dp, StatusMenungguBorder),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Coffee,
                            contentDescription = stringResource(R.string.donation_title),
                            tint = StatusMenungguText,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.donation_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Apresiasi sukarela untuk ${CreatorInfo.CREATOR_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            // Friendly non-pushy message
            Surface(
                shape = KalaOrderShapes.Medium,
                color = PrimarySoft,
                border = BorderStroke(1.dp, StatusSelesaiBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.donation_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(12.dp),
                )
            }

            // Crisp, full-contrast QRIS Frame
            Surface(
                shape = KalaOrderShapes.Card,
                color = SurfaceWhite,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donation_qris_image_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qris_donasi_kalaorder),
                        contentDescription = "Kode QRIS ${CreatorInfo.QRIS_MERCHANT_NAME}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(KalaOrderShapes.Medium),
                    )
                    Surface(
                        shape = KalaOrderShapes.Pill,
                        color = FillSubtle,
                        border = BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Text(
                            text = "${CreatorInfo.QRIS_MERCHANT_NAME} • NMID ${CreatorInfo.QRIS_NMID}",
                            style = KalaOrderMonoStyles.CodeSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // 3-step scan from gallery guide
            Surface(
                shape = KalaOrderShapes.Medium,
                color = FillSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Cara mudah dari HP ini:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "1. Ketuk Simpan ke Galeri  •  2. Buka DANA, GoPay, OVO, ShopeePay, atau m-Banking  •  3. Pilih Scan QR lalu ambil gambar dari Galeri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Primary Save + Share CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KalaPrimaryButton(
                    text = stringResource(R.string.donation_save_gallery),
                    onClick = onSaveToGallery,
                    icon = Icons.Outlined.Download,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("donation_save_gallery_button"),
                )
                KalaSecondaryButton(
                    text = stringResource(R.string.donation_share_qris),
                    onClick = onShareQris,
                    icon = Icons.Outlined.Share,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("donation_share_qris_button"),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Terima kasih banyak atas dukungannya! Semoga usaha semakin lancar & laris manis.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
