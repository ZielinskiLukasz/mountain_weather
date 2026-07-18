package com.ergonomic.mountainweather.widget

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * State for the Sun & UV widget: minimalist almanac showing sunrise / sunset
 * with a "now" marker on the day-progress bar plus the UV index for the day.
 */
sealed interface SunWidgetData {
    data object NoFavorites : SunWidgetData
    data class NoData(val cityName: String?) : SunWidgetData
    data class Ready(
        val cityName: String,
        val sunrise: LocalDateTime,
        val sunset: LocalDateTime,
        val now: LocalDateTime,
        val uvIndexMax: Double?,
        val weatherCode: Int? = null,
        val isDay: Boolean = true,
        val temperature: Double? = null
    ) : SunWidgetData
}

object WidgetSunWindow {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** Parse Open-Meteo sunrise/sunset ISO string; null on failure. */
    fun parseIso(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value, isoFormatter) }.getOrNull()
    }

    /**
     * Progress of `now` between [sunrise] and [sunset] as 0..1.
     * Before sunrise = 0, after sunset = 1. Zero-length day returns 0.
     */
    fun dayProgress(
        sunrise: LocalDateTime,
        sunset: LocalDateTime,
        now: LocalDateTime
    ): Float {
        if (!sunset.isAfter(sunrise)) return 0f
        val total = java.time.Duration.between(sunrise, sunset).seconds
        val elapsed = java.time.Duration.between(sunrise, now).seconds
        return (elapsed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** WHO UV categories with matching string res picked in the widget. */
    enum class UvCategory { Low, Moderate, High, VeryHigh, Extreme }

    fun categorize(uv: Double?): UvCategory? {
        val v = uv ?: return null
        return when {
            v < 3.0 -> UvCategory.Low
            v < 6.0 -> UvCategory.Moderate
            v < 8.0 -> UvCategory.High
            v < 11.0 -> UvCategory.VeryHigh
            else -> UvCategory.Extreme
        }
    }

    /** WHO-style semantic color for a given UV category. Kept constant across widget themes. */
    fun uvColorArgb(category: UvCategory): Int = when (category) {
        UvCategory.Low -> 0xFF3AB55D.toInt()      // green
        UvCategory.Moderate -> 0xFFF2C94C.toInt() // yellow
        UvCategory.High -> 0xFFF2994A.toInt()     // orange
        UvCategory.VeryHigh -> 0xFFEB5757.toInt() // red
        UvCategory.Extreme -> 0xFF9B51E0.toInt()  // violet
    }
}

object WidgetSunLayout {

    data class Spec(
        val citySp: Int,
        val timeSp: Int,
        val labelSp: Int,
        val uvSp: Int,
        val uvLabelSp: Int,
        val barHeightDp: Int,
        val dotSizeDp: Int,
        val uvDotDp: Int,
        val weatherIconDp: Int,
        val tempSp: Int,
        val dayLengthSp: Int,
        val showHero: Boolean
    )

    fun resolve(widthDp: Float, heightDp: Float): Spec {
        val tall = heightDp >= 110f
        val wide = widthDp >= 240f
        val extraTall = heightDp >= 160f
        return Spec(
            citySp = if (tall) 13 else 11,
            timeSp = if (wide) 16 else 14,
            labelSp = if (wide) 10 else 9,
            uvSp = if (tall) 22 else 18,
            uvLabelSp = if (tall) 12 else 11,
            barHeightDp = if (tall) 8 else 6,
            dotSizeDp = if (tall) 14 else 12,
            uvDotDp = if (tall) 14 else 12,
            weatherIconDp = if (extraTall) 56 else 44,
            tempSp = if (extraTall) 22 else 18,
            dayLengthSp = if (extraTall) 11 else 10,
            showHero = tall
        )
    }
}
