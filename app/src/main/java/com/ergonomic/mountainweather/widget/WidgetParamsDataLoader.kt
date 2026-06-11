package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.util.WeatherParamFormatter
import com.ergonomic.mountainweather.util.WeatherParams
import com.ergonomic.mountainweather.util.resolveIsDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

object WidgetParamsDataLoader {

    private const val TAG = "WidgetParamsDataLoader"

    suspend fun loadCurrent(context: Context): WidgetParamsData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)
        val settings = settingsRepo.forecastSettings.first()

        val saved = settingsRepo.getLastLocation()
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        val resolved = resolveLocation(saved, favorites) ?: return WidgetParamsData.NoFavorites

        if (saved == null) {
            runCatching {
                settingsRepo.saveLastLocation(resolved.name, resolved.latitude, resolved.longitude)
            }
        }

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val weather = db.weatherDao().getWeather(key)
        return if (weather != null) {
            buildReady(appCtx, resolved.name, weather, settings.enabledCurrentParams, settings.paramOrder)
        } else {
            WidgetParamsData.NoData(resolved.name)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun widgetDataFlow(context: Context): Flow<WidgetParamsData> {
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
                val resolved = resolveLocation(saved, favorites)
                if (resolved == null) {
                    flowOf(WidgetParamsData.NoFavorites)
                } else {
                    val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                    db.weatherDao().observeWeather(key).mapLatest { weather ->
                        if (weather != null) {
                            buildReady(
                                appCtx,
                                resolved.name,
                                weather,
                                settings.enabledCurrentParams,
                                settings.paramOrder
                            )
                        } else {
                            WidgetParamsData.NoData(resolved.name)
                        }
                    }
                }
            }
            .distinctUntilChanged()
    }

    private fun buildReady(
        context: Context,
        cityName: String,
        weather: WeatherEntity,
        enabledParams: Set<String>,
        paramOrder: List<String>
    ): WidgetParamsData.Ready {
        val enabled = enabledParams - WeatherParams.GPS_ALTITUDE
        val params = WeatherParamFormatter.buildLines(context, weather, enabled, paramOrder)
        Log.d(TAG, "buildReady: $cityName params=${params.size}")
        return WidgetParamsData.Ready(
            cityName = cityName,
            temperature = weather.temperature,
            weatherCode = weather.weatherCode,
            isDay = resolveIsDay(weather.isDay, weather.time, weather.sunrise, weather.sunset),
            params = params
        )
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
