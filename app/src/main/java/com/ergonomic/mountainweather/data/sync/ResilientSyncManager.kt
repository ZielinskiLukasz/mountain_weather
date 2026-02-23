package com.ergonomic.mountainweather.data.sync

import com.ergonomic.mountainweather.data.local.DailyForecastEntity
import com.ergonomic.mountainweather.data.local.HourlyForecastEntity
import com.ergonomic.mountainweather.data.local.WeatherEntity
import com.ergonomic.mountainweather.data.repository.ForecastSettings
import com.ergonomic.mountainweather.data.repository.WeatherRepository

data class SyncResult(
    val currentWeather: Result<WeatherEntity>? = null,
    val hourlyForecast: Result<List<HourlyForecastEntity>>? = null,
    val dailyForecast: Result<List<DailyForecastEntity>>? = null
)

class ResilientSyncManager(
    private val repository: WeatherRepository
) {
    val circuitBreaker = CircuitBreaker(failureThreshold = 3, resetTimeoutMs = 60_000)
    private val retryPolicy = RetryPolicy(maxRetries = 3, initialDelayMs = 1000, maxDelayMs = 30_000)

    suspend fun syncAll(
        latitude: Double,
        longitude: Double,
        locationName: String,
        settings: ForecastSettings
    ): SyncResult {
        val currentResult = syncCurrent(latitude, longitude, locationName)

        val hourlyResult = if (settings.showHourly) {
            syncHourly(latitude, longitude)
        } else null

        val dailyDays = when {
            settings.showDaily5 -> 5
            settings.showDaily3 -> 3
            else -> 0
        }
        val dailyResult = if (dailyDays > 0) {
            syncDaily(latitude, longitude, dailyDays)
        } else null

        return SyncResult(currentResult, hourlyResult, dailyResult)
    }

    private suspend fun syncCurrent(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): Result<WeatherEntity> {
        return try {
            val result = circuitBreaker.execute {
                retryPolicy.execute {
                    repository.refreshWeather(latitude, longitude, locationName).getOrThrow()
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            repository.refreshWeather(latitude, longitude, locationName)
        }
    }

    private suspend fun syncHourly(
        latitude: Double,
        longitude: Double
    ): Result<List<HourlyForecastEntity>> {
        return try {
            val result = circuitBreaker.execute {
                retryPolicy.execute {
                    repository.refreshHourlyForecast(latitude, longitude).getOrThrow()
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            repository.refreshHourlyForecast(latitude, longitude)
        }
    }

    private suspend fun syncDaily(
        latitude: Double,
        longitude: Double,
        days: Int
    ): Result<List<DailyForecastEntity>> {
        return try {
            val result = circuitBreaker.execute {
                retryPolicy.execute {
                    repository.refreshDailyForecast(latitude, longitude, days).getOrThrow()
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            repository.refreshDailyForecast(latitude, longitude, days)
        }
    }
}
