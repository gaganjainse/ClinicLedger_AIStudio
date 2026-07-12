package com.villageclinicledger.voice

import java.util.Locale

/**
 * Data class representing a parsed voice intent.
 *
 * @param intent The type of intent detected (medicine, payment, search, etc.)
 * @param patientName The patient's name if extracted
 * @param villageName The village name if extracted
 * @param medicineAmount Amount for medicine/dues entry
 * @param paymentAmount Amount for payment received
 * @param confidence Confidence score of the parse (0.0-1.0)
 */
data class ParsedVoiceIntent(
    val intent: IntentType,
    val patientName: String? = null,
    val villageName: String? = null,
    val medicineAmount: Double? = null,
    val paymentAmount: Double? = null,
    val confidence: Float = 0f
)

/**
 * Enum of all supported voice intent types.
 */
enum class IntentType {
    SEARCH_BALANCE,
    MEDICINE,
    PAYMENT,
    MEDICINE_AND_PAYMENT,
    NEW_PATIENT,
    CORRECTION,
    CONFIRM_YES,
    CONFIRM_NO,
    UNKNOWN
}

/**
 * Parses natural-language Hindi/English voice input into structured [ParsedVoiceIntent].
 *
 * The parser is designed for rural clinic staff who speak in mixed Hindi-English:
 * - "Ramesh ko 200 ki dawa di" → MEDICINE intent, patient=Ramesh, medicine=200
 * - "Suresh se 500 jama kiye" → PAYMENT intent, patient=Suresh, payment=500
 * - "Mahesh ka baki kitna hai" → SEARCH_BALANCE intent, patient=Mahesh
 *
 * [villages] is an optional list of known villages for fuzzy village name matching.
 * If provided, village names are matched against the spoken text for better accuracy.
 */
object VoiceIntentParser {

    // ──────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Main entry point: parse raw voice text into a structured intent.
     *
     * @param text Raw text from speech recognizer (will be lowercased & normalized)
     * @param villages Optional list of known villages for name matching
     * @return [ParsedVoiceIntent] with detected intent and extracted entities
     */
    fun parse(
        text: String,
        villages: List<com.villageclinicledger.data.models.Village>? = null
    ): ParsedVoiceIntent {
        val clean = normalize(text)
        val intent = detectIntent(clean)
        val patientName = extractPatientName(clean)
        val villageName = extractVillageName(clean, villages)
        val (medicineAmount, paymentAmount) = extractAmounts(clean, intent)

        return ParsedVoiceIntent(
            intent = intent,
            patientName = patientName,
            villageName = villageName,
            medicineAmount = medicineAmount,
            paymentAmount = paymentAmount,
            confidence = 0.7f
        )
    }

    /**
     * Detect the intent type from normalized text.
     */
    fun detectIntent(text: String): IntentType = when {
        isConfirmYes(text) -> IntentType.CONFIRM_YES
        isConfirmNo(text) -> IntentType.CONFIRM_NO
        isCorrection(text) -> IntentType.CORRECTION
        isNewPatient(text) -> IntentType.NEW_PATIENT
        isMedicine(text) && isPayment(text) -> IntentType.MEDICINE_AND_PAYMENT
        isPayment(text) -> IntentType.PAYMENT
        isMedicine(text) -> IntentType.MEDICINE
        isSearchBalance(text) -> IntentType.SEARCH_BALANCE
        else -> IntentType.UNKNOWN
    }

    /**
     * Extract patient name from text.
     *
     * Returns the first word that is not a stop word and has length ≥ 3.
     * Returns null if no such word found.
     */
    fun extractPatientName(text: String): String? {
        val words = text.split("\\s+".toRegex())
        return words.firstOrNull { word ->
            word.length >= 3 && !isStopWord(word)
        }?.replaceFirstChar { it.uppercase() }
    }

