package com.ergonomic.mountainweather.data.repository

import com.ergonomic.mountainweather.data.OpenMeteoApi
import com.ergonomic.mountainweather.data.local.DailyForecastDao
import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.HourlyForecastDao
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.WeatherDao
import com.ergonomic.mountainweather.data.local.WeatherEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class WeatherRepository(
    private val api: OpenMeteoApi,
    private val dao: WeatherDao,
    private val hourlyDao: HourlyForecastDao,
    private val dailyDao: DailyForecastDao
) {
    fun observeCachedWeather(locationKey: String): Flow<WeatherEntity?> =
        dao.observeWeather(locationKey)

    fun observeHourlyForecast(locationKey: String): Flow<List<HourlyForecastEntity>> =
        hourlyDao.observe(locationKey)

    fun observeDailyForecast(locationKey: String): Flow<List<DailyForecastEntity>> =
        dailyDao.observe(locationKey)

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

    suspend fun refreshHourlyForecast(
        latitude: Double,
        longitude: Double
    ): Result<List<HourlyForecastEntity>> {
        val key = locationKey(latitude, longitude)
        return try {
            val response = api.getHourlyForecast(latitude, longitude)
            val now = System.currentTimeMillis()
            val entities = response.hourly.time.indices.map { i ->
                HourlyForecastEntity(
                    locationKey = key,
                    time = response.hourly.time[i],
                    temperature = response.hourly.temperature[i],
                    weatherCode = response.hourly.weatherCode[i],
                    precipitation = response.hourly.precipitation[i],
                    cachedAt = now
                )
            }
            hourlyDao.replaceForLocation(key, entities)
            Result.success(entities)
        } catch (e: Exception) {
            val cached = hourlyDao.getAll(key)
            if (cached.isNotEmpty()) {
                Result.failure(CachedHourlyException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun refreshDailyForecast(
        latitude: Double,
        longitude: Double,
        days: Int
    ): Result<List<DailyForecastEntity>> {
        val key = locationKey(latitude, longitude)
        return try {
            val response = api.getDailyForecast(latitude, longitude, forecastDays = days)
            val now = System.currentTimeMillis()
            val entities = response.daily.time.indices.map { i ->
                DailyForecastEntity(
                    locationKey = key,
                    date = response.daily.time[i],
                    weatherCode = response.daily.weatherCode[i],
                    temperatureMax = response.daily.temperatureMax[i],
                    temperatureMin = response.daily.temperatureMin[i],
                    precipitationSum = response.daily.precipitationSum[i],
                    windSpeedMax = response.daily.windSpeedMax[i],
                    cachedAt = now
                )
            }
            dailyDao.replaceForLocation(key, entities)
            Result.success(entities)
        } catch (e: Exception) {
            val cached = dailyDao.getAll(key)
            if (cached.isNotEmpty()) {
                Result.failure(CachedDailyException(e, cached))
            } else {
                Result.failure(e)
            }
        }
    }

    companion object {
        fun locationKey(lat: Double, lon: Double): String =
            String.format(Locale.US, "%.2f_%.2f", lat, lon)
    }
}

class CachedDataException(
    cause: Exception,
    val cachedData: WeatherEntity
) : Exception("Network error, using cached data", cause)

class CachedHourlyException(
    cause: Exception,
    val cachedData: List<HourlyForecastEntity>
) : Exception("Network error, using cached hourly data", cause)

class CachedDailyException(
    cause: Exception,
    val cachedData: List<DailyForecastEntity>
) : Exception("Network error, using cached daily data", cause)
