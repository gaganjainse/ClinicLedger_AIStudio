package com.villageclinicledger.voice

import java.util.Locale

data class ParsedVoiceIntent(
    val intent: IntentType,
    val patientName: String? = null,
    val villageName: String? = null,
    val medicineAmount: Double? = null,
    val paymentAmount: Double? = null,
    val confidence: Float = 0f,
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
        val patientName = extractPatientName(clean, villages)
        val villageName = extractVillageName(clean, villages)
        
        val wordsList = clean.split("\\s+".toRegex())
        val groups = findNumberGroups(clean)
        val (medicineAmount, paymentAmount) = classifyGroups(groups, wordsList, intent)

        return ParsedVoiceIntent(
            intent = intent,
            patientName = patientName,
            villageName = villageName,
            medicineAmount = medicineAmount,
            paymentAmount = paymentAmount,
            confidence = if (intent != IntentType.UNKNOWN) 0.9f else 0.2f
        )
    }

    fun detectIntent(text: String): IntentType {
        if (isCorrection(text)) return IntentType.CORRECTION
        if (isConfirmNo(text)) return IntentType.CONFIRM_NO
        if (isConfirmYes(text)) return IntentType.CONFIRM_YES
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
        "di", "दवा", "दवाई", "दी"
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
        "ok", "yes", "correct", "ठीक", "सही है", "हाँ", "हां"
    )

    private val noKeywords = setOf(
        "nahi", "nhi", "galat", "galat hai", "badlo", "badal do",
        "no", "wrong", "dobara", "phir se", "नहीं", "नही", "बदलो", "फिर से"
    )

    private val medicineKeywordsNear = setOf(
        "dawa", "dawai", "dava", "davai", "tablet", "syrup", "injection", "medicine", "udhaar", "udhar",
        "दवा", "दवाई", "उधार"
    )

    private val paymentKeywordsNear = setOf(
        "jama", "paid", "payment", "paisa", "paise", "rupee", "rupees", "rupaye", "rupay", "de", "diye", "diya",
        "जमा", "पैसा", "पैसे", "रुपए", "रुपये", "दिए", "दिये"
    )

    private fun containsWordOrPhrase(text: String, keyword: String): Boolean {
        val pattern = "(?:^|\\s)${Regex.escape(keyword)}(?:$|\\s)".toRegex()
        return pattern.containsMatchIn(text)
    }

    private fun isSearchBalance(text: String): Boolean =
        searchKeywords.any { containsWordOrPhrase(text, it) }

    private fun isMedicine(text: String): Boolean =
        medicineKeywords.any { containsWordOrPhrase(text, it) }

    private fun isPayment(text: String): Boolean =
        paymentKeywords.any { containsWordOrPhrase(text, it) }

    private fun isNewPatient(text: String): Boolean =
        newPatientKeywords.any { containsWordOrPhrase(text, it) }

    private fun isCorrection(text: String): Boolean =
        correctionKeywords.any { containsWordOrPhrase(text, it) }

    private fun isConfirmYes(text: String): Boolean =
        yesKeywords.any { keyword ->
            val pattern = "\\b${Regex.escape(keyword)}\\b".toRegex()
            pattern.containsMatchIn(text)
        }

    private fun isConfirmNo(text: String): Boolean =
        noKeywords.any { keyword ->
            val pattern = "\\b${Regex.escape(keyword)}\\b".toRegex()
            pattern.containsMatchIn(text)
        }

    fun extractPatientName(text: String, villages: List<com.villageclinicledger.data.models.Village>? = null): String? {
        val clean = text.lowercase(Locale.getDefault()).trim()
        
        // Let's identify the name using trigger particles and common delimiters
        val delimiters = listOf(
            " ko ", " ne ", " ka ", " se ", " ki ", " ke ", 
            " kitna ", " baki ", " hisaab ", " khata ", " balance ", " due ", 
            " dawa ", " dawai ", " dava ", " tablet ", " medicine ", " jama ", " paid ", " payment ",
            " को ", " ने ", " का ", " की ", " के ", " से ", " कितना ", " बकाया ", " हिसाब ", " खाता ", " दवा ", " जमा "
        )
        
        var nameCandidate: String? = null
        for (delim in delimiters) {
            val idx = clean.indexOf(delim)
            if (idx > 0) {
                val prefix = clean.substring(0, idx).trim()
                // Clean up leading keywords like "naya patient"
                var cleanedPrefix = prefix
                val removePrefixes = listOf(
                    "naya patient ", "nayi patient ", "naya bimaar ", "pehli baar ", "new patient ", "patient ",
                    "नया रोगी ", "नया ", "नयी ", "पहली बार ", "रोगी "
                )
                for (p in removePrefixes) {
                    if (cleanedPrefix.startsWith(p)) {
                        cleanedPrefix = cleanedPrefix.substring(p.length).trim()
                    }
                }
                
                // Split prefix into words and make sure they are not numbers or purely stop words or villages
                val words = cleanedPrefix.split("\\s+".toRegex()).filter { w ->
                    val isVil = if (villages != null) {
                        villages.any { v ->
                            (v.name.lowercase() == w) || (v.nameHindi.lowercase() == w)
                        }
                    } else {
                        val villageKeys = setOf(
                            "siras", "mehtabpura", "jhilai", "bassi", "shyosinghpura", "mandaliya", "nala", "piplya"
                        )
                        villageKeys.contains(w)
                    }
                    w.length >= 2 && !w.all { it.isDigit() } && !isHindiNumberWord(w) && !isVil && !isStopWord(w)
                }
                
                if (words.isNotEmpty()) {
                    nameCandidate = words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    break
                }
            }
        }
        
        // Fallback to original sliding-window token strategy
        if (nameCandidate == null) {
            val words = text.split("\\s+".toRegex())
            val nameWords = mutableListOf<String>()
            var foundStart = false

            for (word in words) {
                val cleanWord = word.lowercase(Locale.getDefault())

                // Check if word is a village
                val isVillage = if (villages != null) {
                    villages.any { v ->
                        val engKey = v.name.lowercase()
                        val hindiKey = v.nameHindi.lowercase()
                        cleanWord == engKey || cleanWord == hindiKey
                    }
                } else {
                    val villageKeys = setOf(
                        "siras", "mehtabpura", "jhilai", "bassi", "shyosinghpura", "mandaliya", "nala", "piplya"
                    )
                    villageKeys.contains(cleanWord)
                }

                val isValidNameWord = word.length >= 2 &&
                        !word.all { it.isDigit() } &&
                        !isStopWord(cleanWord) &&
                        !isVillage &&
                        !isHindiNumberWord(cleanWord)

                if (isValidNameWord) {
                    foundStart = true
                    nameWords.add(word.replaceFirstChar { it.uppercase() })
                } else {
                    if (foundStart) {
                        break
                    }
                }
            }
            if (nameWords.isNotEmpty()) {
                nameCandidate = nameWords.joinToString(" ")
            }
        }
        
        return nameCandidate
    }

    private fun isHindiNumberWord(word: String): Boolean {
        val hindiNumWords = setOf(
            "ek", "do", "teen", "char", "paanch", "panch", "chheh", "saat", "aath", "nau", "das",
            "dedh", "dhai", "adhai", "sava", "sade", "paun", "paune", "sau", "hazaar", "hazar",
            "एक", "दो", "तीन", "चार", "पांच", "छह", "सात", "आठ", "नौ", "दस", "डेढ़", "ढाई", "सौ", "हजार"
        )
        return hindiNumWords.contains(word)
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

    data class NumberGroup(val value: Double, val startIndex: Int, val endIndex: Int)

    fun findNumberGroups(cleanText: String): List<NumberGroup> {
        val words = cleanText.split("\\s+".toRegex())
        val groups = mutableListOf<NumberGroup>()
        val currentGroupWords = mutableListOf<String>()
        var startIdx = -1

        for (i in words.indices) {
            val word = words[i]
            val isNum = word.all { it.isDigit() } || isHindiNumberWord(word)
            if (isNum) {
                if (currentGroupWords.isEmpty()) {
                    startIdx = i
                }
                currentGroupWords.add(word)
            } else {
                if (currentGroupWords.isNotEmpty()) {
                    val groupStr = currentGroupWords.joinToString(" ")
                    parseGroupString(groupStr)?.let {
                        groups.add(NumberGroup(it, startIdx, i - 1))
                    }
                    currentGroupWords.clear()
                }
            }
        }
        if (currentGroupWords.isNotEmpty()) {
            val groupStr = currentGroupWords.joinToString(" ")
            val parsed = parseGroupString(groupStr)
            if (parsed != null) {
                groups.add(NumberGroup(parsed, startIdx, words.size - 1))
            }
        }
        return groups
    }

    private fun parseGroupString(str: String): Double? {
        if (str.all { it.isDigit() }) {
            return str.toDoubleOrNull()
        }
        return HindiNumberConverter.parseHindiNumber(str)
    }

    fun classifyGroups(groups: List<NumberGroup>, words: List<String>, intent: IntentType): Pair<Double?, Double?> {
        if (groups.isEmpty()) return Pair(null, null)

        if (groups.size == 1) {
            val val0 = groups[0].value
            return if (intent == IntentType.PAYMENT) {
                Pair(null, val0)
            } else {
                Pair(val0, null)
            }
        }

        var medicineAmount: Double? = null
        var paymentAmount: Double? = null

        val scoredGroups = groups.map { group ->
            var medScore = 0
            var payScore = 0

            val startLook = maxOf(0, group.startIndex - 4)
            val endLook = minOf(words.size - 1, group.endIndex + 4)

            for (i in startLook..endLook) {
                if (i in group.startIndex..group.endIndex) continue
                val w = words[i].lowercase(Locale.getDefault())
                if (medicineKeywordsNear.contains(w)) medScore += 2
                if (paymentKeywordsNear.contains(w)) payScore += 2
            }

            if (group.startIndex < words.size / 2) {
                medScore += 1
            } else {
                payScore += 1
            }

            group to (medScore to payScore)
        }

        val sortedByMed = scoredGroups.sortedByDescending { it.second.first }
        if (sortedByMed.isNotEmpty()) {
            medicineAmount = sortedByMed[0].first.value
        }

        val sortedByPay = scoredGroups.sortedByDescending { it.second.second }
        if (sortedByPay.isNotEmpty()) {
            val bestPayGroup = sortedByPay.firstOrNull { it.first != sortedByMed.firstOrNull()?.first }
            if (bestPayGroup != null) {
                paymentAmount = bestPayGroup.first.value
            } else if (groups.size >= 2) {
                paymentAmount = groups.firstOrNull { it != sortedByMed[0].first }?.value
            }
        }

        return Pair(medicineAmount, paymentAmount)
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
            "batao", "bataao", "dikhao", "dikhaao", "se",
            "patient", "patients", "bimaar", "marij", "marija", "meeriz", "sick",
            "दवा", "दवाई", "दिया", "दी", "दिये", "रुपए", "रुपये", "पैसे",
            "सौ", "हज़ार", "लाख", "नया", "नयी", "गलत", "हटा", "नहीं",
            "हाँ", "हां", "जमा", "कर", "कितना", "बकाया", "हिसाब", "खाता",
            "बैलेंस", "को", "ने", "का", "की", "के", "से", "में", "और", "पर",
            "हटाओ", "हटाएं", "करो", "दो", "बताओ", "दिखाओ", "मरीज", "मरीज़", "रोगी", "रोगीजी"
        )
        return word.length < 2 || word.all { it.isDigit() } || stopWords.contains(word)
    }
}
