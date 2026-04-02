package com.ergonomic.mountainweather.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ergonomic.mountainweather.R

data class WeatherInfo(
    @StringRes val descriptionRes: Int,
    val icon: String = "",
    @DrawableRes val iconRes: Int = 0
)

fun weatherCodeToInfo(code: Int, isDay: Boolean = true): WeatherInfo = when (code) {
    0 -> WeatherInfo(R.string.wc_clear, if (isDay) "☀️" else "🌙")
    1 -> if (isDay) WeatherInfo(R.string.wc_mainly_clear, "🌤️")
         else WeatherInfo(R.string.wc_mainly_clear, iconRes = R.drawable.ic_weather_night_cloudy)
    2 -> if (isDay) WeatherInfo(R.string.wc_partly_cloudy, "⛅")
         else WeatherInfo(R.string.wc_partly_cloudy, iconRes = R.drawable.ic_weather_night_cloudy)
    3 -> WeatherInfo(R.string.wc_overcast, iconRes = R.drawable.ic_weather_overcast)
    45, 48 -> WeatherInfo(R.string.wc_fog, iconRes = R.drawable.ic_weather_fog)
    51 -> if (isDay) WeatherInfo(R.string.wc_light_drizzle, "🌦️")
          else WeatherInfo(R.string.wc_light_drizzle, iconRes = R.drawable.ic_weather_night_rain)
    53 -> if (isDay) WeatherInfo(R.string.wc_moderate_drizzle, "🌦️")
          else WeatherInfo(R.string.wc_moderate_drizzle, iconRes = R.drawable.ic_weather_night_rain)
    55 -> if (isDay) WeatherInfo(R.string.wc_dense_drizzle, "🌧️")
          else WeatherInfo(R.string.wc_dense_drizzle, iconRes = R.drawable.ic_weather_night_rain)
    56, 57 -> if (isDay) WeatherInfo(R.string.wc_freezing_drizzle, "🌧️")
              else WeatherInfo(R.string.wc_freezing_drizzle, iconRes = R.drawable.ic_weather_night_rain)
    61 -> if (isDay) WeatherInfo(R.string.wc_light_rain, "🌦️")
          else WeatherInfo(R.string.wc_light_rain, iconRes = R.drawable.ic_weather_night_rain)
    63 -> if (isDay) WeatherInfo(R.string.wc_moderate_rain, "🌧️")
          else WeatherInfo(R.string.wc_moderate_rain, iconRes = R.drawable.ic_weather_night_rain)
    65 -> if (isDay) WeatherInfo(R.string.wc_heavy_rain, "🌧️")
          else WeatherInfo(R.string.wc_heavy_rain, iconRes = R.drawable.ic_weather_night_rain)
    66, 67 -> if (isDay) WeatherInfo(R.string.wc_freezing_rain, "🌧️")
              else WeatherInfo(R.string.wc_freezing_rain, iconRes = R.drawable.ic_weather_night_rain)
    71 -> if (isDay) WeatherInfo(R.string.wc_light_snow, "🌨️")
          else WeatherInfo(R.string.wc_light_snow, iconRes = R.drawable.ic_weather_night_snow)
    73 -> if (isDay) WeatherInfo(R.string.wc_moderate_snow, "🌨️")
          else WeatherInfo(R.string.wc_moderate_snow, iconRes = R.drawable.ic_weather_night_snow)
    75 -> if (isDay) WeatherInfo(R.string.wc_heavy_snow, "❄️")
          else WeatherInfo(R.string.wc_heavy_snow, iconRes = R.drawable.ic_weather_night_snow)
    77 -> if (isDay) WeatherInfo(R.string.wc_snow_grains, "❄️")
          else WeatherInfo(R.string.wc_snow_grains, iconRes = R.drawable.ic_weather_night_snow)
    80 -> if (isDay) WeatherInfo(R.string.wc_light_showers, "🌦️")
          else WeatherInfo(R.string.wc_light_showers, iconRes = R.drawable.ic_weather_night_rain)
    81 -> if (isDay) WeatherInfo(R.string.wc_moderate_showers, "🌧️")
          else WeatherInfo(R.string.wc_moderate_showers, iconRes = R.drawable.ic_weather_night_rain)
    82 -> if (isDay) WeatherInfo(R.string.wc_violent_showers, "🌧️")
          else WeatherInfo(R.string.wc_violent_showers, iconRes = R.drawable.ic_weather_night_rain)
    85 -> if (isDay) WeatherInfo(R.string.wc_light_snow_showers, "🌨️")
          else WeatherInfo(R.string.wc_light_snow_showers, iconRes = R.drawable.ic_weather_night_snow)
    86 -> if (isDay) WeatherInfo(R.string.wc_heavy_snow_showers, "❄️")
          else WeatherInfo(R.string.wc_heavy_snow_showers, iconRes = R.drawable.ic_weather_night_snow)
    95 -> WeatherInfo(R.string.wc_thunderstorm, "⛈️")
    96, 99 -> WeatherInfo(R.string.wc_thunderstorm_hail, "⛈️")
    else -> WeatherInfo(R.string.wc_unknown, "❓")
}

fun windDirectionToArrow(degrees: Int): String = when ((degrees + 22) / 45 % 8) {
    0 -> "↓ N"
    1 -> "↙ NE"
    2 -> "← E"
    3 -> "↖ SE"
    4 -> "↑ S"
    5 -> "↗ SW"
    6 -> "→ W"
    7 -> "↘ NW"
    else -> ""
}
