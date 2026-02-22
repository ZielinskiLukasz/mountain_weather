package com.example.mountainweather.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mountainweather.data.OpenMeteoApi
import com.example.mountainweather.data.local.AppDatabase
import com.example.mountainweather.data.repository.SettingsRepository
import com.example.mountainweather.data.repository.WeatherRepository
import kotlinx.coroutines.flow.first

class WeatherSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val api = OpenMeteoApi.create()
        val repository = WeatherRepository(
            api, db.weatherDao(), db.hourlyForecastDao(), db.dailyForecastDao()
        )
        val settingsRepo = SettingsRepository(applicationContext)
        val settings = settingsRepo.forecastSettings.first()

        val favorites = db.savedLocationDao().getFavorites()
        if (favorites.isEmpty()) return Result.success()

        var allOk = true
        for (location in favorites) {
            try {
                repository.refreshWeather(location.latitude, location.longitude, location.name)

                if (settings.showHourly) {
                    repository.refreshHourlyForecast(location.latitude, location.longitude)
                }
                val days = when {
                    settings.showDaily5 -> 5
                    settings.showDaily3 -> 3
                    else -> 0
                }
                if (days > 0) {
                    repository.refreshDailyForecast(location.latitude, location.longitude, days)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync failed for ${location.name}: ${e.message}")
                allOk = false
            }
        }

        val saved = settingsRepo.getLastLocation()
        if (saved != null) {
            val alreadySynced = favorites.any { loc ->
                kotlin.math.abs(loc.latitude - saved.latitude) < 0.005 &&
                        kotlin.math.abs(loc.longitude - saved.longitude) < 0.005
            }
            if (!alreadySynced) {
                try {
                    repository.refreshWeather(saved.latitude, saved.longitude, saved.name)
                    if (settings.showHourly) {
                        repository.refreshHourlyForecast(saved.latitude, saved.longitude)
                    }
                    val days = when {
                        settings.showDaily5 -> 5
                        settings.showDaily3 -> 3
                        else -> 0
                    }
                    if (days > 0) {
                        repository.refreshDailyForecast(saved.latitude, saved.longitude, days)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sync failed for last location ${saved.name}: ${e.message}")
                    allOk = false
                }
            }
        }

        return if (allOk) Result.success() else Result.retry()
    }

    companion object {
        const val TAG = "WeatherSyncWorker"
        const val WORK_NAME = "weather_background_sync"
    }
}
