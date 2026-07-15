package com.villageclinicledger.ui.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

object LocaleManager {
    /**
     * CompositionLocal to provide the current language state (true for Hindi).
     */
    val LocalIsHindi = staticCompositionLocalOf { false }

    private const val PREFS_NAME = "clinic_ledger_prefs"
    private const val KEY_LANG = "selected_language"

    fun getSavedLocale(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "en") ?: "en"
    }

    fun saveLocale(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang).apply()
    }

    fun applyLocale(context: Context): Context {
        val lang = getSavedLocale(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLocaleLegacy(context: Context) {
        val lang = getSavedLocale(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Extracts the appropriate language substring from a bilingual string formatted as "English / Hindi".
     */
    fun getLocalizedText(text: String?): String {
        if (text == null) return ""
        if (!text.contains(" / ")) return text
        val parts = text.split(" / ")
        if (parts.size >= 2) {
            val isHindi = Locale.getDefault().language == "hi"
            return if (isHindi) parts[1].trim() else parts[0].trim()
        }
        return text
    }

    /**
     * Extracts localized text and formats it to Title Case (Aa Bb) if it contains English characters.
     */
    fun formatPatientName(text: String?): String {
        if (text == null) return ""
        val localized = getLocalizedText(text)
        if (localized.any { it.isLetter() }) {
            return localized.split(" ")
                .filter { it.isNotEmpty() }
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
        }
        return localized
    }

    /**
     * Localizes a village name using its English and Hindi fields.
     */
    fun getLocalizedVillage(name: String, nameHindi: String?): String {
        val isHindi = Locale.getDefault().language == "hi"
        return if (isHindi && !nameHindi.isNullOrBlank()) nameHindi else name
    }

    /**
     * Formats a double value as a string.
     * Shows decimals only if they are non-zero.
     */
    fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)
    }

    /**
     * Formats a double value as currency (Rupees) with localization.
     */
    fun formatCurrency(amount: Double): String {
        return "₹${formatAmount(amount)}"
    }

    /**
     * Returns a localized description for a transaction type.
     */
    fun getLocalizedTransactionType(type: String, isHindi: Boolean): String {
        return when (type) {
            "medicine" -> if (isHindi) "दवाई दी" else "Medicine Given"
            "payment" -> if (isHindi) "जमा किया" else "Payment Received"
            "adjustment" -> if (isHindi) "समायोजन" else "Balance Adjustment"
            else -> type
        }
    }

    /**
     * Formats a date relative to now (e.g., "5 days ago" or "5 दिन पहले").
     */
    fun formatRelativeDate(date: java.util.Date): String {
        val isHindi = Locale.getDefault().language == "hi"
        val diffMs = java.util.Date().time - date.time
        val days = diffMs / (1000 * 60 * 60 * 24)
        return if (isHindi) "$days दिन पहले" else "$days days ago"
    }
}