    /**
     * Extract village name from text, using optional known [villages] list.
     *
     * First tries to match against provided [villages] (English & Hindi names).
     * Falls back to a small hardcoded map of common village names.
     */
    fun extractVillageName(
        text: String,
        villages: List<com.villageclinicledger.data.models.Village>? = null
    ): String? {
        val words = text.split("\\s+".toRegex())

        // Try user-configured villages first
        villages?.let { villageList ->
            for (word in words) {
                val cleanWord = word.lowercase(Locale.getDefault())
                for (v in villageList) {
                    val engKey = v.name.lowercase(Locale.getDefault())
                    val hindiKey = v.nameHindi.lowercase(Locale.getDefault())
                    if (cleanWord.contains(engKey) ||
                        (hindiKey.isNotEmpty() && cleanWord.contains(hindiKey))
                    ) {
                        return v.name
                    }
                }
            }
        }

        // Fallback: hardcoded village map (lowercase key → proper-case value)
        val villageMap = mapOf(
            "siras" to "Siras",
            "mehtabpura" to "Mehtabpura",
            "jhilai" to "Jhilai",
            "bassi" to "Bassi",
            "shyosinghpura" to "Shyosinghpura",
            "mandaliya" to "Mandaliya",
            "nala" to "Nala",
            "piplya" to "Piplya"
        )

        return words.firstOrNull { word ->
            val cleanWord = word.lowercase(Locale.getDefault())
            villageMap.any { (key, _) -> cleanWord.contains(key) }
        }?.let { word ->
            val cleanWord = word.lowercase(Locale.getDefault())
            villageMap.entries.firstOrNull { (key, _) -> cleanWord.contains(key) }?.value
        }
    }

    /**
     * Extract medicine and payment amounts from text.
     *
     * Returns [Pair<medicineAmount?, paymentAmount?>].
     * Logic:
     * - If no payment trigger word present: first number = medicine, second = payment
     * - If payment trigger word present AND only one number: that number = payment
     * - If payment trigger word present AND two+ numbers: first = medicine, second = payment
     * - Hindi number words (via [HindiNumberConverter]) are treated as additional numbers
     */
    fun extractAmounts(text: String): Pair<Double?, Double?> =
        extractAmounts(text, IntentType.UNKNOWN)

