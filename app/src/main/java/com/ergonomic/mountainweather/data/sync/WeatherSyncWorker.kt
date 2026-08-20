package com.ergonomic.mountainweather.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ergonomic.mountainweather.data.OpenMeteoApi
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.repository.SettingsRepository
import com.ergonomic.mountainweather.data.repository.WeatherRepository
import com.ergonomic.mountainweather.widget.WeatherWidgetUpdater
import com.ergonomic.mountainweather.widget.WidgetDataRequirements
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
        val extraDaily = WidgetDataRequirements.extraDailyFields(applicationContext)

        var allOk = true
        for (location in favorites) {
            try {
                repository.refreshAll(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = location.name,
                    enabledParams = settings.enabledCurrentParams,
                    showHourly = settings.showHourly,
                    dailyDays = settings.dailyForecastDays,
                    extraDailyFields = extraDaily
                )
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
                    repository.refreshAll(
                        latitude = saved.latitude,
                        longitude = saved.longitude,
                        locationName = saved.name,
                        enabledParams = settings.enabledCurrentParams,
                        showHourly = settings.showHourly,
                        dailyDays = settings.dailyForecastDays,
                        extraDailyFields = extraDaily
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Sync failed for last location ${saved.name}: ${e.message}")
                    allOk = false
                }
            }
        }

        WeatherWidgetUpdater.refreshAll(applicationContext)

        return if (allOk) Result.success() else Result.retry()
    }

    companion object {
        const val TAG = "WeatherSyncWorker"
        const val WORK_NAME = "weather_background_sync"
    }
}
