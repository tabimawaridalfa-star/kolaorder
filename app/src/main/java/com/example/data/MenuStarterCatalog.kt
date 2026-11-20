package com.example.data

/**
 * Curated UMKM starter menu templates and flexible multi-line bulk menu parser
 * so merchants can populate their catalog in seconds or start cleanly empty.
 */
data class StarterMenuPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val items: List<MenuItemRecord>,
)

object MenuStarterCatalog {

    val starterPacks: List<StarterMenuPack> = listOf(
        StarterMenuPack(
            id = "warung_makan",
            title = "Warung Makan",
            subtitle = "Nasi goreng, geprek, soto & minuman segar",
            items = listOf(
                MenuItemRecord("A1", "Nasi Goreng Spesial", MenuCategory.MAKANAN, 20000, 20, true),
                MenuItemRecord("A2", "Ayam Geprek Sambal Bawang", MenuCategory.MAKANAN, 18000, 20, true),
                MenuItemRecord("A3", "Mie Goreng Jawa", MenuCategory.MAKANAN, 16000, 20, true),
                MenuItemRecord("A4", "Soto Ayam Lamongan", MenuCategory.MAKANAN, 18000, 15, true),
                MenuItemRecord("B1", "Es Teh Manis Jumbo", MenuCategory.MINUMAN, 5000, 40, true),
                MenuItemRecord("B2", "Es Jeruk Peras", MenuCategory.MINUMAN, 7000, 30, true),
                MenuItemRecord("C1", "Tahu Crispy Lada Garam", MenuCategory.CAMILAN, 10000, 20, true),
            ),
        ),
        StarterMenuPack(
            id = "kedai_kopi",
            title = "Kedai Kopi",
            subtitle = "Kopi susu, non-kopi & roti bakar",
            items = listOf(
                MenuItemRecord("B1", "Kopi Susu Gula Aren", MenuCategory.MINUMAN, 16000, 30, true),
                MenuItemRecord("B2", "Iced Americano", MenuCategory.MINUMAN, 14000, 30, true),
                MenuItemRecord("B3", "Matcha Latte Creamy", MenuCategory.MINUMAN, 18000, 25, true),
                MenuItemRecord("B4", "Cokelat Klasik Dingin", MenuCategory.MINUMAN, 15000, 25, true),
                MenuItemRecord("B5", "Thai Tea Original", MenuCategory.MINUMAN, 12000, 30, true),
                MenuItemRecord("C1", "Roti Bakar Cokelat Keju", MenuCategory.CAMILAN, 14000, 20, true),
                MenuItemRecord("C2", "Kentang Goreng Saus Keju", MenuCategory.CAMILAN, 13000, 20, true),
            ),
        ),
        StarterMenuPack(
            id = "camilan_jajanan",
            title = "Jajanan & Snack",
            subtitle = "Pisang goreng, dimsum, cireng & es",
            items = listOf(
                MenuItemRecord("C1", "Pisang Goreng Cokelat Keju", MenuCategory.CAMILAN, 13000, 25, true),
                MenuItemRecord("C2", "Dimsum Ayam (Isi 5)", MenuCategory.CAMILAN, 15000, 25, true),
                MenuItemRecord("C3", "Cireng Bumbu Rujak", MenuCategory.CAMILAN, 10000, 30, true),
                MenuItemRecord("C4", "Sosis Bakar Jumbo", MenuCategory.CAMILAN, 12000, 25, true),
                MenuItemRecord("B1", "Es Teh Leci Segar", MenuCategory.MINUMAN, 8000, 30, true),
                MenuItemRecord("B2", "Es Milo Dinosaur", MenuCategory.MINUMAN, 10000, 25, true),
            ),
        ),
    )

    const val BULK_SAMPLE_TEXT: String =
        "Nasi Goreng Spesial, 18000\nAyam Geprek Sambal, 17000\nEs Teh Manis, 5000\nTahu Crispy, 10000"

    /**
     * Parses multi-line menu text into MenuItemRecord objects with automatic unique codes.
     *
     * Supported formats per line:
     * - "Nasi Goreng, 18000"
     * - "Es Teh Manis - 5000"
     * - "Ayam Geprek = 20rb" or "Ayam Geprek 20k"
     * - "A1, Nasi Goreng, 18000, 25"
     * - "B2; Es Jeruk; 7000"
     */
    fun parseBulkMenuLines(
        rawText: String,
        defaultCategory: MenuCategory,
        defaultStock: Int = 20,
        existingCodes: Set<String> = emptySet(),
    ): List<MenuItemRecord> {
        if (rawText.isBlank()) return emptyList()

        val usedCodes = existingCodes.map { it.uppercase() }.toMutableSet()
        val parsed = mutableListOf<MenuItemRecord>()
        val safeDefaultStock = defaultStock.coerceIn(1, 999)

        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val item = parseSingleLine(
                    line = line,
                    defaultCategory = defaultCategory,
                    defaultStock = safeDefaultStock,
                    usedCodes = usedCodes,
                )
                if (item != null) {
                    usedCodes.add(item.code.uppercase())
                    parsed.add(item)
                }
            }

