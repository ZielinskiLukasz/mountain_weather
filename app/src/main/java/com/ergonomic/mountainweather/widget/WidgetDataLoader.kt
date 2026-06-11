package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.util.resolveIsDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

object WidgetDataLoader {

    private const val TAG = "WidgetDataLoader"

    suspend fun loadCurrent(context: Context): WidgetData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        val saved = settingsRepo.getLastLocation()
        val favorites = runCatching { db.savedLocationDao().getFavorites() }.getOrDefault(emptyList())
        val resolved = resolveLocation(saved, favorites)

        if (resolved == null) {
            Log.d(TAG, "loadCurrent: NoFavorites (saved=$saved favoritesCount=${favorites.size})")
            return WidgetData.NoFavorites
        }

        if (saved == null) {
            runCatching {
                settingsRepo.saveLastLocation(resolved.name, resolved.latitude, resolved.longitude)
            }.onFailure { Log.w(TAG, "saveLastLocation failed: ${it.message}") }
        }

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val w = db.weatherDao().getWeather(key)
        return if (w != null) {
            Log.d(TAG, "loadCurrent: Ready ${resolved.name} t=${w.temperature} code=${w.weatherCode}")
            buildReady(resolved, favorites, w, db)
        } else {
            Log.d(TAG, "loadCurrent: NoData ${resolved.name}")
            WidgetData.NoData(resolved.name)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun widgetDataFlow(context: Context): Flow<WidgetData> {
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
                    flowOf(WidgetData.NoFavorites)
                } else {
                    val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                    // Re-observe favorites inside so neighbor columns update even when
                    // the current city's weather row has not changed.
                    combine(
                        db.savedLocationDao().observeFavorites(limit = 10),
                        db.weatherDao().observeWeather(key)
                    ) { favs, weather -> Triple(saved, favs, weather) }
                        .mapLatest { (lastSaved, favs, weather) ->
                            val loc = resolveLocation(lastSaved, favs)
                                ?: return@mapLatest WidgetData.NoFavorites
                            if (weather != null) {
                                buildReady(loc, favs, weather, db)
                            } else {
                                WidgetData.NoData(loc.name)
                            }
                        }
                }
            }
            .distinctUntilChanged()
    }

    private suspend fun buildReady(
        resolved: ResolvedLocation,
        favorites: List<SavedLocationEntity>,
        weather: WeatherEntity,
        db: AppDatabase
    ): WidgetData.Ready {
        val (previous, next) = neighborSnapshots(resolved, favorites, db)
        return WidgetData.Ready(
            cityName = resolved.name,
            temperature = weather.temperature,
            weatherCode = weather.weatherCode,
            cachedAt = weather.cachedAt,
            isDay = resolveIsDay(weather.isDay, weather.time, weather.sunrise, weather.sunset),
            previous = previous,
            next = next
        )
    }

    private suspend fun neighborSnapshots(
        current: ResolvedLocation,
        favorites: List<SavedLocationEntity>,
        db: AppDatabase
    ): Pair<WidgetCitySnapshot?, WidgetCitySnapshot?> {
        if (favorites.isEmpty()) return null to null

        val currentIdx = favorites.indexOfFirst { fav ->
            kotlin.math.abs(fav.latitude - current.latitude) < 0.005 &&
                kotlin.math.abs(fav.longitude - current.longitude) < 0.005
        }
        if (currentIdx < 0) return null to null

        // Linear order (sortOrder): previous cities on the left, next on the right.
        // First favorite → center only + right neighbor; left stays empty until user cycles.
        val prev = if (currentIdx > 0) {
            snapshotForFavorite(db, favorites[currentIdx - 1])
        } else {
            null
        }
        val next = if (currentIdx + 1 < favorites.size) {
            snapshotForFavorite(db, favorites[currentIdx + 1])
        } else {
            null
        }
        return prev to next
    }

    private suspend fun snapshotForFavorite(
        db: AppDatabase,
        fav: SavedLocationEntity
    ): WidgetCitySnapshot {
        val key = WeatherRepository.locationKey(fav.latitude, fav.longitude)
        val w = db.weatherDao().getWeather(key)
        return if (w != null) {
            WidgetCitySnapshot(
                cityName = fav.name,
                temperature = w.temperature,
                weatherCode = w.weatherCode,
                isDay = resolveIsDay(w.isDay, w.time, w.sunrise, w.sunset)
            )
        } else {
            WidgetCitySnapshot(
                cityName = fav.name,
                temperature = null,
                weatherCode = null,
                isDay = true
            )
        }
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
