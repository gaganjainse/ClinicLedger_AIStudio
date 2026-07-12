package com.villageclinicledger.voice

import java.util.Locale

data class ParsedVoiceIntent(
    val intent: IntentType,
    val patientName: String? = null,
    val villageName: String? = null,
    val medicineAmount: Double? = null,
    val paymentAmount: Double? = null,
    val confidence: Float = 0f
)

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

object VoiceIntentParser {

    fun parse(text: String, villages: List<com.villageclinicledger.data.models.Village>? = null): ParsedVoiceIntent {
        val clean = text.lowercase(Locale.getDefault()).trim()
            .replace("[.,!?]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ").trim()

        val intent = detectIntent(clean)
        val patientName = extractPatientName(clean)
        val villageName = extractVillageName(clean, villages)
        val amounts = extractAmounts(clean)
        val medicineAmount = if (intent == IntentType.MEDICINE || intent == IntentType.MEDICINE_AND_PAYMENT) amounts.first else null
        val paymentAmount = if (intent == IntentType.PAYMENT || intent == IntentType.MEDICINE_AND_PAYMENT) amounts.second else null

        return ParsedVoiceIntent(
            intent = intent,
            patientName = patientName,
            villageName = villageName,
            medicineAmount = medicineAmount,
            paymentAmount = paymentAmount,
            confidence = 0.7f
        )
    }

    fun detectIntent(text: String): IntentType {
        if (isConfirmYes(text)) return IntentType.CONFIRM_YES
        if (isConfirmNo(text)) return IntentType.CONFIRM_NO
        if (isCorrection(text)) return IntentType.CORRECTION
        if (isNewPatient(text)) return IntentType.NEW_PATIENT
        if (isMedicine(text) && isPayment(text)) return IntentType.MEDICINE_AND_PAYMENT
        if (isPayment(text)) return IntentType.PAYMENT
        if (isMedicine(text)) return IntentType.MEDICINE
        if (isSearchBalance(text)) return IntentType.SEARCH_BALANCE
        return IntentType.UNKNOWN
    }

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

    private fun isSearchBalance(text: String): Boolean =
        searchKeywords.any { text.contains(it) }

    private fun isMedicine(text: String): Boolean =
        medicineKeywords.any { text.contains(it) }

    private fun isPayment(text: String): Boolean =
        paymentKeywords.any { text.contains(it) }

    private fun isNewPatient(text: String): Boolean =
        newPatientKeywords.any { text.contains(it) }

    private fun isCorrection(text: String): Boolean =
        correctionKeywords.any { text.contains(it) }

    private fun isConfirmYes(text: String): Boolean =
        yesKeywords.any { text.contains(it) }

    private fun isConfirmNo(text: String): Boolean =
        noKeywords.any { text.contains(it) }

