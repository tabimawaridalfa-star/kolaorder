package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.BotSettings
import com.example.data.CreatorInfo
import com.example.data.DataResetSelection
import com.example.data.MenuCategory
import com.example.data.MenuItemRecord
import com.example.data.MenuStarterCatalog
import com.example.data.RupiahFormatter
import com.example.ui.components.KalaBottomSheetDragHandle
import com.example.ui.components.KalaFilterChip
import com.example.ui.components.KalaOrderCard
import com.example.ui.components.KalaPrimaryButton
import com.example.ui.components.KalaSecondaryButton
import com.example.ui.components.MonospaceCodeTag
import com.example.ui.components.SemanticPillBadge
import com.example.ui.theme.BgCanvas
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
import com.example.ui.theme.StatusSelesaiBg
import com.example.ui.theme.StatusSelesaiBorder
import com.example.ui.theme.StatusSelesaiText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class BulkMenuMode(val label: String) {
    BULK_TEXT("Isi Banyak Sekaligus"),
    TEMPLATE("Template Usaha"),
}

private enum class OnboardingMenuChoice(val label: String) {
    BULK_TEXT("Isi Banyak Menu"),
    TEMPLATE("Template Usaha"),
    EMPTY("Mulai Kosong Dulu"),
}

@Composable
fun InitialSetupDialog(
    currentSettings: BotSettings,
    existingMenuCodes: Set<String>,
    onSkip: () -> Unit,
    onComplete: (storeName: String, whatsappNumber: String, initialCapital: Int, initialMenus: List<MenuItemRecord>) -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var storeName by remember {
        mutableStateOf(
            if (currentSettings.storeName == "Usaha Saya") "" else currentSettings.storeName
        )
    }
    var whatsappNumber by remember { mutableStateOf(currentSettings.whatsappNumber) }
    var initialCapitalText by remember { mutableStateOf("") }

    // Step 2 menu state
    var menuChoice by remember { mutableStateOf(OnboardingMenuChoice.BULK_TEXT) }
    var defaultCategory by remember { mutableStateOf(MenuCategory.MAKANAN) }
    var defaultStockText by remember { mutableStateOf("20") }
    var bulkRawText by remember { mutableStateOf("") }

    var selectedPackId by remember { mutableStateOf(MenuStarterCatalog.starterPacks.first().id) }
    val selectedPack = MenuStarterCatalog.starterPacks.firstOrNull { it.id == selectedPackId }
        ?: MenuStarterCatalog.starterPacks.first()
    val checkedTemplateKeys = remember {
        mutableStateMapOf<String, Boolean>().apply {
            MenuStarterCatalog.starterPacks.forEach { pack ->
                pack.items.forEach { item ->
                    put("${pack.id}_${item.code}", true)
                }
            }
        }
    }

    val defaultStock = defaultStockText.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 999) ?: 20
    val parsedBulkItems = remember(bulkRawText, defaultCategory, defaultStock, existingMenuCodes) {
        MenuStarterCatalog.parseBulkMenuLines(
            rawText = bulkRawText,
            defaultCategory = defaultCategory,
            defaultStock = defaultStock,
            existingCodes = existingMenuCodes,
        )
    }

    val chosenTemplateItems = selectedPack.items.filter { item ->
        checkedTemplateKeys["${selectedPack.id}_${item.code}"] != false
    }

    val finalInitialMenus = when (menuChoice) {
        OnboardingMenuChoice.BULK_TEXT -> parsedBulkItems
        OnboardingMenuChoice.TEMPLATE -> chosenTemplateItems
        OnboardingMenuChoice.EMPTY -> emptyList()
    }
    val parsedCapital = initialCapitalText.filter { it.isDigit() }.toIntOrNull() ?: 0

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgCanvas,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // Top Bar
                Surface(color = SurfaceWhite) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = KalaOrderShapes.Medium,
                                    color = PrimaryEmerald,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Storefront,
                                            contentDescription = "KalaOrder",
                                            tint = SurfaceWhite,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Siapkan KalaOrder",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = "Langkah $step dari 2 • Siap pakai bersih",
                                        style = KalaOrderMonoStyles.CodeSmall,
                                        color = PrimaryEmerald,
                                    )
                                }
                            }
                            TextButton(
                                onClick = onSkip,
                                modifier = Modifier.testTag("setup_skip_button"),
                            ) {
                                Text(
                                    text = "Lewati",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                )
                            }
                        }
                        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (step == 1) {
                        KalaOrderCard(
                            containerColor = PrimarySoft,
                            borderColor = StatusSelesaiBorder,
                            contentPadding = PaddingValues(16.dp),
                        ) {
                            Text(
                                text = "Mulai Usaha Tanpa Data Contoh",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Aplikasi dimulai kosong bersih (Rp 0 omzet & 0 pesanan). Isi nama usahamu di bawah agar struk, Bot WA, dan laporan keuangan langsung sesuai toko kamu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }

                        KalaOrderCard(contentPadding = PaddingValues(16.dp)) {
                            Text(
                                text = "1. Identitas Usaha & Modal Awal",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("Nama Usaha / Warung") },
                                placeholder = { Text("Contoh: Warung Berkah / Kedai Kopi Alfa") },
                                singleLine = true,
                                shape = KalaOrderShapes.Medium,
                                colors = setupFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_store_name_input"),
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = whatsappNumber,
                                onValueChange = { whatsappNumber = it },
                                label = { Text("Nomor WhatsApp Toko (Opsional)") },
                                placeholder = { Text("Contoh: 0812-3456-7890") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = KalaOrderShapes.Medium,
                                colors = setupFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_wa_number_input"),
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = initialCapitalText,
                                onValueChange = { input -> initialCapitalText = input.filter { it.isDigit() }.take(9) },
                                label = { Text("Modal Kas Awal (Opsional)") },
                                placeholder = { Text("Contoh: 500000") },
                                prefix = {
                                    Text(
                                        text = "Rp ",
                                        style = KalaOrderMonoStyles.CodeMedium,
                                        color = TextSecondary,
                                    )
                                },
                                supportingText = {
                                    Text(
                                        text = if (parsedCapital > 0) {
                                            "Tercatat otomatis sebagai Modal Awal: ${RupiahFormatter.format(parsedCapital)}"
                                        } else {
                                            "Boleh dikosongkan dan diisi nanti di tab Keuangan"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (parsedCapital > 0) PrimaryEmerald else TextMuted,
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = KalaOrderShapes.Medium,
                                colors = setupFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_initial_capital_input"),
                            )
                        }
                    } else {
                        KalaOrderCard(contentPadding = PaddingValues(16.dp)) {
                            Text(
                                text = "2. Isi Daftar Menu Usaha",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pilih cara paling cepat untuk mengisi katalog menu, atau mulai kosong dulu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(OnboardingMenuChoice.entries) { choice ->
                                    KalaFilterChip(
                                        label = choice.label,
                                        selected = menuChoice == choice,
                                        onClick = { menuChoice = choice },
                                        modifier = Modifier.testTag("setup_menu_choice_${choice.name.lowercase()}"),
                                    )
                                }
                            }
                        }

                        when (menuChoice) {
                            OnboardingMenuChoice.BULK_TEXT -> {
                                BulkMenuTextInputCard(
                                    defaultCategory = defaultCategory,
                                    onSelectCategory = { defaultCategory = it },
                                    defaultStockText = defaultStockText,
                                    onDefaultStockChange = { defaultStockText = it },
                                    rawText = bulkRawText,
                                    onRawTextChange = { bulkRawText = it },
                                    parsedItems = parsedBulkItems,
                                )
                            }

                            OnboardingMenuChoice.TEMPLATE -> {
                                TemplateMenuPickerCard(
                                    selectedPackId = selectedPackId,
                                    onSelectPack = { selectedPackId = it },
                                    checkedKeys = checkedTemplateKeys,
                                )
                            }

                            OnboardingMenuChoice.EMPTY -> {
                                KalaOrderCard(
                                    containerColor = FillSubtle,
                                    borderColor = BorderSubtle,
                                    contentPadding = PaddingValues(16.dp),
                                ) {
                                    Text(
                                        text = "Mulai 100% Kosong Bersih",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Katalog menu, antrean, dan keuangan akan mulai dari 0. Kamu bisa menambah menu satu per satu atau banyak sekaligus kapan saja lewat tab Menu.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = CreatorInfo.ATTRIBUTION_FOOTER,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 4.dp),
                    )
                }

                // Bottom Action Bar
                Surface(color = SurfaceWhite) {
                    Column {
                        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (step == 1) {
                                KalaSecondaryButton(
                                    text = "Mulai Kosong",
                                    onClick = {
                                        onComplete(
                                            storeName.ifBlank { "Usaha Saya" },
                                            whatsappNumber,
                                            parsedCapital,
                                            emptyList(),
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("setup_start_empty_button"),
                                )
                                KalaPrimaryButton(
                                    text = "Lanjut Isi Menu",
                                    onClick = { step = 2 },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("setup_next_step_button"),
                                )
                            } else {
                                KalaSecondaryButton(
                                    text = "Kembali",
                                    onClick = { step = 1 },
                                    modifier = Modifier.weight(0.9f),
                                )
                                KalaPrimaryButton(
                                    text = if (finalInitialMenus.isNotEmpty()) {
                                        "Simpan (${finalInitialMenus.size} Menu)"
                                    } else {
                                        "Selesai & Mulai"
                                    },
                                    icon = Icons.Rounded.Check,
                                    onClick = {
                                        onComplete(
                                            storeName.ifBlank { "Usaha Saya" },
                                            whatsappNumber,
                                            parsedCapital,
                                            finalInitialMenus,
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("setup_finish_button"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkMenuSetupBottomSheet(
    initialMode: BulkMenuMode,
    existingMenuCodes: Set<String>,
    onDismiss: () -> Unit,
    onSaveMenus: (List<MenuItemRecord>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeMode by remember(initialMode) { mutableStateOf(initialMode) }

    var defaultCategory by remember { mutableStateOf(MenuCategory.MAKANAN) }
    var defaultStockText by remember { mutableStateOf("20") }
    var bulkRawText by remember { mutableStateOf("") }

    var selectedPackId by remember { mutableStateOf(MenuStarterCatalog.starterPacks.first().id) }
    val selectedPack = MenuStarterCatalog.starterPacks.firstOrNull { it.id == selectedPackId }
        ?: MenuStarterCatalog.starterPacks.first()
    val checkedTemplateKeys = remember {
        mutableStateMapOf<String, Boolean>().apply {
            MenuStarterCatalog.starterPacks.forEach { pack ->
                pack.items.forEach { item ->
                    put("${pack.id}_${item.code}", true)
                }
            }
        }
    }

    val defaultStock = defaultStockText.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 999) ?: 20
    val parsedBulkItems = remember(bulkRawText, defaultCategory, defaultStock, existingMenuCodes) {
        MenuStarterCatalog.parseBulkMenuLines(
            rawText = bulkRawText,
            defaultCategory = defaultCategory,
            defaultStock = defaultStock,
            existingCodes = existingMenuCodes,
        )
    }

    val selectedTemplateItems = selectedPack.items.filter { item ->
        checkedTemplateKeys["${selectedPack.id}_${item.code}"] != false
    }

    val itemsToSave = when (activeMode) {
        BulkMenuMode.BULK_TEXT -> parsedBulkItems
        BulkMenuMode.TEMPLATE -> selectedTemplateItems
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Isi Daftar Menu Cepat",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Text(
                text = "Masukkan banyak menu sekaligus dari daftar teks atau pilih paket template siap pakai.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BulkMenuMode.entries) { mode ->
                    KalaFilterChip(
                        label = mode.label,
                        selected = activeMode == mode,
                        onClick = { activeMode = mode },
                        modifier = Modifier.testTag("bulk_mode_${mode.name.lowercase()}"),
                    )
                }
            }

            when (activeMode) {
                BulkMenuMode.BULK_TEXT -> {
                    BulkMenuTextInputCard(
                        defaultCategory = defaultCategory,
                        onSelectCategory = { defaultCategory = it },
                        defaultStockText = defaultStockText,
                        onDefaultStockChange = { defaultStockText = it },
                        rawText = bulkRawText,
                        onRawTextChange = { bulkRawText = it },
                        parsedItems = parsedBulkItems,
                    )
                }

                BulkMenuMode.TEMPLATE -> {
                    TemplateMenuPickerCard(
                        selectedPackId = selectedPackId,
                        onSelectPack = { selectedPackId = it },
                        checkedKeys = checkedTemplateKeys,
                    )
                }
            }

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
                    text = if (itemsToSave.isNotEmpty()) {
                        "Simpan ${itemsToSave.size} Menu"
                    } else {
                        "Simpan Menu"
                    },
                    enabled = itemsToSave.isNotEmpty(),
                    onClick = {
                        onSaveMenus(itemsToSave)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("bulk_menu_save_button"),
                )
            }
        }
    }
}

@Composable
private fun BulkMenuTextInputCard(
    defaultCategory: MenuCategory,
    onSelectCategory: (MenuCategory) -> Unit,
    defaultStockText: String,
    onDefaultStockChange: (String) -> Unit,
    rawText: String,
    onRawTextChange: (String) -> Unit,
    parsedItems: List<MenuItemRecord>,
) {
    KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
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
                    imageVector = Icons.Outlined.PlaylistAdd,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Daftar Teks Multi-Baris",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
            }
            TextButton(
                onClick = { onRawTextChange(MenuStarterCatalog.BULK_SAMPLE_TEXT) },
                modifier = Modifier.testTag("bulk_fill_sample_button"),
            ) {
                Text(
                    text = "Isi Contoh",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryEmerald,
                )
            }
        }

        Text(
            text = "Tulis 1 menu per baris: Nama Menu, Harga (atau Nama - 15rb). Kode A1/B1/C1 dibuat otomatis.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(MenuCategory.entries) { cat ->
                    KalaFilterChip(
                        label = cat.label,
                        selected = defaultCategory == cat,
                        onClick = { onSelectCategory(cat) },
                    )
                }
            }
            OutlinedTextField(
                value = defaultStockText,
                onValueChange = { onDefaultStockChange(it.filter { ch -> ch.isDigit() }.take(3)) },
                label = { Text("Stok") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = KalaOrderShapes.Medium,
                colors = setupFieldColors(),
                modifier = Modifier.width(76.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = rawText,
            onValueChange = onRawTextChange,
            label = { Text("Tempel / Ketik Daftar Menu") },
            placeholder = {
                Text("Nasi Goreng, 18000\nAyam Geprek - 17000\nEs Teh Manis, 5000")
            },
            minLines = 4,
            maxLines = 8,
            shape = KalaOrderShapes.Medium,
            colors = setupFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bulk_menu_text_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (parsedItems.isNotEmpty()) {
            Surface(
                shape = KalaOrderShapes.Medium,
                color = PrimarySoft,
                border = BorderStroke(1.dp, StatusSelesaiBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Pratinjau Siap Simpan (${parsedItems.size} menu terbaca)",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryEmerald,
                    )
                    parsedItems.take(8).forEach { item ->
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
                                MonospaceCodeTag(code = item.code, emphasized = true)
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = RupiahFormatter.format(item.price),
                                style = KalaOrderMonoStyles.CodeSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryEmerald,
                            )
                        }
                    }
                    if (parsedItems.size > 8) {
                        Text(
                            text = "+${parsedItems.size - 8} menu lainnya siap disimpan",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        } else if (rawText.isNotBlank()) {
            Text(
                text = "Format belum terbaca. Pastikan setiap baris memuat nama menu dan harga angka (contoh: Nasi Goreng, 18000).",
                style = MaterialTheme.typography.bodySmall,
                color = StatusDibatalkanText,
            )
        }
    }
}

@Composable
private fun TemplateMenuPickerCard(
    selectedPackId: String,
    onSelectPack: (String) -> Unit,
    checkedKeys: MutableMap<String, Boolean>,
) {
    val selectedPack = MenuStarterCatalog.starterPacks.firstOrNull { it.id == selectedPackId }
        ?: MenuStarterCatalog.starterPacks.first()

    KalaOrderCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = PrimaryEmerald,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Pilih Paket Template Usaha",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Centang item yang sesuai usahamu. Harga & stok bisa langsung diedit kapan saja di tab Menu.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MenuStarterCatalog.starterPacks) { pack ->
                KalaFilterChip(
                    label = pack.title,
                    selected = pack.id == selectedPack.id,
                    onClick = { onSelectPack(pack.id) },
                    modifier = Modifier.testTag("template_pack_${pack.id}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = selectedPack.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            selectedPack.items.forEach { item ->
                val key = "${selectedPack.id}_${item.code}"
                val isChecked = checkedKeys[key] != false
                Surface(
                    shape = KalaOrderShapes.Medium,
                    color = if (isChecked) PrimarySoft.copy(alpha = 0.55f) else FillSubtle,
                    border = BorderStroke(1.dp, if (isChecked) StatusSelesaiBorder else BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(KalaOrderShapes.Medium)
                        .clickable { checkedKeys[key] = !isChecked },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checkedKeys[key] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryEmerald,
                                uncheckedColor = BorderStrong,
                            ),
                        )
                        MonospaceCodeTag(code = item.code, emphasized = isChecked)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.category.label} • Stok ${item.stock}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        Text(
                            text = RupiahFormatter.format(item.price),
                            style = KalaOrderMonoStyles.CodeSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryEmerald,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectiveResetDataDialog(
    orderCount: Int,
    cashflowCount: Int,
    menuCount: Int,
    botLogCount: Int,
    onDismiss: () -> Unit,
    onConfirmReset: (DataResetSelection) -> Unit,
) {
    var clearOrders by remember { mutableStateOf(true) }
    var clearCashflow by remember { mutableStateOf(true) }
    var clearMenu by remember { mutableStateOf(false) }
    var clearBotLogs by remember { mutableStateOf(true) }

    val selection = DataResetSelection(
        clearOrders = clearOrders,
        clearCashflow = clearCashflow,
        clearMenu = clearMenu,
        clearBotLogs = clearBotLogs,
    )

    val allSelected = clearOrders && clearCashflow && clearMenu && clearBotLogs

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = KalaOrderShapes.Card,
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = null,
                tint = StatusDibatalkanText,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Text(
                text = "Reset Data Terpilih",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih data yang ingin dikosongkan. Katalog menu tidak dicentang otomatis agar daftar menu tetap aman saat menghapus data uji coba.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            val next = !allSelected
                            clearOrders = next
                            clearCashflow = next
                            clearMenu = next
                            clearBotLogs = next
                        },
                        modifier = Modifier.testTag("reset_select_all_toggle"),
                    ) {
                        Text(
                            text = if (allSelected) "Batal Pilih Semua" else "Pilih Semua (Mulai dari Nol)",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusDibatalkanText,
                        )
                    }
                }

                ResetOptionRow(
                    title = "Antrean & Riwayat Pesanan",
                    subtitle = "$orderCount pesanan tersimpan (nomor balik ke KO-0001)",
                    checked = clearOrders,
                    onCheckedChange = { clearOrders = it },
                    testTag = "reset_option_orders",
                )
                ResetOptionRow(
                    title = "Catatan Keuangan Manual",
                    subtitle = "$cashflowCount catatan pengeluaran, modal & kas lain",
                    checked = clearCashflow,
                    onCheckedChange = { clearCashflow = it },
                    testTag = "reset_option_cashflow",
                )
                ResetOptionRow(
                    title = "Katalog Menu",
                    subtitle = "$menuCount item menu (centang jika ingin kosongkan menu)",
                    checked = clearMenu,
                    onCheckedChange = { clearMenu = it },
                    testTag = "reset_option_menu",
                )
                ResetOptionRow(
                    title = "Log & Cooldown Bot WA",
                    subtitle = "$botLogCount log aktivitas & jeda salam kontak",
                    checked = clearBotLogs,
                    onCheckedChange = { clearBotLogs = it },
                    testTag = "reset_option_bot_logs",
                )

                Surface(
                    shape = KalaOrderShapes.Medium,
                    color = StatusDibatalkanBg.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, StatusDibatalkanBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = StatusDibatalkanText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Data yang dihapus tidak dapat dikembalikan. Pastikan kamu sudah Ekspor CSV jika butuh arsip.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusDibatalkanText,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selection.hasAnySelection,
                onClick = {
                    onConfirmReset(selection)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_selective_reset_button"),
            ) {
                Text(
                    text = "Hapus Data Terpilih",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selection.hasAnySelection) StatusDibatalkanText else TextMuted,
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
private fun ResetOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Surface(
        shape = KalaOrderShapes.Medium,
        color = if (checked) StatusDibatalkanBg.copy(alpha = 0.25f) else FillSubtle,
        border = BorderStroke(1.dp, if (checked) StatusDibatalkanBorder else BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(KalaOrderShapes.Medium)
            .testTag(testTag)
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = StatusDibatalkanText,
                    uncheckedColor = BorderStrong,
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun setupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryEmerald,
    unfocusedBorderColor = BorderStrong,
    focusedLabelColor = PrimaryEmerald,
    cursorColor = PrimaryEmerald,
)
