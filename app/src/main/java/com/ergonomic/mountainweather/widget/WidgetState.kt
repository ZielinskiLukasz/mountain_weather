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
        val cachedAt: Long
    ) : WidgetData
}
