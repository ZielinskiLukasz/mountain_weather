package com.example.mountainweather.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    fun enable(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            3, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
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
}
