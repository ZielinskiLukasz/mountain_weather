package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
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

    const val MAX_HOURS = 8

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val hourLabelFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun appWidgetHostWidthDp(context: Context, glanceId: GlanceId): Float? {
        return try {
            val appCtx = context.applicationContext
            val widgetId = GlanceAppWidgetManager(appCtx).getAppWidgetId(glanceId)
            val options = AppWidgetManager.getInstance(appCtx).getAppWidgetOptions(widgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            if (width > 0) width.toFloat() else null
        } catch (_: Exception) {
            null
        }
    }

    fun resolveLayoutWidthDp(context: Context, glanceId: GlanceId, localWidthDp: Float): Float {
        val host = appWidgetHostWidthDp(context, glanceId) ?: return localWidthDp
        return minOf(localWidthDp, host)
    }

    /** Hour columns for launcher width (4×1 → 6h, 5×1 → 7h, wider → 8h). */
    fun visibleHourCount(widthDp: Float): Int = when {
        widthDp < 325f -> 6
        widthDp < 422f -> 7
        else -> 8
    }

    fun filterHoursForWidget(hours: List<HourlyHourSnapshot>): List<HourlyHourSnapshot> {
        if (hours.isEmpty()) return emptyList()
        val now = LocalDateTime.now()
        val today = LocalDate.now().toString()
        val todayHours = hours.filter { it.time.startsWith(today) }
        if (todayHours.isEmpty()) return hours.take(MAX_HOURS)

        val startIdx = todayHours.indexOfFirst { entry ->
            runCatching {
                LocalDateTime.parse(entry.time, isoFormatter).hour >= now.hour
            }.getOrDefault(false)
        }.let { if (it < 0) 0 else it }

        return todayHours.drop(startIdx).take(MAX_HOURS)
    }

    fun formatHourLabel(timeIso: String): String = runCatching {
        LocalDateTime.parse(timeIso, isoFormatter).format(hourLabelFormatter)
    }.getOrElse { timeIso.takeLast(5) }

    /** Shrink hour label so full HH:mm fits the column without ellipsis. */
    fun hourLabelSp(label: String, columnWidthDp: Float, preferredSp: Int): Int {
        if (columnWidthDp <= 0f) return preferredSp
        val floor = 8
        for (sp in preferredSp downTo floor) {
            if (label.length * sp * 0.46f <= columnWidthDp) return sp
        }
        return floor
    }
}
