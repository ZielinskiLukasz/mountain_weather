package com.example.mountainweather.data.repository

import com.example.mountainweather.data.OpenMeteoApi
import com.example.mountainweather.data.local.WeatherDao
import com.example.mountainweather.data.local.WeatherEntity
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val api: OpenMeteoApi,
    private val dao: WeatherDao
) {
    fun observeCachedWeather(locationKey: String): Flow<WeatherEntity?> =
        dao.observeWeather(locationKey)

    suspend fun refreshWeather(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): Result<WeatherEntity> {
        val key = locationKey(latitude, longitude)
        return try {
            val response = api.getCurrentWeather(latitude, longitude)
            val entity = WeatherEntity(
                locationKey = key,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                temperature = response.current.temperature,
                apparentTemperature = response.current.apparentTemperature,
                weatherCode = response.current.weatherCode,
                windSpeed = response.current.windSpeed,
                windDirection = response.current.windDirection,
                humidity = response.current.humidity,
                precipitation = response.current.precipitation,
                pressure = response.current.pressure,
                time = response.current.time,
                cachedAt = System.currentTimeMillis()
            )
            dao.insertWeather(entity)
            Result.success(entity)
        } catch (e: Exception) {
            val cached = dao.getWeather(key)
            if (cached != null) {
                Result.failure(CachedDataException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    companion object {
        fun locationKey(lat: Double, lon: Double): String =
            "%.2f_%.2f".format(lat, lon)
    }
}

class CachedDataException(
    cause: Exception,
    val cachedData: WeatherEntity
) : Exception("Network error, using cached data", cause)
