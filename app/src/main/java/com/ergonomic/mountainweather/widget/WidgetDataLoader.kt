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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

object WidgetDataLoader {

    private const val TAG = "WidgetDataLoader"

    /**
     * One-shot resolution of what the widget should display right now. Used to
     * provide an initial value for `collectAsState` so the very first composition
     * doesn't flicker through "no favorites".
     */
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

        // If lastLocation is null but a favorite exists, persist it so cycle
        // actions start from a meaningful position.
        if (saved == null) {
            runCatching {
                settingsRepo.saveLastLocation(resolved.name, resolved.latitude, resolved.longitude)
            }.onFailure { Log.w(TAG, "saveLastLocation failed: ${it.message}") }
        }

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val w = db.weatherDao().getWeather(key)
        return if (w != null) {
            Log.d(TAG, "loadCurrent: Ready ${resolved.name} t=${w.temperature} code=${w.weatherCode}")
            WidgetData.Ready(resolved.name, w.temperature, w.weatherCode, w.cachedAt)
        } else {
            Log.d(TAG, "loadCurrent: NoData ${resolved.name}")
            WidgetData.NoData(resolved.name)
        }
    }

    /**
     * Reactive stream of widget data. The composition observes this via
     * `collectAsState` so any change in `lastLocation`, favorites or cached
     * weather is reflected immediately while the Glance composition is alive.
     */
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
                    db.weatherDao().observeWeather(key).map { w ->
                        if (w != null) {
                            WidgetData.Ready(resolved.name, w.temperature, w.weatherCode, w.cachedAt)
                        } else {
                            WidgetData.NoData(resolved.name)
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
