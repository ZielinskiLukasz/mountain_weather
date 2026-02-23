package com.ergonomic.mountainweather.data.local

import androidx.room.Entity

@Entity(
    tableName = "hourly_forecast",
    primaryKeys = ["locationKey", "time"]
)
data class HourlyForecastEntity(
    val locationKey: String,
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val precipitation: Double,
    val cachedAt: Long = System.currentTimeMillis()
)
