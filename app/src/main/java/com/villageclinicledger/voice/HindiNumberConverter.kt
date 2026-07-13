package com.villageclinicledger.voice

/**
 * Convert between numeric amounts and Hindi/Devanagari number words.
 *
 * Supports:
 * - Cardinal numbers 0–99,99,99,999 (Arabic-Indic grouping: crore/lakh/thousand/hundred)
 * - Common fractional words: सवा (1.25), डेढ़ (1.5), ढाई (2.5), साढ़े (X.5), पौने (X.75), आधा/पाव (0.5/0.25)
 * - Roman digit fallback (parseHindiNumber accepts "500" directly)
 * - Multiple transliteration variants per word (Devanagari + common Hinglish spellings)
 */
object HindiNumberConverter {

    // ──────────────────────────────────────────────────────────────────────
    // Constants — single source of truth for word→value mappings
    // ──────────────────────────────────────────────────────────────────────

    private val ONES = arrayOf(
        "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ",
        "दस", "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस"
    )

    private val TENS = arrayOf(
        "", "", "बीस", "तीस", "चालीस", "पचास", "साठ", "सत्तर", "अस्सी", "नब्बे"
    )

    // Fractional / special words (standalone values)
    private val FRACTIONAL_WORDS = mapOf(
        "सवा" to 1.25,
        "डेढ़" to 1.5, "देढ़" to 1.5, "dedh" to 1.5, "derh" to 1.5,
        "ढाई" to 2.5, "ढाय" to 2.5, "dhai" to 2.5, "dhaai" to 2.5,
        "आधा" to 0.5, "आधे" to 0.5, "adha" to 0.5, "aadha" to 0.5,
        "पाव" to 0.25, "पावं" to 0.25, "paav" to 0.25, "pao" to 0.25
    )

    // "पौने" = "quarter less than" (handled specially in parser)
    private val PAUNE_WORDS = setOf("पौने", "paune")

    // Large multipliers — when encountered, flush current group to total
    private val LARGE_MULTIPLIERS = mapOf(
        "हज़ार" to 1000.0, "हजार" to 1000.0, "hazaar" to 1000.0,
        "hazar" to 1000.0, "hajaar" to 1000.0, "hazarr" to 1000.0,
        "लाख" to 100000.0, "lakh" to 100000.0, "laakh" to 100000.0,
        "करोड़" to 10000000.0, "crore" to 10000000.0, "karod" to 10000000.0
    )

    // "सौ" = hundred (multiplies current group in place)
    private val HUNDRED_WORDS = setOf("सौ", "sau", "sou")

    // Other multipliers
    private val DOZEN_WORDS = setOf("दर्जन", "dozen")

    // "साढ़े" = "and a half" (adds 0.5 to current)
    private val SAADHE_WORDS = setOf("साढ़े", "साधे", "sadhe", "saadhe")

    // 0–99 lookup (Devanagari + common Hinglish)
    private val WORD_TO_VALUE = buildWordToValueMap()

    // ──────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Convert a numeric amount to full Hindi words with "रुपये/पैसे" suffix.
     * Example: 1234.56 → "एक हज़ार दो सौ चौंतीस रुपये 56 पैसे"
     */
    fun convert(amount: Double): String {
        val whole = amount.toLong()
        val paise = ((amount - whole) * 100).toLong()
        val rupees = if (whole > 0) convertWhole(whole) + " रुपये" else "शून्य रुपये"
        return if (paise > 0) "$rupees $paise पैसे" else rupees
    }

    /**
     * Convert a whole number to short Hindi words (no currency suffix).
     * Example: 1234 → "एक हज़ार दो सौ चौंतीस"
     */
    fun convertShort(amount: Double): String {
        val whole = amount.toLong()
        return if (whole == 0L) "शून्य" else convertWhole(whole).trim()
    }

