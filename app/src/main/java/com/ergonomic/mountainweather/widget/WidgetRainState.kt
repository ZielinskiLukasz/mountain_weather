package com.ergonomic.mountainweather.widget

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * State and helpers for the Rain Bar widget (24h precipitation profile).
 * One-glance answer to "will it rain soon".
 */
sealed interface RainWidgetData {
    data object NoFavorites : RainWidgetData
    data class NoData(val cityName: String?) : RainWidgetData
    data class Ready(
        val cityName: String,
        val bars: List<RainBar>,
        val sumMm: Double
    ) : RainWidgetData
}

/** Single hour bucket on the chart. */
data class RainBar(
    val time: LocalDateTime,
    val mm: Double
)

object WidgetRainWindow {

    /** How many hours forward to show on the chart. */
    const val MAX_HOURS = 24

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Pick at most [MAX_HOURS] hours from [hours] starting from the current hour
     * forward. Returns empty list if cache holds no future-or-current hour.
     */
    fun nextNHours(
        hours: List<Pair<String, Double>>,
        now: LocalDateTime = LocalDateTime.now()
    ): List<RainBar> {
        val parsed = hours.mapNotNull { (iso, mm) ->
            runCatching { LocalDateTime.parse(iso, isoFormatter) }
                .getOrNull()
                ?.let { it to mm }
        }.sortedBy { it.first }

        // Round "now" down to the hour for stable comparisons across minutes.
        val currentHour = now.withMinute(0).withSecond(0).withNano(0)
        val window = parsed.filter { !it.first.isBefore(currentHour) }.take(MAX_HOURS)
        return window.map { (t, mm) -> RainBar(t, mm) }
    }

    /** Sum of precipitation across the displayed window, in mm. */
    fun totalMm(bars: List<RainBar>): Double = bars.sumOf { it.mm }
}

object WidgetRainLayout {

    data class Spec(
        val citySp: Int,
        val sumSp: Int,
        val axisSp: Int,
        val barChartHeightDp: Int,
        val barGapDp: Int,
        /** Hard cap on bars actually drawn (matches widget width). */
        val maxBars: Int
    )

    /** Pick a layout from current widget dimensions in dp. */
    fun resolve(widthDp: Float, heightDp: Float): Spec {
        // Narrow (< 250 dp): we can't comfortably fit 24 bars; halve to 12 buckets (2h step).
        val maxBars = when {
            widthDp < 250f -> 12
            else -> 24
        }
        val tall = heightDp >= 100f
        return Spec(
            citySp = if (tall) 14 else 12,
            sumSp = if (tall) 16 else 13,
            axisSp = 10,
            barChartHeightDp = if (tall) 44 else 28,
            barGapDp = if (maxBars >= 24) 1 else 2,
            maxBars = maxBars
        )
    }

    /**
     * Compress bars to [maxBars] buckets by simple averaging-and-summing pairs.
     * If [bars] already has <= maxBars entries it is returned as-is.
     */
    fun fitToMaxBars(bars: List<RainBar>, maxBars: Int): List<RainBar> {
        if (bars.size <= maxBars) return bars
        val groupSize = (bars.size + maxBars - 1) / maxBars
        return bars.chunked(groupSize).map { group ->
            // Take the first hour as anchor for axis labels, sum mm of the bucket.
            RainBar(time = group.first().time, mm = group.sumOf { it.mm })
        }
    }
}
