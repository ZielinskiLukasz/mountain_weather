package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.util.dryEquivalentWeatherCode
import com.ergonomic.mountainweather.util.resolveIsDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        val weather = db.weatherDao().getWeather(key)
        val hourly = db.hourlyForecastDao().getAll(key)
        val days = WidgetDailyWindow.filterDaysForWidget(
            db.dailyForecastDao().getAll(key).map { it.toSnapshot() },
            settings.dailyForecastDays
        )
        val enriched = enrichTodayWithCurrentWeather(days, weather, hourly)
        return buildReady(resolved.name, enriched)
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
                        db.dailyForecastDao().observe(key),
                        db.weatherDao().observeWeather(key),
                        db.hourlyForecastDao().observe(key)
                    ) { favs, daily, weather, hourly ->
                        TupleData(resolveLocation(saved, favs), daily, weather, hourly)
                    }
                        .mapLatest { tuple ->
                            if (tuple.loc == null) DailyWidgetData.NoFavorites
                            else {
                                val days = WidgetDailyWindow.filterDaysForWidget(
                                    tuple.daily.map { it.toSnapshot() },
                                    settings.dailyForecastDays
                                )
                                val enriched = enrichTodayWithCurrentWeather(
                                    days, tuple.weather, tuple.hourly
                                )
                                buildReady(tuple.loc.name, enriched)
                            }
                        }
                }
            }
            .distinctUntilChanged()
    }

    /**
     * For today's entry in the daily forecast, override the weather code and
     * temperature with the current-hour values — matching the main screen logic.
     *
     * The main screen uses the hourly forecast entry for the current hour to pick
     * the icon (weather code), and WeatherEntity.temperature for the displayed
     * temperature.  The daily API returns a whole-day aggregate weather code
     * (which might be "overcast" or "rain" even though right now it's sunny) and
     * the daily max temperature (which differs from the current reading).
     */
    private fun enrichTodayWithCurrentWeather(
        days: List<DailyDaySnapshot>,
        weather: WeatherEntity?,
        hourly: List<HourlyForecastEntity>
    ): List<DailyDaySnapshot> {
        if (weather == null) return days
        val today = LocalDate.now().toString()

        // Find the hourly forecast entry for the current hour (same logic as
        // MainActivity.WeatherContent)
        val currentHour = LocalDateTime.now().hour
        val hourlyCode = hourly.firstOrNull { entry ->
            try {
                LocalDateTime.parse(
                    entry.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME
                ).hour == currentHour
                    && entry.time.startsWith(today)
            } catch (_: Exception) {
                false
            }
        }?.weatherCode

        val effectiveCode = hourlyCode ?: weather.weatherCode
        val isDay = resolveIsDay(
            isDayFromApi = if (hourlyCode != null) null else weather.isDay,
            timeIso = if (hourlyCode != null) null else weather.time,
            sunriseIso = weather.sunrise,
            sunsetIso = weather.sunset
        )

        return days.map { day ->
            if (day.date == today) {
                day.copy(
                    currentWeatherCode = effectiveCode,
                    currentTemp = weather.temperature,
                    currentIsDay = isDay
                )
            } else {
                day
            }
        }
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

    private fun DailyForecastEntity.toSnapshot(): DailyDaySnapshot {
        val effectiveCode = dryEquivalentWeatherCode(weatherCode, precipitationSum)
        return DailyDaySnapshot(
            date = date,
            weatherCode = effectiveCode,
            tempMax = temperatureMax,
            tempMin = temperatureMin
        )
    }

    /** Helper to bundle combine() results without destructuring limits. */
    private data class TupleData(
        val loc: ResolvedLocation?,
        val daily: List<DailyForecastEntity>,
        val weather: WeatherEntity?,
        val hourly: List<HourlyForecastEntity>
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

