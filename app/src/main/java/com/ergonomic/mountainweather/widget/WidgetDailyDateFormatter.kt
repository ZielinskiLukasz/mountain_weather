package com.ergonomic.mountainweather.widget

import android.content.Context
import com.ergonomic.mountainweather.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WidgetDailyDateFormatter {

    fun format(context: Context, dateIso: String): String {
        return try {
            val date = LocalDate.parse(dateIso)
            val today = LocalDate.now()
            when (date) {
                today -> context.getString(R.string.widget_day_today)
                today.plusDays(1) -> context.getString(R.string.widget_day_tomorrow)
                else -> date.format(DateTimeFormatter.ofPattern("dd.MM"))
            }
        } catch (_: Exception) {
            dateIso.takeLast(5).replace('-', '.')
        }
    }
}
