package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

/**
 * Loads sunrise / sunset / UV for the current main-app city. All data comes
 * from `WeatherEntity`; no new endpoints.
 */
object WidgetSunDataLoader {

    private const val TAG = "WidgetSunDataLoader"

    suspend fun loadCurrent(context: Context): SunWidgetData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        val saved = settingsRepo.getLastLocation()
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        val resolved = resolveLocation(saved, favorites) ?: return SunWidgetData.NoFavorites

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val weather = db.weatherDao().getWeather(key)
        val sunrise = WidgetSunWindow.parseIso(weather?.sunrise)
        val sunset = WidgetSunWindow.parseIso(weather?.sunset)
        return if (weather == null || sunrise == null || sunset == null) {
            Log.d(TAG, "loadCurrent: no sun data for ${resolved.name}")
            SunWidgetData.NoData(resolved.name)
        } else {
            SunWidgetData.Ready(
                cityName = resolved.name,
                sunrise = sunrise,
                sunset = sunset,
                now = LocalDateTime.now(),
                uvIndexMax = weather.uvIndexMax,
                weatherCode = weather.weatherCode,
                isDay = (weather.isDay ?: 1) == 1,
                temperature = weather.temperature
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun widgetDataFlow(context: Context): Flow<SunWidgetData> {
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
                    flowOf(SunWidgetData.NoFavorites)
                } else {
                    val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                    db.weatherDao().observeWeather(key).mapLatest { weather ->
                        val sunrise = WidgetSunWindow.parseIso(weather?.sunrise)
                        val sunset = WidgetSunWindow.parseIso(weather?.sunset)
                        if (weather == null || sunrise == null || sunset == null) {
                            SunWidgetData.NoData(resolved.name)
                        } else {
                            SunWidgetData.Ready(
                                cityName = resolved.name,
                                sunrise = sunrise,
                                sunset = sunset,
                                now = LocalDateTime.now(),
                                uvIndexMax = weather.uvIndexMax,
                                weatherCode = weather.weatherCode,
                                isDay = (weather.isDay ?: 1) == 1,
                                temperature = weather.temperature
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
