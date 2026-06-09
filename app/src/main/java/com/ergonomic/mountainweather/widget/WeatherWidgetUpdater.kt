package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll

object WeatherWidgetUpdater {

    private const val TAG = "WidgetUpdater"

    suspend fun refreshAll(context: Context) {
        val appCtx = context.applicationContext
        try {
            WeatherMinimalWidget().updateAll(appCtx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh minimal widget: ${e.message}")
        }
    }
}
