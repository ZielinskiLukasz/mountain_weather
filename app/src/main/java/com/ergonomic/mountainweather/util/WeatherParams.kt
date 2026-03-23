package com.ergonomic.mountainweather.util

import com.ergonomic.mountainweather.R

data class WeatherParamDef(
    val key: String,
    val icon: String,
    val labelRes: Int
)

object WeatherParams {
    const val APPARENT_TEMP = "apparent_temp"
    const val TEMPERATURE = "temperature"
    const val WIND = "wind"
    const val HUMIDITY = "humidity"
    const val PRECIPITATION = "precipitation"
    const val PRESSURE = "pressure"
    const val CLOUD_COVER = "cloud_cover"
    const val WIND_GUSTS = "wind_gusts"
    const val WIND_DIRECTION = "wind_direction"
    const val SNOWFALL = "snowfall"
    const val RAIN = "rain"
    const val SUNRISE_SUNSET = "sunrise_sunset"
    const val UV_INDEX = "uv_index"
    const val RAIN_SUM = "rain_sum"
    const val SHOWERS_SUM = "showers_sum"
    const val SNOWFALL_SUM = "snowfall_sum"
    const val PRECIP_HOURS = "precip_hours"
    const val PRECIP_PROBABILITY = "precip_probability"
    const val SUNSHINE_DURATION = "sunshine_duration"
    const val WIND_GUSTS_MAX = "wind_gusts_max"
    const val DOMINANT_WIND_DIR = "dominant_wind_dir"
    const val DEW_POINT = "dew_point"
    const val VISIBILITY = "visibility"
    const val FREEZING_LEVEL = "freezing_level"

    const val AQI_EU = "aqi_eu"
    const val AQI_US = "aqi_us"
    const val PM25 = "pm2_5"
    const val PM10 = "pm10"
    const val OZONE = "ozone"

    val DEFAULTS = setOf(APPARENT_TEMP, TEMPERATURE, WIND, HUMIDITY, PRECIPITATION, PRESSURE)

    val ALL = listOf(
        WeatherParamDef(APPARENT_TEMP, "🤗", R.string.param_feels_like),
        WeatherParamDef(TEMPERATURE, "🌡️", R.string.param_temperature),
        WeatherParamDef(WIND, "💨", R.string.param_wind),
        WeatherParamDef(HUMIDITY, "💧", R.string.param_humidity),
        WeatherParamDef(PRECIPITATION, "🌧️", R.string.param_precipitation),
        WeatherParamDef(PRESSURE, "🌀", R.string.param_pressure),
        WeatherParamDef(CLOUD_COVER, "☁️", R.string.param_clouds),
        WeatherParamDef(WIND_GUSTS, "🌬️", R.string.param_wind_gusts),
        WeatherParamDef(WIND_DIRECTION, "🧭", R.string.param_wind_dir),
        WeatherParamDef(SNOWFALL, "❄️", R.string.param_snowfall),
        WeatherParamDef(RAIN, "🌦️", R.string.param_rain),
        WeatherParamDef(SUNRISE_SUNSET, "🌅", R.string.param_sunrise_sunset),
        WeatherParamDef(UV_INDEX, "☀️", R.string.param_uv_index),
        WeatherParamDef(RAIN_SUM, "💦", R.string.param_rain_sum),
        WeatherParamDef(SHOWERS_SUM, "🚿", R.string.param_showers_sum),
        WeatherParamDef(SNOWFALL_SUM, "🌨️", R.string.param_snowfall_sum),
        WeatherParamDef(PRECIP_HOURS, "⏱️", R.string.param_precip_hours),
        WeatherParamDef(PRECIP_PROBABILITY, "📊", R.string.param_precip_prob),
        WeatherParamDef(SUNSHINE_DURATION, "🌤️", R.string.param_sunshine),
        WeatherParamDef(WIND_GUSTS_MAX, "💥", R.string.param_gusts_max),
        WeatherParamDef(DOMINANT_WIND_DIR, "🔄", R.string.param_dom_wind),
        WeatherParamDef(DEW_POINT, "💧", R.string.param_dew_point),
        WeatherParamDef(VISIBILITY, "👁️", R.string.param_visibility),
        WeatherParamDef(FREEZING_LEVEL, "🏔️", R.string.param_freezing_level),
        WeatherParamDef(AQI_EU, "🟢", R.string.param_aqi_eu),
        WeatherParamDef(AQI_US, "🟡", R.string.param_aqi_us),
        WeatherParamDef(PM25, "🫁", R.string.param_pm25),
        WeatherParamDef(PM10, "💨", R.string.param_pm10),
        WeatherParamDef(OZONE, "🛡️", R.string.param_ozone)
    )

    val AIR_QUALITY_KEYS = setOf(AQI_EU, AQI_US, PM25, PM10, OZONE)
}
