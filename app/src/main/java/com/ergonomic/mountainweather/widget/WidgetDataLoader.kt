package com.ergonomic.mountainweather.widget

import android.content.Context
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository

data class WidgetSnapshot(
    val locationName: String,
    val temperature: Double,
    val weatherCode: Int,
    val cachedAt: Long
)

object WidgetDataLoader {

    suspend fun loadCurrent(context: Context): WidgetSnapshot? {
        val appCtx = context.applicationContext
        val loc = SettingsRepository(appCtx).getLastLocation() ?: return null
        val key = WeatherRepository.locationKey(loc.latitude, loc.longitude)
        val w = AppDatabase.getInstance(appCtx).weatherDao().getWeather(key) ?: return null
        return WidgetSnapshot(
            locationName = loc.name,
            temperature = w.temperature,
            weatherCode = w.weatherCode,
            cachedAt = w.cachedAt
        )
    }
}
