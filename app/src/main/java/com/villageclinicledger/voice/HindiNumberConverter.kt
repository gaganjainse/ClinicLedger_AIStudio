package com.villageclinicledger.voice

object HindiNumberConverter {

    private val ones = arrayOf(
        "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ",
        "दस", "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस"
    )
    private val tens = arrayOf(
        "", "", "बीस", "तीस", "चालीस", "पचास", "साठ", "सत्तर", "अस्सी", "नब्बे"
    )
    private val specialTens = mapOf(
        2 to "बीस", 3 to "तीस", 4 to "चालीस", 5 to "पचास",
        6 to "साठ", 7 to "सत्तर", 8 to "अस्सी", 9 to "नब्बे"
    )

    fun convert(amount: Double): String {
        val whole = amount.toLong()
        val paise = ((amount - whole) * 100).toLong()
        val rupees = if (whole > 0) convertWhole(whole) + " रुपये" else "शून्य रुपये"
        return if (paise > 0) "$rupees ${paise} पैसे" else rupees
    }

    fun convertShort(amount: Double): String {
        val whole = amount.toLong()
        if (whole == 0L) return "शून्य"
        val text = when {
            whole == 0L -> "शून्य"
            whole <= 19 -> ones[whole.toInt()]
            whole <= 99 -> {
                val t = (whole / 10).toInt()
                val o = (whole % 10).toInt()
                if (o == 0) tens[t] else tens[t] + " " + ones[o]
            }
            whole <= 199 -> "एक सौ " + if (whole % 100 > 0) convertUnder100((whole % 100).toInt()) + " " else ""
            whole <= 999 -> {
                val h = (whole / 100).toInt()
                val r = whole % 100
                ones[h] + " सौ " + if (r > 0) convertUnder100(r.toInt()) + " " else ""
            }
            whole <= 1999 -> {
                val rem = whole % 1000
                "एक हज़ार " + if (rem > 0) convertUnder999(rem.toInt()) + " " else ""
            }
            whole <= 99999 -> {
                val t = (whole / 1000).toInt()
                val r = whole % 1000
                convertUnder99(t) + " हज़ार " + if (r > 0) convertUnder999(r.toInt()) + " " else ""
            }
            whole <= 9999999 -> {
                val l = (whole / 100000).toInt()
                val r = whole % 100000
                convertUnder99(l) + " लाख " + if (r > 0) convertUnder99999(r.toInt()) + " " else ""
            }
            else -> {
                val c = (whole / 10000000).toInt()
                val r = whole % 10000000
                convertUnder99(c) + " करोड़ " + if (r > 0) convertUnder9999999(r.toInt()) + " " else ""
            }
        }
        return text.trim()
    }

    fun parseHindiNumber(text: String): Double? {
        val clean = text.lowercase().trim()
            .replace("रुपये", "").replace("rupaye", "").replace("paise", "").replace("पैसे", "")
            .replace(",", "").trim()
        val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        var total = 0.0
        var current = 0.0
        for (w in words) {
            when (w) {
                "एक", "ek", "eak" -> current += 1
                "दो", "do", "dho" -> current += 2
                "तीन", "teen", "tin" -> current += 3
                "चार", "char", "caar" -> current += 4
                "पाँच", "paanch", "paanch", "panch" -> current += 5
                "छह", "chhah", "che", "chhah", "chhe" -> current += 6
                "सात", "saat", "sat" -> current += 7
                "आठ", "aath", "ath" -> current += 8
                "नौ", "nau", "nau" -> current += 9
                "दस", "das", "dus" -> current += 10
                "ग्यारह", "gyaarah", "gyarah" -> current += 11
                "बारह", "baarah", "barah" -> current += 12
                "तेरह", "terah" -> current += 13
                "चौदह", "chaudah" -> current += 14
                "पंद्रह", "pandrah", "pondrah" -> current += 15
                "सोलह", "solah" -> current += 16
                "सत्रह", "satrah" -> current += 17
                "अठारह", "athaarah", "atharah" -> current += 18
                "उन्नीस", "unnees" -> current += 19
                "बीस", "bees", "bis" -> current += 20
                "तीस", "tees", "tis" -> current += 30
                "चालीस", "chaalees", "chalees" -> current += 40
                "पचास", "pachaas", "pachas" -> current += 50
                "साठ", "saath", "saath" -> current += 60
                "सत्तर", "sattar" -> current += 70
                "अस्सी", "assee", "asee" -> current += 80
                "नब्बे", "nabbe" -> current += 90
                "सौ", "sau", "sou" -> {
                    current = maxOf(current, 1.0) * 100
                }
                "हज़ार", "hazaar", "hazar", "hajaar", "hazarr" -> {
                    current = maxOf(current, 1.0) * 1000
                }
                "लाख", "lakh", "laakh" -> {
                    current = maxOf(current, 1.0) * 100000
                }
                "करोड़", "crore", "karod" -> {
                    current = maxOf(current, 1.0) * 10000000
                }
                "दर्जन", "dozen" -> current += 12
                "सवा" -> current += 0.25
                "आधा", "adha", "aadha" -> current += 0.5
                "पाव", "paav", "pao" -> current += 0.25
                "डेढ़", "dedh", "derh" -> current += 1.5
                "साढ़े", "sadhe", "saadhe" -> {
                    current = current * 1.0 + 0.5
                }
                "ढाई", "dhai", "dhaai" -> current += 2.5
                "पौने" -> current += 0.75
                else -> {
                    val num = w.toDoubleOrNull()
                    if (num != null) current += num
                }
            }
        }
        total += current
        return if (total > 0) total else null
    }

    private fun convertWhole(n: Long): String {
        return when {
            n == 0L -> "शून्य"
            n <= 19 -> ones[n.toInt()]
            n <= 99 -> convertUnder100(n.toInt())
            n <= 199 -> "एक सौ " + if (n % 100 > 0) convertUnder100((n % 100).toInt()) else ""
            n <= 999 -> ones[(n / 100).toInt()] + " सौ " + (if (n % 100 > 0) convertUnder100((n % 100).toInt()) else "")
            n <= 1999 -> "एक हज़ार " + (if (n % 1000 > 0) convertUnder999((n % 1000).toInt()) else "")
            n <= 99999 -> convertUnder99((n / 1000).toInt()) + " हज़ार " + (if (n % 1000 > 0) convertUnder999((n % 1000).toInt()) else "")
            n <= 9999999 -> convertUnder99((n / 100000).toInt()) + " लाख " + (if (n % 100000 > 0) convertUnder99999((n % 100000).toInt()) else "")
            else -> convertUnder99((n / 10000000).toInt()) + " करोड़ " + (if (n % 10000000 > 0) convertUnder9999999((n % 10000000).toInt()) else "")
        }.trimEnd()
    }

    private fun convertUnder100(n: Int): String = when {
        n <= 19 -> ones[n]
        n % 10 == 0 -> tens[n / 10]
        else -> tens[n / 10] + " " + ones[n % 10]
    }

    private fun convertUnder99(n: Int): String = convertUnder100(n)

    private fun convertUnder999(n: Int): String = when {
        n <= 99 -> convertUnder100(n)
        else -> ones[n / 100] + " सौ " + (if (n % 100 > 0) convertUnder100(n % 100) else "")
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
