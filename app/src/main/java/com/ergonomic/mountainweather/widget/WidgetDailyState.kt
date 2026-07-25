package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import java.time.LocalDate

/** Glance state key: first visible day index in the forecast strip (tap scrolls forward). */
object WidgetDailyKeys {
    val DAY_INDEX = intPreferencesKey("day_index")
}

sealed interface DailyWidgetData {
    data object NoFavorites : DailyWidgetData
    data class NoData(val cityName: String?) : DailyWidgetData
    data class Ready(
        val cityName: String,
        val days: List<DailyDaySnapshot>
    ) : DailyWidgetData
}

data class DailyDaySnapshot(
    val date: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    /** Current-hour weather code override for today (matches main screen). */
    val currentWeatherCode: Int? = null,
    /** Current temperature override for today (matches main screen). */
    val currentTemp: Double? = null,
    /** Whether it is currently day (used for icon selection when overriding). */
    val currentIsDay: Boolean = true
)

/** Windowing and scroll math shared by [WeatherDailyWidget] and [CycleDailyAction]. */
object WidgetDailyWindow {

    private const val MAX_COLUMNS = 16

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

    /**
     * Width used to count grid columns — the narrower of Glance size and host options.
     * Host width matches launcher cells; LocalSize can be 2× in landscape compositions.
     */
    fun resolveLayoutWidthDp(context: Context, glanceId: GlanceId, localWidthDp: Float): Float {
        val host = appWidgetHostWidthDp(context, glanceId) ?: return localWidthDp
        return minOf(localWidthDp, host)
    }

    /**
     * Days shown side-by-side: 1×1 → 1, 1×2 → 2, 1×3 → 3, …
     *
     * Thresholds from measured launcher sizes (e.g. emulator: 82 / 179 / 276 / 373 dp).
     */
    fun visibleColumnCount(widthDp: Float): Int {
        if (widthDp <= 0f) return 1
        return when {
            widthDp < 95f -> 1
            widthDp < 200f -> 2
            widthDp < 325f -> 3
            widthDp < 422f -> 4
            widthDp < 519f -> 5
            widthDp < 616f -> 6
            widthDp < 713f -> 7
            widthDp < 810f -> 8
            else -> ((widthDp + 15f) / 97f).toInt().coerceIn(1, MAX_COLUMNS)
        }
    }

    fun maxStartIndex(dayCount: Int, columns: Int): Int =
        (dayCount - columns).coerceAtLeast(0)

    fun coerceStartIndex(startIndex: Int, dayCount: Int, columns: Int): Int =
        startIndex.coerceIn(0, maxStartIndex(dayCount, columns))

    fun nextStartIndex(current: Int, dayCount: Int, columns: Int): Int {
        val max = maxStartIndex(dayCount, columns)
        if (max == 0) return 0
        return (current + 1) % (max + 1)
    }

    fun visibleDays(days: List<DailyDaySnapshot>, startIndex: Int, columns: Int): List<DailyDaySnapshot> {
        if (days.isEmpty()) return emptyList()
        val start = coerceStartIndex(startIndex, days.size, columns)
        return days.drop(start).take(columns)
    }

    /** Today plus up to [dailyForecastDays] future days (matches app forecast setting). */
    fun filterDaysForWidget(
        days: List<DailyDaySnapshot>,
        dailyForecastDays: Int
    ): List<DailyDaySnapshot> {
        if (dailyForecastDays <= 0) return emptyList()
        val today = LocalDate.now().toString()
        return days
            .filter { it.date >= today }
            .sortedBy { it.date }
            .take(dailyForecastDays + 1)
    }
}

/** Resolved view for one widget frame (selected day + optional neighbors). */
data class DailyDisplayState(
    val cityName: String,
    val selectedIndex: Int,
    val current: DailyDaySnapshot,
    val previous: DailyDaySnapshot?,
    val next: DailyDaySnapshot?
)

fun DailyWidgetData.Ready.toDisplay(dayIndex: Int): DailyDisplayState? {
    if (days.isEmpty()) return null
    val idx = dayIndex.coerceIn(0, days.lastIndex)
    return DailyDisplayState(
        cityName = cityName,
        selectedIndex = idx,
        current = days[idx],
        previous = days.getOrNull(idx - 1),
        next = days.getOrNull(idx + 1)
    )
}
