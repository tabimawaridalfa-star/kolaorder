package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CashflowCategories
import com.example.data.CashflowEntryRecord
import com.example.data.CashflowType
import com.example.data.CreatorInfo
import com.example.data.FinanceSummaryState
import com.example.data.RupiahFormatter
import com.example.ui.components.KalaBottomSheetDragHandle
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FillSubtle
import com.example.ui.theme.KalaOrderMonoStyles
import com.example.ui.theme.KalaOrderShapes
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimarySoft
import com.example.ui.theme.StatusDibatalkanText
import com.example.ui.theme.StatusSelesaiBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowEditorBottomSheet(
    editingEntry: CashflowEntryRecord?,
    onDismiss: () -> Unit,
    onSave: (CashflowEntryRecord, Boolean) -> Unit,
) {
    val isEdit = editingEntry != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember(editingEntry) {
        mutableStateOf(editingEntry?.type ?: CashflowType.EXPENSE)
    }
    var selectedCategory by remember(editingEntry) {
        mutableStateOf(
            editingEntry?.category
                ?: CashflowCategories.forType(CashflowType.EXPENSE).first()
        )
    }
    var title by remember(editingEntry) { mutableStateOf(editingEntry?.title ?: "") }
    var amountText by remember(editingEntry) {
        mutableStateOf(editingEntry?.amount?.toString() ?: "")
    }
    var note by remember(editingEntry) { mutableStateOf(editingEntry?.note ?: "") }
    // Day offset from today for quick date entry (0 = Hari Ini, 1 = Kemarin, etc.)
    var daysAgo by remember(editingEntry) { mutableIntStateOf(0) }

    val availableCategories = CashflowCategories.forType(selectedType)
    val parsedAmount = amountText.filter { it.isDigit() }.toIntOrNull() ?: 0
    val canSave = title.trim().isNotEmpty() && parsedAmount > 0

    val dateOffsets = listOf(
        0 to "Hari Ini",
        1 to "Kemarin",
        2 to "2 Hari Lalu",
        7 to "1 Minggu Lalu",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isEdit) "Sunting Catatan Kas" else "Catat Pengeluaran / Modal",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )

            // Type selector chips
            Text(
                text = "Jenis Transaksi",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CashflowType.entries) { type ->
                    KalaFilterChip(
                        label = type.label,
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            val cats = CashflowCategories.forType(type)
                            if (selectedCategory !in cats) {
                                selectedCategory = cats.first()
                            }
                        },
                        modifier = Modifier.testTag("cashflow_type_${type.name.lowercase()}"),
                    )
                }
            }

            // Quick category selector chips
            Text(
                text = "Kategori",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableCategories) { category ->
                    KalaFilterChip(
                        label = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        modifier = Modifier.testTag("cashflow_cat_${category.lowercase().replace(" ", "_")}"),
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Keterangan / Nama Transaksi") },
                placeholder = {
                    Text(
                        when (selectedType) {
                            CashflowType.EXPENSE -> "Contoh: Belanja ayam & beras pasar pagi"
                            CashflowType.CAPITAL -> "Contoh: Tambah modal kas warung"
                            CashflowType.OTHER_INCOME -> "Contoh: DP nasi kotak acara RT"
                        }
                    )
                },
                singleLine = true,
                shape = KalaOrderShapes.Medium,
                colors = financeFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashflow_title_input"),
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() }.take(9) },
                label = { Text("Nominal (Rp)") },
                placeholder = { Text("Contoh: 75000") },
                prefix = {
                    Text(
                        text = "Rp ",
                        style = KalaOrderMonoStyles.CodeMedium,
                        color = TextSecondary,
                    )
                },
                supportingText = {
                    if (parsedAmount > 0) {
                        Text(
                            text = "Terbaca: ${RupiahFormatter.format(parsedAmount)}",
                            style = KalaOrderMonoStyles.CodeSmall,
                            color = PrimaryEmerald,
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = KalaOrderShapes.Medium,
                colors = financeFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashflow_amount_input"),
            )

            if (!isEdit) {
                Text(
                    text = "Tanggal Transaksi",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dateOffsets) { (offset, label) ->
                        KalaFilterChip(
                            label = label,
                            selected = daysAgo == offset,
                            onClick = { daysAgo = offset },
                            modifier = Modifier.testTag("cashflow_date_offset_$offset"),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                placeholder = { Text("Misal: struk dari supplier atau keterangan khusus") },
                minLines = 2,
                maxLines = 3,
                shape = KalaOrderShapes.Medium,
                colors = financeFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashflow_note_input"),
            )

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
                    text = stringResource(R.string.action_simpan),
                    enabled = canSave,
                    onClick = {
                        val timestamp = editingEntry?.createdAtMillis
                            ?: (System.currentTimeMillis() - daysAgo * 86_400_000L)
                        val dateLabel = editingEntry?.dateLabel
                            ?: SimpleDateFormat("dd MMM yyyy", Locale("in", "ID")).format(Date(timestamp))
                        onSave(
                            CashflowEntryRecord(
                                id = editingEntry?.id ?: 0L,
                                type = selectedType,
                                category = selectedCategory,
                                title = title.trim(),
                                amount = parsedAmount,
                                note = note.trim(),
                                dateLabel = dateLabel,
                                createdAtMillis = timestamp,
                            ),
                            isEdit,
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("cashflow_save_button"),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceRecapBottomSheet(
    summary: FinanceSummaryState,
    storeName: String,
    formattedRecapText: String,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareOther: () -> Unit,
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
        ) {
            Column {
                Text(
                    text = "Pratinjau Rekap Keuangan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Text(
                    text = "$storeName • ${summary.periodLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            // Summary highlight box
            Surface(
                shape = KalaOrderShapes.Card,
                color = PrimarySoft,
                border = BorderStroke(1.dp, StatusSelesaiBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RecapSummaryRow(
                        label = "Pemasukan Order (${summary.completedOrderCount}x)",
                        value = RupiahFormatter.format(summary.totalOrderIncome),
                        valueColor = PrimaryEmerald,
                    )
                    if (summary.totalOtherIncome > 0) {
                        RecapSummaryRow(
                            label = "Pemasukan Lain (${summary.otherIncomeCount}x)",
                            value = RupiahFormatter.format(summary.totalOtherIncome),
                            valueColor = PrimaryEmerald,
                        )
                    }
                    RecapSummaryRow(
                        label = "Total Pengeluaran (${summary.expenseCount}x)",
                        value = "-${RupiahFormatter.format(summary.totalExpense)}",
                        valueColor = StatusDibatalkanText,
                    )
                    HorizontalDivider(color = StatusSelesaiBorder, modifier = Modifier.padding(vertical = 4.dp))
                    RecapSummaryRow(
                        label = "Laba Bersih (Margin ${summary.profitMarginPercent}%)",
                        value = RupiahFormatter.formatSigned(summary.netProfit),
                        valueColor = if (summary.netProfit >= 0) PrimaryEmerald else StatusDibatalkanText,
                        bold = true,
                    )
                    RecapSummaryRow(
                        label = "Modal Usaha Tercatat",
                        value = RupiahFormatter.format(summary.totalCapital),
                        valueColor = TextPrimary,
                    )
                    RecapSummaryRow(
                        label = "Saldo Kas Total",
                        value = RupiahFormatter.format(summary.netCashBalance),
                        valueColor = TextPrimary,
                        bold = true,
                    )
                }
            }

            // Formatted text preview (ready for WhatsApp)
            Text(
                text = "Format Pesan WhatsApp",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            Surface(
                shape = KalaOrderShapes.Medium,
                color = FillSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = formattedRecapText,
                    style = KalaOrderMonoStyles.CodeSmall.copy(lineHeight = MaterialTheme.typography.bodySmall.lineHeight),
                    color = TextPrimary,
                    modifier = Modifier.padding(12.dp),
                )
            }

            KalaPrimaryButton(
                text = stringResource(R.string.finance_action_export_csv),
                onClick = onExportCsv,
                icon = Icons.Outlined.Download,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recap_sheet_export_csv"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KalaSecondaryButton(
                    text = stringResource(R.string.finance_action_share_wa),
                    onClick = onShareWhatsApp,
                    icon = Icons.Outlined.Share,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("recap_sheet_share_wa"),
                )
                KalaSecondaryButton(
                    text = "Bagikan Lain",
                    onClick = onShareOther,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("recap_sheet_share_other"),
                )
            }

            Text(
                text = CreatorInfo.ATTRIBUTION_FOOTER,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RecapSummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = TextPrimary,
        )
        Text(
            text = value,
            style = if (bold) {
                KalaOrderMonoStyles.CodeLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                KalaOrderMonoStyles.CodeMedium
            },
            color = valueColor,
        )
    }
}

@Composable
fun DeleteCashflowConfirmDialog(
    entry: CashflowEntryRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = KalaOrderShapes.Card,
        title = {
            Text(
                text = "Hapus Catatan Kas?",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        },
        text = {
            Text(
                text = "Catatan ${entry.type.label} \"${entry.title}\" (${RupiahFormatter.format(entry.amount)}) akan dihapus permanen dari buku kas.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_delete_cashflow_button"),
            ) {
                Text(
                    text = stringResource(R.string.action_hapus),
                    style = MaterialTheme.typography.labelLarge,
                    color = StatusDibatalkanText,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_batal),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
            }
        },
    )
}

@Composable
private fun financeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryEmerald,
    unfocusedBorderColor = BorderStrong,
    focusedLabelColor = PrimaryEmerald,
    cursorColor = PrimaryEmerald,
)
