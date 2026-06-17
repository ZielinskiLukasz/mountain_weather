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

    fun filterHoursForWidget(hours: List<HourlyHourSnapshot>): List<HourlyHourSnapshot> =
        filterHoursForWidget(hours, LocalDateTime.now())

    /**
     * Pick a [MAX_HOURS]-wide window of hours around [now]:
     * - normally: current hour + future hours (today, then spilling into tomorrow if needed),
     * - late in the day (when fewer than [MAX_HOURS] future hours remain in the cache):
     *   pad with earlier hours so the strip is always full and current is on the right.
     */
    fun filterHoursForWidget(
        hours: List<HourlyHourSnapshot>,
        now: LocalDateTime
    ): List<HourlyHourSnapshot> {
        if (hours.isEmpty()) return emptyList()

        val sorted = hours.sortedBy { it.time }
        val hourFloor = now.withMinute(0).withSecond(0).withNano(0)
        val futureStartIdx = sorted.indexOfFirst { entry ->
            val parsed = runCatching {
                LocalDateTime.parse(entry.time, isoFormatter)
            }.getOrNull()
            parsed != null && !parsed.isBefore(hourFloor)
        }

        if (futureStartIdx < 0) {
            return sorted.takeLast(MAX_HOURS)
        }

        val futureCount = sorted.size - futureStartIdx
        if (futureCount >= MAX_HOURS) {
            return sorted.subList(futureStartIdx, futureStartIdx + MAX_HOURS)
        }

        val start = (sorted.size - MAX_HOURS).coerceAtLeast(0)
        return sorted.subList(start, sorted.size)
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