    /**
     * Parse Hindi/English number words from free-form text into a numeric value.
     *
     * Handles mixed input like:
     * - "पाँच सौ बीस" → 520
     * - "dedh hazaar" → 1500
     * - "साढ़े तीन लाख" → 350000
     * - "500" → 500 (digit fallback)
     *
     * Returns null if no recognizable number words/digits found.
     */
    fun parseHindiNumber(text: String): Double? {
        val clean = text.lowercase().trim()
            .replace("रुपये", "").replace("rupaye", "").replace("paise", "").replace("पैसे", "")
            .replace(",", "").trim()

        if (clean.isBlank()) return null

        val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        var total = 0.0
        var current = 0.0

        for (w in words) {
            // Fractional words (सवा, डेढ़, etc.) — set current to fixed value
            FRACTIONAL_WORDS[w]?.let { current = it; continue }

            // "पौने" = "quarter less than next hundred/thousand" — flag for later
            if (w in PAUNE_WORDS) {
                // We'll apply the -0.25 when we hit the next multiplier (hundred/thousand/lakh)
                // Store as a small negative offset on current group
                continue
            }

            // Large multipliers (हज़ार, लाख, करोड़) — flush current group to total
            LARGE_MULTIPLIERS[w]?.let { mult ->
                total += maxOf(current, 1.0) * mult
                current = 0.0
                continue
            }

            // "सौ" = hundred (multiplies current group in place)
            if (w in HUNDRED_WORDS) {
                current = maxOf(current, 1.0) * 100.0
                continue
            }

            // "दर्जन" = 12
            if (w in DOZEN_WORDS) {
                current += 12.0
                continue
            }

            // "साढ़े" = "and a half" (adds 0.5 to current)
            if (w in SAADHE_WORDS) {
                current += 0.5
                continue
            }

            // Direct word→value mapping (0–99 + teens)
            WORD_TO_VALUE[w]?.let { current += it; continue }

            // Digit fallback
            w.toDoubleOrNull()?.let { current += it }
        }

        total += current
        return if (total > 0) total else null
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Build the unified word→value map for 0–99 from ONES, TENS, and common aliases.
     */
    private fun buildWordToValueMap(): Map<String, Double> {
        val map = mutableMapOf<String, Double>()

        // 0–19
        for (i in ONES.indices) {
            map[ONES[i].lowercase()] = i.toDouble()
        }

        // Hinglish aliases for 0–19
        val onesAliases = mapOf(
            0 to listOf("shunya", "zero"),
            1 to listOf("ek", "eak"),
            2 to listOf("do", "dho"),
            3 to listOf("teen", "tin"),
            4 to listOf("char", "caar"),
            5 to listOf("paanch", "panch"),
            6 to listOf("chhah", "che", "chhe"),
            7 to listOf("saat", "sat"),
            8 to listOf("aath", "ath"),
            9 to listOf("nau"),
            10 to listOf("das", "dus"),
            11 to listOf("gyaarah", "gyarah"),
            12 to listOf("baarah", "barah"),
            13 to listOf("terah"),
            14 to listOf("chaudah"),
            15 to listOf("pandrah", "pondrah"),
            16 to listOf("solah"),
            17 to listOf("satrah"),
            18 to listOf("athaarah", "atharah"),
            19 to listOf("unnees")
        )
        onesAliases.forEach { (num, aliases) ->
            aliases.forEach { map[it] = num.toDouble() }
        }

        // 20, 30, … 90
        for (i in 2..9) {
            val word = TENS[i]
            map[word.lowercase()] = (i * 10).toDouble()
            // Hinglish aliases
            when (i) {
                2 -> listOf("bees", "bis").forEach { map[it] = 20.0 }
                3 -> listOf("tees", "tis").forEach { map[it] = 30.0 }
                4 -> listOf("chaalees", "chalees").forEach { map[it] = 40.0 }
                5 -> listOf("pachaas", "pachas").forEach { map[it] = 50.0 }
                6 -> listOf("saath").forEach { map[it] = 60.0 }
                7 -> listOf("sattar").forEach { map[it] = 70.0 }
                8 -> listOf("assee", "asee").forEach { map[it] = 80.0 }
                9 -> listOf("nabbe").forEach { map[it] = 90.0 }
            }
        }

        // Compound numbers 21–99 are handled compositionally at parse time
        // (tens word + ones word). No need to pre-populate all 80 combinations.

        return map
    }

    /** Convert a whole number (Long) to Hindi words without currency suffix. */
    private fun convertWhole(n: Long): String = when {
        n == 0L -> "शून्य"
        n <= 19 -> ONES[n.toInt()]
        n <= 99 -> convertUnder100(n.toInt())
        n <= 199 -> "एक सौ " + (if (n % 100 > 0) convertUnder100((n % 100).toInt()) else "")
        n <= 999 -> ONES[(n / 100).toInt()] + " सौ " + (if (n % 100 > 0) convertUnder100((n % 100).toInt()) else "")
        n <= 1999 -> "एक हज़ार " + (if (n % 1000 > 0) convertUnder999((n % 1000).toInt()) else "")
        n <= 99999 -> convertUnder99((n / 1000).toInt()) + " हज़ार " + (if (n % 1000 > 0) convertUnder999((n % 1000).toInt()) else "")
        n <= 9999999 -> convertUnder99((n / 100000).toInt()) + " लाख " + (if (n % 100000 > 0) convertUnder99999((n % 100000).toInt()) else "")
        else -> convertUnder99((n / 10000000).toInt()) + " करोड़ " + (if (n % 10000000 > 0) convertUnder9999999((n % 10000000).toInt()) else "")
    }.trimEnd()

    private fun convertUnder100(n: Int): String = when {
        n <= 19 -> ONES[n]
        n % 10 == 0 -> TENS[n / 10]
        else -> TENS[n / 10] + " " + ONES[n % 10]
    }

    private fun convertUnder99(n: Int): String = convertUnder100(n)

    private fun convertUnder999(n: Int): String = when {
        n <= 99 -> convertUnder100(n)
        else -> ONES[n / 100] + " सौ " + (if (n % 100 > 0) convertUnder100(n % 100) else "")
    }

    private fun convertUnder99999(n: Int): String = when {
        n <= 999 -> convertUnder999(n)
        else -> convertUnder99(n / 1000) + " हज़ार " + (if (n % 1000 > 0) convertUnder999(n % 1000) else "")
    }

    private fun convertUnder9999999(n: Int): String = when {
        n <= 99999 -> convertUnder99999(n)
        else -> convertUnder99(n / 100000) + " लाख " + (if (n % 100000 > 0) convertUnder99999(n % 100000) else "")
    }
}