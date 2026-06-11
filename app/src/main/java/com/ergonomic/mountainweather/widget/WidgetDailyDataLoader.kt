package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
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

object WidgetDailyDataLoader {

    private const val TAG = "WidgetDailyDataLoader"

    suspend fun loadCurrent(context: Context): DailyWidgetData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        val saved = settingsRepo.getLastLocation()
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        val resolved = resolveLocation(saved, favorites) ?: return DailyWidgetData.NoFavorites

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val settings = settingsRepo.forecastSettings.first()
        val days = WidgetDailyWindow.filterDaysForWidget(
            db.dailyForecastDao().getAll(key).map { it.toSnapshot() },
            settings.dailyForecastDays
        )
        return buildReady(resolved.name, days)
    }

    suspend fun dayCountForCurrentLocation(context: Context): Int {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val settings = settingsRepo.forecastSettings.first()
        if (settings.dailyForecastDays <= 0) return 0
        val db = AppDatabase.getInstance(appCtx)
        val saved = settingsRepo.getLastLocation() ?: return 0
        val key = WeatherRepository.locationKey(saved.latitude, saved.longitude)
        return WidgetDailyWindow.filterDaysForWidget(
            db.dailyForecastDao().getAll(key).map { it.toSnapshot() },
            settings.dailyForecastDays
        ).size
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun widgetDataFlow(context: Context): Flow<DailyWidgetData> {
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
                    flowOf(DailyWidgetData.NoFavorites)
                } else {
                    val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                    combine(
                        db.savedLocationDao().observeFavorites(limit = 10),
                        db.dailyForecastDao().observe(key)
                    ) { favs, daily -> resolveLocation(saved, favs) to daily }
                        .mapLatest { (loc, daily) ->
                            if (loc == null) DailyWidgetData.NoFavorites
                            else buildReady(
                                loc.name,
                                WidgetDailyWindow.filterDaysForWidget(
                                    daily.map { it.toSnapshot() },
                                    settings.dailyForecastDays
                                )
                            )
                        }
                }
            }
            .distinctUntilChanged()
    }

    private fun buildReady(cityName: String, days: List<DailyDaySnapshot>): DailyWidgetData {
        return if (days.isEmpty()) {
            Log.d(TAG, "buildReady: NoData for $cityName (no daily cache)")
            DailyWidgetData.NoData(cityName)
        } else {
            Log.d(TAG, "buildReady: $cityName days=${days.size}")
            DailyWidgetData.Ready(cityName, days)
        }
    }

    private fun DailyForecastEntity.toSnapshot() = DailyDaySnapshot(
        date = date,
        weatherCode = weatherCode,
        tempMax = temperatureMax,
        tempMin = temperatureMin
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