    fun extractPatientName(text: String): String? {
        val knownPrefixes = listOf(
            "ram", "ravi", "roshan", "raj", "shyam", "mohan", "sohan",
            "gopal", "kishan", "hari", "mangal", "om", "ramesh", "suresh",
            "dinesh", "mahesh", "rajesh", "kamlesh", "jagdish"
        )
        val words = text.split("\\s+".toRegex())
        for (word in words) {
            if (word.length >= 3 && !isStopWord(word)) {
                return word.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    fun extractVillageName(text: String, villages: List<com.villageclinicledger.data.models.Village>? = null): String? {
        val words = text.split("\\s+".toRegex())
        if (villages != null) {
            for (word in words) {
                val cleanWord = word.lowercase(Locale.getDefault())
                for (v in villages) {
                    val engKey = v.name.lowercase()
                    val hindiKey = v.nameHindi.lowercase()
                    if (cleanWord.contains(engKey) || (hindiKey.isNotEmpty() && cleanWord.contains(hindiKey))) {
                        return v.name
                    }
                }
            }
        }
        val villageMap = mapOf(
            "siras" to "Siras", "mehtabpura" to "Mehtabpura", "jhilai" to "Jhilai",
            "bassi" to "Bassi", "shyosinghpura" to "Shyosinghpura",
            "mandaliya" to "Mandaliya", "nala" to "Nala", "piplya" to "Piplya"
        )
        return words.firstOrNull { word ->
            val cleanWord = word.lowercase(Locale.getDefault())
            villageMap.any { (key, _) -> cleanWord.contains(key) }
        }?.let { word ->
            val cleanWord = word.lowercase(Locale.getDefault())
            villageMap.entries.firstOrNull { (key, _) -> cleanWord.contains(key) }?.value
        }
    }

    fun extractAmounts(text: String): Pair<Double?, Double?> {
        val amounts = mutableListOf<Double>()
        val paymentTriggers = listOf("diye", "diya", "jama", "de gaya", "de diye")
        val textLower = text.lowercase()

        val numberPattern = "\\b(\\d+)\\b".toRegex()
        for (match in numberPattern.findAll(textLower)) {
            match.value.toDoubleOrNull()?.let { amounts.add(it) }
        }

        val hindiAmount = HindiNumberConverter.parseHindiNumber(text)
        if (hindiAmount != null) amounts.add(hindiAmount)

        val afterDiye = textLower.substringAfter("diye ").trim()
        val afterJama = textLower.substringAfter("jama ").trim()
        val afterDiya = textLower.substringAfter("diya ").trim()

        var medicine = amounts.firstOrNull()
        var payment = amounts.getOrNull(1)

        if (paymentTriggers.any { textLower.contains(it) }) {
            if (amounts.isNotEmpty()) {
                val afterTrigger = try {
                    when {
                        textLower.contains("diye ") -> afterDiye
                        textLower.contains("jama ") -> afterJama
                        textLower.contains("diya ") -> afterDiya
                        else -> ""
                    }
                } catch (e: Exception) { "" }
                if (afterTrigger.isNotBlank()) {
                    val paymentMatch = numberPattern.find(afterTrigger)
                    if (paymentMatch != null) {
                        medicine = amounts.firstOrNull { it.toString() != paymentMatch.value }
                        payment = paymentMatch.value.toDoubleOrNull()
                    } else if (hindiAmount != null && amounts.size >= 2) {
                        medicine = amounts.getOrNull(0)
                        payment = amounts.getOrNull(1)
                    }
                } else if (amounts.size >= 2) {
                    medicine = amounts[0]
                    payment = amounts[1]
                }
            }
        }

        return Pair(medicine, payment)
    }

    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
            "kitna", "baki", "hai", "ko", "ne", "ka", "ki", "ke",
            "se", "mein", "me", "par", "aur", "the", "a", "an",
            "to", "for", "in", "on", "at", "by", "is", "are",
            "dawa", "dawai", "di", "diya", "diye", "rupaye", "rupay",
            "paise", "sau", "hazaar", "lakh", "naya", "nayi",
            "galat", "hata", "nahi", "haan", "sahi", "jama", "kar",
            "dava", "davai", "tablet", "syrup", "injection", "medicine",
            "payment", "paisa", "paise", "rupee", "rupees", "baki", "hisaab",
            "khata", "balance", "due", "how", "much", "hatao", "hataen",
            "hataon", "karne", "karo", "do", "diya", "diye", "di",
            "batao", "bataao", "dikhao", "dikhaao",
            "दवा", "दवाई", "दिया", "दी", "दिये", "रुपए", "रुपये", "पैसे",
            "सौ", "हज़ार", "लाख", "नया", "नयी", "गलत", "हटा", "नहीं",
            "हाँ", "हां", "जमा", "कर", "कितना", "बकाया", "हिसाब", "खाता",
            "बैलेंस", "को", "ने", "का", "की", "के", "से", "में", "और", "पर",
            "हटाओ", "हटाएं", "करो", "दो", "बताओ", "दिखाओ"
        )
        return word.length < 2 || word.all { it.isDigit() } || stopWords.contains(word)
    }
}
