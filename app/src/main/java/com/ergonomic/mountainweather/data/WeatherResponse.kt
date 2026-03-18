package com.ergonomic.mountainweather.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather,
    val daily: DailyData? = null,
    val hourly: HourlyData? = null
)

data class CurrentWeather(
    val time: String,
    @SerializedName("temperature_2m")
    val temperature: Double,
    @SerializedName("apparent_temperature")
    val apparentTemperature: Double,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("wind_speed_10m")
    val windSpeed: Double,
    @SerializedName("wind_direction_10m")
    val windDirection: Int,
    @SerializedName("relative_humidity_2m")
    val humidity: Int,
    val precipitation: Double,
    @SerializedName("pressure_msl")
    val pressure: Double,
    @SerializedName("cloud_cover")
    val cloudCover: Int? = null,
    @SerializedName("wind_gusts_10m")
    val windGusts: Double? = null,
    val snowfall: Double? = null,
    val rain: Double? = null,
    @SerializedName("is_day")
    val isDay: Int? = null
)

data class HourlyForecastResponse(
    val hourly: HourlyData
)

data class HourlyData(
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperature: List<Double>? = null,
    @SerializedName("weather_code")
    val weatherCode: List<Int>? = null,
    val precipitation: List<Double>? = null,
    @SerializedName("dew_point_2m")
    val dewPoint: List<Double>? = null,
    val visibility: List<Double>? = null,
    @SerializedName("freezing_level_height")
    val freezingLevelHeight: List<Double>? = null
)

data class DailyForecastResponse(
    val daily: DailyData
)

data class DailyData(
    val time: List<String>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>? = null,
    @SerializedName("temperature_2m_max")
    val temperatureMax: List<Double>? = null,
    @SerializedName("temperature_2m_min")
    val temperatureMin: List<Double>? = null,
    @SerializedName("precipitation_sum")
    val precipitationSum: List<Double>? = null,
    @SerializedName("wind_speed_10m_max")
    val windSpeedMax: List<Double>? = null,
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
    @SerializedName("uv_index_max")
    val uvIndexMax: List<Double>? = null,
    @SerializedName("rain_sum")
    val rainSum: List<Double>? = null,
    @SerializedName("showers_sum")
    val showersSum: List<Double>? = null,
    @SerializedName("snowfall_sum")
    val snowfallSum: List<Double>? = null,
    @SerializedName("precipitation_hours")
    val precipitationHours: List<Double>? = null,
    @SerializedName("precipitation_probability_max")
    val precipitationProbabilityMax: List<Int>? = null,
    @SerializedName("sunshine_duration")
    val sunshineDuration: List<Double>? = null,
    @SerializedName("wind_gusts_10m_max")
    val windGustsMax: List<Double>? = null,
    @SerializedName("wind_direction_10m_dominant")
    val windDirectionDominant: List<Int>? = null
)
