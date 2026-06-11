package com.ergonomic.mountainweather.widget

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface HourlyWidgetData {
    data object NoFavorites : HourlyWidgetData
    data class NoData(val cityName: String?, val hourlyDisabled: Boolean = false) : HourlyWidgetData
    data class Ready(
        val cityName: String,
        val currentTemp: Double?,
        val currentWeatherCode: Int?,
        val currentIsDay: Boolean,
        val hours: List<HourlyHourSnapshot>
    ) : HourlyWidgetData
}

data class HourlyHourSnapshot(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val precipitation: Double
)

object WidgetHourlyWindow {

    /** Hours shown on the fixed 4×1 strip. */
    const val VISIBLE_HOURS = 6

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val hourLabelFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun filterHoursForWidget(hours: List<HourlyHourSnapshot>): List<HourlyHourSnapshot> {
        if (hours.isEmpty()) return emptyList()
        val now = LocalDateTime.now()
        val today = LocalDate.now().toString()
        val todayHours = hours.filter { it.time.startsWith(today) }
        if (todayHours.isEmpty()) return hours.take(VISIBLE_HOURS)

        val startIdx = todayHours.indexOfFirst { entry ->
            runCatching {
                LocalDateTime.parse(entry.time, isoFormatter).hour >= now.hour
            }.getOrDefault(false)
        }.let { if (it < 0) 0 else it }

        return todayHours.drop(startIdx).take(VISIBLE_HOURS)
    }

    fun formatHourLabel(timeIso: String): String = runCatching {
        LocalDateTime.parse(timeIso, isoFormatter).format(hourLabelFormatter)
    }.getOrElse { timeIso.takeLast(5) }
}
