package com.ergonomic.mountainweather.widget

/**
 * What the widget should display. The composition observes a `Flow<WidgetData>`
 * via `collectAsState` so any change in the underlying sources (DataStore,
 * favorites, weather cache) is reflected immediately.
 */
sealed interface WidgetData {
    /** No last-location and no favorites configured. Tap → open app. */
    data object NoFavorites : WidgetData

    /** A location is selected but no cached weather is available for it yet. */
    data class NoData(val cityName: String?) : WidgetData

    /** Full snapshot available. */
    data class Ready(
        val cityName: String,
        val temperature: Double,
        val weatherCode: Int,
        val cachedAt: Long,
        val isDay: Boolean = true,
        /** Previous favorite in the carousel (only populated when ≥2 favorites). */
        val previous: WidgetCitySnapshot? = null,
        /** Next favorite in the carousel (only populated when ≥2 favorites). */
        val next: WidgetCitySnapshot? = null
    ) : WidgetData
}

/** Compact city display used for prev/next previews on wide widget layouts. */
data class WidgetCitySnapshot(
    val cityName: String,
    val temperature: Double?,
    val weatherCode: Int?,
    val isDay: Boolean = true
)
