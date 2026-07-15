package com.villageclinicledger.ui.util

import java.util.*

object DateTimeUtils {

    fun getStartOfDay(date: Date = Date()): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun getStartOfWeek(date: Date = Date()): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return getStartOfDay(cal.time)
    }

    fun getStartOfMonth(date: Date = Date()): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(cal.time)
    }

    fun getDaysAgo(days: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return cal.time
    }

    fun getLocalizedDayOfWeek(date: Date, isHindi: Boolean): String {
        val cal = Calendar.getInstance()
        cal.time = date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> if (isHindi) "रविवार / Sunday" else "Sunday"
            Calendar.MONDAY -> if (isHindi) "सोमवार / Monday" else "Monday"
            Calendar.TUESDAY -> if (isHindi) "मंगलवार / Tuesday" else "Tuesday"
            Calendar.WEDNESDAY -> if (isHindi) "बुधवार / Wednesday" else "Wednesday"
            Calendar.THURSDAY -> if (isHindi) "गुरुवार / Thursday" else "Thursday"
            Calendar.FRIDAY -> if (isHindi) "शुक्रवार / Friday" else "Friday"
            Calendar.SATURDAY -> if (isHindi) "शनिवार / Saturday" else "Saturday"
            else -> ""
        }
    }
}
