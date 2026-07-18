package com.ergonomic.mountainweather.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    val INTERVAL_OPTIONS = listOf(0, 15, 30, 60, 180, 360, 720)

    fun enable(context: Context, intervalMinutes: Int) {
        if (intervalMinutes <= 0) {
            disable(context)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val flexMinutes = (intervalMinutes / 4).coerceAtLeast(5).toLong()

        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
            flexMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeatherSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WeatherSyncWorker.WORK_NAME)
    }

    fun runOnce(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<WeatherSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "weather_oneshot_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
