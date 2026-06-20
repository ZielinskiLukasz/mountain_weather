package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

/**
 * Loads the next [WidgetRainWindow.MAX_HOURS] hours of precipitation for the
 * currently selected city, mirroring `WidgetHourlyDataLoader`'s flow.
 */
object WidgetRainDataLoader {

    private const val TAG = "WidgetRainDataLoader"

    suspend fun loadCurrent(context: Context): RainWidgetData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        val saved = settingsRepo.getLastLocation()
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        val resolved = resolveLocation(saved, favorites) ?: return RainWidgetData.NoFavorites

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val hours = db.hourlyForecastDao().getAll(key).map { it.time to it.precipitation }
        val bars = WidgetRainWindow.nextNHours(hours)
        return if (bars.isEmpty()) {
            Log.d(TAG, "loadCurrent: no hourly cache for ${resolved.name}")
            RainWidgetData.NoData(resolved.name)
        } else {
            RainWidgetData.Ready(
                cityName = resolved.name,
                bars = bars,
                sumMm = WidgetRainWindow.totalMm(bars)
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun widgetDataFlow(context: Context): Flow<RainWidgetData> {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        return combine(
            settingsRepo.lastLocationFlow,
            db.savedLocationDao().observeFavorites(limit = 10)
        ) { saved, favorites -> saved to favorites }
            .distinctUntilChanged()
            .flatMapLatest { (saved, favorites) ->
                val resolved = resolveLocation(saved, favorites)
                if (resolved == null) {
                    flowOf(RainWidgetData.NoFavorites)
                } else {
                    val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                    db.hourlyForecastDao().observe(key).mapLatest { hourly ->
                        val bars = WidgetRainWindow.nextNHours(
                            hourly.map { it.time to it.precipitation }
                        )
                        if (bars.isEmpty()) {
                            RainWidgetData.NoData(resolved.name)
                        } else {
                            RainWidgetData.Ready(
                                cityName = resolved.name,
                                bars = bars,
                                sumMm = WidgetRainWindow.totalMm(bars)
                            )
                        }
                    }
                }
            }
            .distinctUntilChanged()
    }

    private data class ResolvedLocation(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    private fun resolveLocation(
        saved: SettingsRepository.SavedLocation?,
        favorites: List<SavedLocationEntity>
    ): ResolvedLocation? {
        if (saved != null) return ResolvedLocation(saved.name, saved.latitude, saved.longitude)
        val first = favorites.firstOrNull() ?: return null
        return ResolvedLocation(first.name, first.latitude, first.longitude)
    }
}
