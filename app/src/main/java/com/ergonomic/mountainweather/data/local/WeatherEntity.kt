package com.ergonomic.mountainweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey
    val locationKey: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val apparentTemperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val humidity: Int,
    val precipitation: Double,
    val pressure: Double,
    val time: String,
    val cachedAt: Long = System.currentTimeMillis(),
    /** 1 = day, 0 = night (from Open-Meteo `is_day`). */
    val isDay: Int? = null,
    val cloudCover: Int? = null,
    val windGusts: Double? = null,
    val snowfall: Double? = null,
    val rain: Double? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val uvIndexMax: Double? = null,
    val rainSum: Double? = null,
    val showersSum: Double? = null,
    val snowfallSum: Double? = null,
    val precipitationHours: Double? = null,
    val precipitationProbabilityMax: Int? = null,
    val sunshineDuration: Double? = null,
    val windGustsMax: Double? = null,
    val dominantWindDirection: Int? = null,
    val dewPoint: Double? = null,
    val visibility: Double? = null,
    val freezingLevelHeight: Double? = null,
    val temperatureMax: Double? = null,
    val temperatureMin: Double? = null,
    val aqiEu: Int? = null,
    val aqiUs: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null,
    val elevation: Double? = null
)