        return parsed
    }

    private fun parseSingleLine(
        line: String,
        defaultCategory: MenuCategory,
        defaultStock: Int,
        usedCodes: Set<String>,
    ): MenuItemRecord? {
        // Strip leading bullet or numbering like "1. " or "- "
        val cleaned = line.replace(Regex("""^\s*(?:[-•*]|\d+[.)])\s+"""), "").trim()
        if (cleaned.isEmpty()) return null

        // Split by comma, semicolon, pipe, or spaced dash/equals
        val tokens = cleaned
            .split(Regex("""[,;|]|\s+[-–=]\s+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var explicitCode: String? = null
        var name = ""
        var price = 0
        var stock = defaultStock

        when {
            tokens.size >= 3 && isMenuCode(tokens[0]) -> {
                explicitCode = tokens[0].uppercase()
                name = tokens[1]
                price = parseRupiahValue(tokens[2])
                if (tokens.size >= 4) {
                    stock = tokens[3].filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 999) ?: defaultStock
                }
            }

            tokens.size >= 2 -> {
                name = tokens[0]
                price = parseRupiahValue(tokens[1])
                if (tokens.size >= 3) {
                    stock = tokens[2].filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 999) ?: defaultStock
                }
            }

            else -> {
                // Fallback: single string like "Nasi Goreng 18000" or "Es Teh 5k"
                val trailingPriceRegex = Regex("""^(.*?)\s+(?:Rp\.?\s*)?(\d+(?:[.,]\d{1,3})?\s*(?:rb|ribu|k)?)\s*$""", RegexOption.IGNORE_CASE)
                val match = trailingPriceRegex.find(cleaned) ?: return null
                name = match.groupValues[1].trim().trimEnd('-', ':', '=')
                price = parseRupiahValue(match.groupValues[2])
            }
        }

        if (name.isBlank() || price <= 0) return null

        val category = inferCategoryIfNeeded(name, defaultCategory)
        val code = explicitCode?.takeIf { it !in usedCodes }
            ?: nextAvailableCode(category, usedCodes)

        return MenuItemRecord(
            code = code,
            name = name,
            category = category,
            price = price,
            stock = stock,
            isAvailable = stock > 0,
        )
    }

    private fun isMenuCode(token: String): Boolean =
        token.matches(Regex("""^[A-Za-z]\d{1,3}$"""))

    private fun parseRupiahValue(raw: String): Int {
        val trimmed = raw.trim().lowercase()
        val multiplier = when {
            trimmed.endsWith("rb") || trimmed.endsWith("ribu") || trimmed.endsWith("k") -> 1000
            else -> 1
        }
        val stripped = trimmed
            .replace("rp", "")
            .replace("ribu", "")
            .replace("rb", "")
            .replace("k", "")
            .replace(" ", "")

        return if (multiplier == 1000) {
            val normalized = stripped.replace(",", ".")
            val asDouble = normalized.toDoubleOrNull() ?: return 0
            (asDouble * 1000.0).toInt().coerceAtLeast(0)
        } else {
            val digits = stripped.filter { it.isDigit() }
            digits.toIntOrNull()?.coerceAtLeast(0) ?: 0
        }
    }

    private fun inferCategoryIfNeeded(name: String, defaultCategory: MenuCategory): MenuCategory {
        val lower = name.lowercase()
        return when {
            lower.startsWith("es ") ||
                lower.contains("kopi") ||
                lower.contains("teh") ||
                lower.contains("jeruk") ||
                lower.contains("jus") ||
                lower.contains("latte") ||
                lower.contains("thai tea") ||
                lower.contains("milo") ||
                lower.contains("air mineral") -> MenuCategory.MINUMAN

            lower.contains("pisang goreng") ||
                lower.contains("tahu crispy") ||
                lower.contains("kentang") ||
                lower.contains("roti bakar") ||
                lower.contains("cireng") ||
                lower.contains("dimsum") ||
                lower.contains("snack") -> MenuCategory.CAMILAN

            else -> defaultCategory
        }
    }

    private fun nextAvailableCode(category: MenuCategory, usedCodes: Set<String>): String {
        val prefix = when (category) {
            MenuCategory.MAKANAN -> "A"
            MenuCategory.MINUMAN -> "B"
            MenuCategory.CAMILAN -> "C"
        }
        var number = 1
        while ("$prefix$number" in usedCodes) {
            number++
        }
        return "$prefix$number"
    }
}
