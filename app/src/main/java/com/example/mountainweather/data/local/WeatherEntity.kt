package com.example.mountainweather.data.local

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
    val cachedAt: Long = System.currentTimeMillis()
)
