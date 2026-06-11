package com.ergonomic.mountainweather.util

import android.content.Context
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.data.local.WeatherEntity
import java.util.Locale

data class WeatherParamLine(
    val key: String,
    val icon: String,
    val label: String,
    val value: String
) {
    fun displayText(compact: Boolean): String =
        if (compact) "$icon $value" else "$icon $label: $value"
}

object WeatherParamFormatter {

    private val pm25Thresholds = doubleArrayOf(10.0, 25.0, 50.0)
    private val pm10Thresholds = doubleArrayOf(20.0, 50.0, 100.0)

    fun buildLines(
        context: Context,
        weather: WeatherEntity,
        enabled: Set<String>,
        paramOrder: List<String>
    ): List<WeatherParamLine> {
        val allItems = linkedMapOf<String, WeatherParamLine>()

        if (WeatherParams.APPARENT_TEMP in enabled) {
            allItems[WeatherParams.APPARENT_TEMP] = line(
                WeatherParams.APPARENT_TEMP,
                context.getString(R.string.param_feels_like),
                "${weather.apparentTemperature.toInt()}°C"
            )
        }
        if (WeatherParams.TEMPERATURE in enabled &&
            weather.temperatureMax != null && weather.temperatureMin != null
        ) {
            allItems[WeatherParams.TEMPERATURE] = line(
                WeatherParams.TEMPERATURE,
                context.getString(R.string.param_temperature),
                context.getString(
                    R.string.temp_max_min,
                    weather.temperatureMax.toInt().toString(),
                    weather.temperatureMin.toInt().toString()
                )
            )
        }
        if (WeatherParams.WIND in enabled) {
            allItems[WeatherParams.WIND] = line(
                WeatherParams.WIND,
                context.getString(R.string.param_wind),
                "${weather.windSpeed} km/h ${windDirectionToArrow(weather.windDirection)}"
            )
        }
        if (WeatherParams.HUMIDITY in enabled) {
            allItems[WeatherParams.HUMIDITY] = line(
                WeatherParams.HUMIDITY,
                context.getString(R.string.param_humidity),
                "${weather.humidity}%"
            )
        }
        if (WeatherParams.PRECIPITATION in enabled) {
            allItems[WeatherParams.PRECIPITATION] = line(
                WeatherParams.PRECIPITATION,
                context.getString(R.string.param_precipitation),
                String.format(Locale.getDefault(), "%.1f mm", weather.precipitation)
            )
        }
        if (WeatherParams.PRESSURE in enabled) {
            allItems[WeatherParams.PRESSURE] = line(
                WeatherParams.PRESSURE,
                context.getString(R.string.param_pressure),
                "${weather.pressure.toInt()} hPa"
            )
        }
        if (WeatherParams.CLOUD_COVER in enabled && weather.cloudCover != null) {
            allItems[WeatherParams.CLOUD_COVER] = line(
                WeatherParams.CLOUD_COVER,
                context.getString(R.string.param_clouds),
                "${weather.cloudCover}%"
            )
        }
        if (WeatherParams.WIND_GUSTS in enabled && weather.windGusts != null) {
            allItems[WeatherParams.WIND_GUSTS] = line(
                WeatherParams.WIND_GUSTS,
                context.getString(R.string.param_wind_gusts),
                "${weather.windGusts} km/h"
            )
        }
        if (WeatherParams.WIND_DIRECTION in enabled) {
            allItems[WeatherParams.WIND_DIRECTION] = line(
                WeatherParams.WIND_DIRECTION,
                context.getString(R.string.param_wind_dir),
                "${weather.windDirection}° ${windDirectionToArrow(weather.windDirection)}"
            )
        }
        if (WeatherParams.SNOWFALL in enabled && weather.snowfall != null) {
            allItems[WeatherParams.SNOWFALL] = line(
                WeatherParams.SNOWFALL,
                context.getString(R.string.param_snowfall),
                String.format(Locale.getDefault(), "%.1f cm", weather.snowfall)
            )
        }
        if (WeatherParams.RAIN in enabled && weather.rain != null) {
            allItems[WeatherParams.RAIN] = line(
                WeatherParams.RAIN,
                context.getString(R.string.param_rain),
                String.format(Locale.getDefault(), "%.1f mm", weather.rain)
            )
        }
        if (WeatherParams.SUNRISE_SUNSET in enabled &&
            weather.sunrise != null && weather.sunset != null
        ) {
            allItems[WeatherParams.SUNRISE_SUNSET] = line(
                WeatherParams.SUNRISE_SUNSET,
                context.getString(R.string.param_sunrise_sunset),
                "${weather.sunrise.takeLast(5)} / ${weather.sunset.takeLast(5)}"
            )
        }
        if (WeatherParams.UV_INDEX in enabled && weather.uvIndexMax != null) {
            allItems[WeatherParams.UV_INDEX] = line(
                WeatherParams.UV_INDEX,
                context.getString(R.string.param_uv_index),
                "${weather.uvIndexMax}"
            )
        }
        if (WeatherParams.RAIN_SUM in enabled && weather.rainSum != null) {
            allItems[WeatherParams.RAIN_SUM] = line(
                WeatherParams.RAIN_SUM,
                context.getString(R.string.param_rain_sum),
                String.format(Locale.getDefault(), "%.1f mm", weather.rainSum)
            )
        }
        if (WeatherParams.SHOWERS_SUM in enabled && weather.showersSum != null) {
            allItems[WeatherParams.SHOWERS_SUM] = line(
                WeatherParams.SHOWERS_SUM,
                context.getString(R.string.param_showers_sum),
                String.format(Locale.getDefault(), "%.1f mm", weather.showersSum)
            )
        }
        if (WeatherParams.SNOWFALL_SUM in enabled && weather.snowfallSum != null) {
            allItems[WeatherParams.SNOWFALL_SUM] = line(
                WeatherParams.SNOWFALL_SUM,
                context.getString(R.string.param_snowfall_sum),
                String.format(Locale.getDefault(), "%.1f cm", weather.snowfallSum)
            )
        }
        if (WeatherParams.PRECIP_HOURS in enabled && weather.precipitationHours != null) {
            allItems[WeatherParams.PRECIP_HOURS] = line(
                WeatherParams.PRECIP_HOURS,
                context.getString(R.string.param_precip_hours),
                "${weather.precipitationHours.toInt()} h"
            )
        }
        if (WeatherParams.PRECIP_PROBABILITY in enabled && weather.precipitationProbabilityMax != null) {
            allItems[WeatherParams.PRECIP_PROBABILITY] = line(
                WeatherParams.PRECIP_PROBABILITY,
                context.getString(R.string.param_precip_prob),
                "${weather.precipitationProbabilityMax}%"
            )
        }
        if (WeatherParams.SUNSHINE_DURATION in enabled && weather.sunshineDuration != null) {
            val hours = (weather.sunshineDuration / 3600).toInt()
            val minutes = ((weather.sunshineDuration % 3600) / 60).toInt()
            allItems[WeatherParams.SUNSHINE_DURATION] = line(
                WeatherParams.SUNSHINE_DURATION,
                context.getString(R.string.param_sunshine),
                "${hours}h ${minutes}m"
            )
        }
        if (WeatherParams.WIND_GUSTS_MAX in enabled && weather.windGustsMax != null) {
            allItems[WeatherParams.WIND_GUSTS_MAX] = line(
                WeatherParams.WIND_GUSTS_MAX,
                context.getString(R.string.param_gusts_max),
                "${weather.windGustsMax} km/h"
            )
        }
        if (WeatherParams.DOMINANT_WIND_DIR in enabled && weather.dominantWindDirection != null) {
            allItems[WeatherParams.DOMINANT_WIND_DIR] = line(
                WeatherParams.DOMINANT_WIND_DIR,
                context.getString(R.string.param_dom_wind),
                "${weather.dominantWindDirection}° ${windDirectionToArrow(weather.dominantWindDirection)}"
            )
        }
        if (WeatherParams.DEW_POINT in enabled && weather.dewPoint != null) {
            allItems[WeatherParams.DEW_POINT] = line(
                WeatherParams.DEW_POINT,
                context.getString(R.string.param_dew_point),
                "${weather.dewPoint}°C"
            )
        }
        if (WeatherParams.VISIBILITY in enabled && weather.visibility != null) {
            allItems[WeatherParams.VISIBILITY] = line(
                WeatherParams.VISIBILITY,
                context.getString(R.string.param_visibility),
                "${"%.1f".format(Locale.getDefault(), weather.visibility / 1000.0)} km"
            )
        }
        if (WeatherParams.FREEZING_LEVEL in enabled && weather.freezingLevelHeight != null) {
            allItems[WeatherParams.FREEZING_LEVEL] = line(
                WeatherParams.FREEZING_LEVEL,
                context.getString(R.string.param_freezing_level),
                "${weather.freezingLevelHeight.toInt()} m"
            )
        }
        if (WeatherParams.AQI_EU in enabled && weather.aqiEu != null) {
            allItems[WeatherParams.AQI_EU] = line(
                WeatherParams.AQI_EU,
                context.getString(R.string.param_aqi_eu),
                "${weather.aqiEu} EAQI"
            )
        }
        if (WeatherParams.AQI_US in enabled && weather.aqiUs != null) {
            allItems[WeatherParams.AQI_US] = line(
                WeatherParams.AQI_US,
                context.getString(R.string.param_aqi_us),
                "${weather.aqiUs} USAQI"
            )
        }
        if (WeatherParams.PM25 in enabled && weather.pm25 != null) {
            allItems[WeatherParams.PM25] = line(
                WeatherParams.PM25,
                context.getString(R.string.param_pm25),
                "${pmLevel(weather.pm25, pm25Thresholds)} ${"%.1f".format(Locale.getDefault(), weather.pm25)} μg/m³"
            )
        }
        if (WeatherParams.PM10 in enabled && weather.pm10 != null) {
            allItems[WeatherParams.PM10] = line(
                WeatherParams.PM10,
                context.getString(R.string.param_pm10),
                "${pmLevel(weather.pm10, pm10Thresholds)} ${"%.1f".format(Locale.getDefault(), weather.pm10)} μg/m³"
            )
        }
        if (WeatherParams.OZONE in enabled && weather.ozone != null) {
            allItems[WeatherParams.OZONE] = line(
                WeatherParams.OZONE,
                context.getString(R.string.param_ozone),
                "${"%.0f".format(Locale.getDefault(), weather.ozone)} μg/m³"
            )
        }
        if (WeatherParams.ELEVATION in enabled && weather.elevation != null) {
            allItems[WeatherParams.ELEVATION] = line(
                WeatherParams.ELEVATION,
                context.getString(R.string.param_elevation),
                "${"%.0f".format(Locale.getDefault(), weather.elevation)} m"
            )
        }
        if (WeatherParams.GPS_ALTITUDE in enabled) {
            allItems[WeatherParams.GPS_ALTITUDE] = line(
                WeatherParams.GPS_ALTITUDE,
                context.getString(R.string.param_gps_altitude),
                "—"
            )
        }

        val orderedKeys = paramOrder.filter { it in allItems } +
            allItems.keys.filter { it !in paramOrder }
        return orderedKeys.mapNotNull { allItems[it] }
    }

    private fun line(key: String, label: String, value: String): WeatherParamLine {
        val icon = WeatherParams.ALL.firstOrNull { it.key == key }?.icon ?: "•"
        return WeatherParamLine(key, icon, label, value)
    }

    private fun pmLevel(value: Double, thresholds: DoubleArray): String = when {
        value <= thresholds[0] -> "🟢"
        value <= thresholds[1] -> "🟡"
        value <= thresholds[2] -> "🟠"
        else -> "🔴"
    }
}