    // ──────────────────────────────────────────────────────────────────────
    // Internal implementation
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Normalize text for parsing: lowercase, collapse whitespace, strip punctuation.
     */
    private fun normalize(text: String): String =
        text.lowercase(Locale.getDefault()).trim()
            .replace("[.,!?]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

    // Keyword sets for intent detection
    private val searchKeywords = setOf(
        "kitna", "baki", "baki hai", "hisaab", "kita", "dikhao", "khata", "balance",
        "due", "how much", "का", "बकाया", "कितना", "हिसाब", "दिखाओ", "खाता"
    )

    private val medicineKeywords = setOf(
        "dawa", "dawai", "dava", "davai", "tablet", "syrup", "injection", "medicine",
        "di", "diya", "diye", "दवा", "दवाई", "दिया", "दी", "दिये", "दवा दी"
    )

    private val paymentKeywords = setOf(
        "diye", "diya", "jama", "de gaya", "de diye", "paid", "payment",
        "paisa diya", "paisa diye", "दिये", "दिए", "जमा", "दे गए", "पैसा दिया",
        "jama kar diye"
    )

    private val newPatientKeywords = setOf(
        "naya", "nayi", "naya patient", "nayi patient", "naya bimaar",
        "pehli baar", "new patient", "नया", "नयी", "नया रोगी", "पहली बार"
    )

    private val correctionKeywords = setOf(
        "galat", "galat ho gaya", "hata do", "hatao", "kat do", "kato",
        "sudhar", "wrong", "cancel", "undo", "hata den", "delete karo",
        "गलत", "हटा दो", "हटाओ", "काट दो", "सुधार"
    )

    private val yesKeywords = setOf(
        "haan", "ha", "hmm", "sahi", "sahi hai", "theek", "theek hai",
        "ok", "yes", "correct", "ठीक", "सही है", "हाँ", "हां", "theek hai"
    )

    private val noKeywords = setOf(
        "nahi", "nhi", "galat", "galat hai", "badlo", "badal do",
        "no", "wrong", "dobara", "phir se", "नहीं", "नही", "बदलो", "फिर से",
        "galat hai"
    )

    private fun isSearchBalance(text: String): Boolean = searchKeywords.any { text.contains(it) }
    private fun isMedicine(text: String): Boolean = medicineKeywords.any { text.contains(it) }
    private fun isPayment(text: String): Boolean = paymentKeywords.any { text.contains(it) }
    private fun isNewPatient(text: String): Boolean = newPatientKeywords.any { text.contains(it) }
    private fun isCorrection(text: String): Boolean = correctionKeywords.any { text.contains(it) }
    private fun isConfirmYes(text: String): Boolean = yesKeywords.any { text.contains(it) }
    private fun isConfirmNo(text: String): Boolean = noKeywords.any { text.contains(it) }

    /**
     * Internal overload that uses pre-detected [intent] to avoid double-detection.
     */
    private fun extractAmounts(text: String, intent: IntentType): Pair<Double?, Double?> {
        val paymentTriggers = paymentKeywords
        val hasPaymentTrigger = paymentTriggers.any { text.contains(it) }

        // Extract all digit sequences
        val digitAmounts = mutableListOf<Double>()
        for (match in "\\b(\\d+)\\b".toRegex().findAll(text)) {
            match.value.toDoubleOrNull()?.let { digitAmounts.add(it) }
        }

        // Extract Hindi number words as a single additional amount
        val hindiAmount = HindiNumberConverter.parseHindiNumber(text)

        // Combine: digit amounts first, then Hindi amount if present AND not a duplicate
        val allAmounts = mutableListOf<Double>().apply {
            addAll(digitAmounts)
            if (hindiAmount != null && !digitAmounts.contains(hindiAmount)) {
                add(hindiAmount)
            }
        }

        // No amounts found
        if (allAmounts.isEmpty()) {
            return Pair(null, null)
        }

        // Single amount
        if (allAmounts.size == 1) {
            return if (hasPaymentTrigger || intent == IntentType.PAYMENT) {
                Pair(null, allAmounts[0])
            } else {
                Pair(allAmounts[0], null)
            }
        }

        // Two or more amounts: first = medicine, second = payment
        return Pair(allAmounts[0], allAmounts[1])
    }

    /**
     * Words that should not be treated as patient names.
     * Includes grammatical particles, common verbs, and domain-specific stop words.
     */
    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
            // Hindi/English grammatical particles
            "kitna", "baki", "hai", "ko", "ne", "ka", "ki", "ke",
            "se", "mein", "me", "par", "aur", "the", "a", "an",
            "to", "for", "in", "on", "at", "by", "is", "are",

            // Domain verbs & nouns that appear in voice commands
            "dawa", "dawai", "di", "diya", "diye", "rupaye", "rupay",
            "paise", "sau", "hazaar", "lakh", "naya", "nayi",
            "galat", "hata", "nahi", "haan", "sahi", "jama", "kar",
            "dava", "davai", "tablet", "syrup", "injection", "medicine",
            "payment", "paisa", "paise", "rupee", "rupees", "baki",
            "hisaab", "khata", "balance", "due", "how", "much",
            "hatao", "hataen", "hataon", "karne", "karo", "do",
            "batao", "bataao", "dikhao", "dikhaao",

            // Devanagari equivalents
            "दवा", "दवाई", "दिया", "दी", "दिये", "रुपए", "रुपये", "पैसे",
            "सौ", "हज़ार", "लाख", "नया", "नयी", "गलत", "हटा", "नहीं",
            "हाँ", "हां", "जमा", "कर", "कितना", "बकाया", "हिसाब", "खाता",
            "बैलेंस", "को", "ने", "का", "की", "के", "से", "में", "और", "पर",
            "हटाओ", "हटाएं", "करो", "दो", "बताओ", "दिखाओ"
        )

        return word.length < 2
            || word.all { it.isDigit() }
            || stopWords.contains(word)
    }
}