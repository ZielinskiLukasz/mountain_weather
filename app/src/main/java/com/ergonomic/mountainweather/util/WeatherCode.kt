package com.ergonomic.mountainweather.util

import androidx.annotation.StringRes
import com.ergonomic.mountainweather.R

data class WeatherInfo(
    @StringRes val descriptionRes: Int,
    val icon: String
)

fun weatherCodeToInfo(code: Int, isDay: Boolean = true): WeatherInfo = when (code) {
    0 -> WeatherInfo(R.string.wc_clear, if (isDay) "☀️" else "🌙")
    1 -> WeatherInfo(R.string.wc_mainly_clear, if (isDay) "🌤️" else "🌙")
    2 -> WeatherInfo(R.string.wc_partly_cloudy, "⛅")
    3 -> WeatherInfo(R.string.wc_overcast, "☁️")
    45, 48 -> WeatherInfo(R.string.wc_fog, "🌫️")
    51 -> WeatherInfo(R.string.wc_light_drizzle, "🌦️")
    53 -> WeatherInfo(R.string.wc_moderate_drizzle, "🌦️")
    55 -> WeatherInfo(R.string.wc_dense_drizzle, "🌧️")
    56, 57 -> WeatherInfo(R.string.wc_freezing_drizzle, "🌧️")
    61 -> WeatherInfo(R.string.wc_light_rain, "🌦️")
    63 -> WeatherInfo(R.string.wc_moderate_rain, "🌧️")
    65 -> WeatherInfo(R.string.wc_heavy_rain, "🌧️")
    66, 67 -> WeatherInfo(R.string.wc_freezing_rain, "🌧️")
    71 -> WeatherInfo(R.string.wc_light_snow, "🌨️")
    73 -> WeatherInfo(R.string.wc_moderate_snow, "🌨️")
    75 -> WeatherInfo(R.string.wc_heavy_snow, "❄️")
    77 -> WeatherInfo(R.string.wc_snow_grains, "❄️")
    80 -> WeatherInfo(R.string.wc_light_showers, "🌦️")
    81 -> WeatherInfo(R.string.wc_moderate_showers, "🌧️")
    82 -> WeatherInfo(R.string.wc_violent_showers, "🌧️")
    85 -> WeatherInfo(R.string.wc_light_snow_showers, "🌨️")
    86 -> WeatherInfo(R.string.wc_heavy_snow_showers, "❄️")
    95 -> WeatherInfo(R.string.wc_thunderstorm, "⛈️")
    96, 99 -> WeatherInfo(R.string.wc_thunderstorm_hail, "⛈️")
    else -> WeatherInfo(R.string.wc_unknown, "❓")
}

fun windDirectionToArrow(degrees: Int): String = when ((degrees + 22) / 45 % 8) {
    0 -> "↑ N"
    1 -> "↗ NE"
    2 -> "→ E"
    3 -> "↘ SE"
    4 -> "↓ S"
    5 -> "↙ SW"
    6 -> "← W"
    7 -> "↖ NW"
    else -> ""
}
