package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.util.WeatherParamFormatter
import com.ergonomic.mountainweather.util.WeatherParams
import com.ergonomic.mountainweather.util.resolveIsDay
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Loads data for the Compact widget. The location can either be pinned in
 * `WidgetPrefs` per `appWidgetId` or follow `SettingsRepository.lastLocation`.
 */
object WidgetCompactDataLoader {

    private const val TAG = "WidgetCompactLoader"

    suspend fun load(context: Context, appWidgetId: Int): CompactData {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        val pin = WidgetPrefs.getPin(appCtx, appWidgetId)
        val resolved = resolve(pin, settingsRepo.getLastLocation())
            ?: run {
                Log.d(TAG, "load id=$appWidgetId NoData (no pin, no lastLocation)")
                return CompactData.NoData
            }

        val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
        val w = runCatching { db.weatherDao().getWeather(key) }.getOrNull()
        val daily = runCatching { db.dailyForecastDao().getAll(key) }.getOrDefault(emptyList())
        val params = WidgetPrefs.getParams(appCtx, appWidgetId) ?: WidgetPrefs.DEFAULT_PARAMS
        val paramOrder = runCatching { settingsRepo.forecastSettings.first().paramOrder }
            .getOrDefault(WeatherParams.ALL.map { it.key })
        val gpsAltitude = runCatching { settingsRepo.getLastGpsAltitude() }.getOrNull()
        val theme = WidgetPrefs.getTheme(appCtx, appWidgetId)
        val opacity = WidgetPrefs.getOpacity(appCtx, appWidgetId)
        return build(appCtx, resolved, w, daily, params, paramOrder, gpsAltitude, theme, opacity)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun flow(context: Context, appWidgetId: Int): Flow<CompactData> {
        val appCtx = context.applicationContext
        val settingsRepo = SettingsRepository(appCtx)
        val db = AppDatabase.getInstance(appCtx)

        // Bundle per-widget settings (pin, params, theme, opacity) into a single
        // flow so the outer `combine` below stays within the 5-arg overload.
        val widgetSettingsFlow: Flow<WidgetSettings> = combine(
            WidgetPrefs.observePin(appCtx, appWidgetId),
            WidgetPrefs.observeParams(appCtx, appWidgetId),
            WidgetPrefs.observeTheme(appCtx, appWidgetId),
            WidgetPrefs.observeOpacity(appCtx, appWidgetId)
        ) { pin, params, theme, opacity ->
            WidgetSettings(
                pin = pin,
                params = params ?: WidgetPrefs.DEFAULT_PARAMS,
                theme = theme,
                opacity = opacity
            )
        }

        return combine(
            widgetSettingsFlow,
            settingsRepo.lastLocationFlow,
            settingsRepo.forecastSettings.map { it.paramOrder },
            settingsRepo.lastGpsAltitudeFlow
        ) { ws, last, order, alt ->
            Inputs(
                widget = ws,
                last = last,
                paramOrder = order,
                gpsAltitude = alt
            )
        }
            .distinctUntilChanged()
            .flatMapLatest { inp ->
                val resolved = resolve(inp.widget.pin, inp.last)
                    ?: return@flatMapLatest flowOf(CompactData.NoData)
                val key = WeatherRepository.locationKey(resolved.latitude, resolved.longitude)
                combine(
                    db.weatherDao().observeWeather(key),
                    db.dailyForecastDao().observe(key)
                ) { weather, daily ->
                    build(
                        appCtx, resolved, weather, daily,
                        inp.widget.params, inp.paramOrder, inp.gpsAltitude,
                        inp.widget.theme, inp.widget.opacity
                    )
                }
            }
            .distinctUntilChanged()
    }

    private data class WidgetSettings(
        val pin: WidgetPrefs.Pin?,
        val params: Set<String>,
        val theme: WidgetPrefs.Theme,
        val opacity: Int
    )

    private data class Inputs(
        val widget: WidgetSettings,
        val last: SettingsRepository.SavedLocation?,
        val paramOrder: List<String>,
        val gpsAltitude: Double?
    )

    private fun build(
        context: Context,
        resolved: Resolved,
        weather: WeatherEntity?,
        daily: List<DailyForecastEntity>,
        enabledParams: Set<String>,
        paramOrder: List<String>,
        gpsAltitude: Double?,
        theme: WidgetPrefs.Theme,
        opacityPct: Int
    ): CompactData {
        if (weather == null) {
            // Surface a label even without weather so user sees which city is pinned.
            return CompactData.Ready(
                cityName = resolved.name,
                latitude = resolved.latitude,
                longitude = resolved.longitude,
                source = resolved.source,
                temperature = null,
                apparentTemperature = null,
                temperatureMax = null,
                temperatureMin = null,
                weatherCode = null,
                isDay = true,
                cachedAt = 0L,
                params = emptyList(),
                theme = theme,
                opacityPct = opacityPct
            )
        }
        val today = LocalDate.now().toString()
        val todayDaily = daily.firstOrNull { it.date == today }
        val params = runCatching {
            WeatherParamFormatter.buildLines(
                context = context,
                weather = weather,
                enabled = enabledParams,
                paramOrder = paramOrder,
                gpsAltitude = gpsAltitude
            )
        }.getOrDefault(emptyList())
        return CompactData.Ready(
            cityName = resolved.name,
            latitude = resolved.latitude,
            longitude = resolved.longitude,
            source = resolved.source,
            temperature = weather.temperature,
            apparentTemperature = weather.apparentTemperature,
            temperatureMax = todayDaily?.temperatureMax ?: weather.temperatureMax,
            temperatureMin = todayDaily?.temperatureMin ?: weather.temperatureMin,
            weatherCode = weather.weatherCode,
            isDay = resolveIsDay(weather.isDay, weather.time, weather.sunrise, weather.sunset),
            cachedAt = weather.cachedAt,
            params = params,
            theme = theme,
            opacityPct = opacityPct
        )
    }

    private data class Resolved(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val source: CompactSource
    )

    private fun resolve(
        pin: WidgetPrefs.Pin?,
        last: SettingsRepository.SavedLocation?
    ): Resolved? {
        if (pin != null) return Resolved(pin.name, pin.latitude, pin.longitude, CompactSource.Pinned)
        if (last != null) return Resolved(last.name, last.latitude, last.longitude, CompactSource.FollowMain)
        return null
    }
}
