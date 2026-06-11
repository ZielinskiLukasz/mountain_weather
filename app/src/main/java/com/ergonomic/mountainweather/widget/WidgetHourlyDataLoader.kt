package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.util.resolveIsDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

object WidgetHourlyDataLoader {

    private const val TAG = "WidgetHourlyDataLoader"

    suspend fun loadCurrent(context: Context): HourlyWidgetData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)
        val settings = settingsRepo.forecastSettings.first()

        if (!settings.showHourly) {
            val saved = settingsRepo.getLastLocation()
            return HourlyWidgetData.NoData(saved?.name, hourlyDisabled = true)
        }

        val saved = settingsRepo.getLastLocation()
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        val resolved = resolveLocation(saved, favorites) ?: return HourlyWidgetData.NoFavorites

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val weather = db.weatherDao().getWeather(key)
        val hours = db.hourlyForecastDao().getAll(key).map { it.toSnapshot() }
        return buildReady(resolved.name, weather, hours)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun widgetDataFlow(context: Context): Flow<HourlyWidgetData> {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        return combine(
            settingsRepo.lastLocationFlow,
            settingsRepo.forecastSettings,
            db.savedLocationDao().observeFavorites(limit = 10)
        ) { saved, settings, favorites -> Triple(saved, settings, favorites) }
            .distinctUntilChanged()
            .flatMapLatest { (saved, settings, favorites) ->
                if (!settings.showHourly) {
                    val name = resolveLocation(saved, favorites)?.name
                    flowOf(HourlyWidgetData.NoData(name, hourlyDisabled = true))
                } else {
                    val resolved = resolveLocation(saved, favorites)
                    if (resolved == null) {
                        flowOf(HourlyWidgetData.NoFavorites)
                    } else {
                        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                        combine(
                            db.weatherDao().observeWeather(key),
                            db.hourlyForecastDao().observe(key)
                        ) { weather, hourly -> weather to hourly }
                            .mapLatest { (weather, hourly) ->
                                buildReady(
                                    resolved.name,
                                    weather,
                                    hourly.map { it.toSnapshot() }
                                )
                            }
                    }
                }
            }
            .distinctUntilChanged()
    }

    private fun buildReady(
        cityName: String,
        weather: WeatherEntity?,
        hours: List<HourlyHourSnapshot>
    ): HourlyWidgetData {
        val visible = WidgetHourlyWindow.filterHoursForWidget(hours)
        return if (visible.isEmpty()) {
            Log.d(TAG, "buildReady: NoData for $cityName (no hourly cache)")
            HourlyWidgetData.NoData(cityName)
        } else {
            Log.d(TAG, "buildReady: $cityName hours=${visible.size}")
            HourlyWidgetData.Ready(
                cityName = cityName,
                currentTemp = weather?.temperature ?: visible.first().temperature,
                currentWeatherCode = weather?.weatherCode ?: visible.first().weatherCode,
                currentIsDay = weather?.let {
                    resolveIsDay(it.isDay, it.time, it.sunrise, it.sunset)
                } ?: resolveIsDay(timeIso = visible.first().time),
                hours = visible
            )
        }
    }

    private fun HourlyForecastEntity.toSnapshot() = HourlyHourSnapshot(
        time = time,
        temperature = temperature,
        weatherCode = weatherCode,
        precipitation = precipitation
    )

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
