package com.ergonomic.mountainweather.data.local

import androidx.room.Entity

@Entity(
    tableName = "daily_forecast",
    primaryKeys = ["locationKey", "date"]
)
data class DailyForecastEntity(
    val locationKey: String,
    val date: String,
    val weatherCode: Int,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val precipitationSum: Double,
    val windSpeedMax: Double,
    val cachedAt: Long = System.currentTimeMillis()
)
