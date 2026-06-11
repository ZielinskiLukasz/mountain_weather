package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll

object WeatherWidgetUpdater {

    private const val TAG = "WidgetUpdater"

    suspend fun refreshAll(context: Context) {
        val appCtx = context.applicationContext

        runCatching { WeatherMinimalWidget().updateAll(appCtx) }
            .onSuccess { Log.d(TAG, "Minimal updateAll OK") }
            .onFailure { Log.w(TAG, "Minimal updateAll failed: ${it.message}", it) }

        runCatching { WeatherDailyWidget().updateAll(appCtx) }
            .onSuccess { Log.d(TAG, "Daily updateAll OK") }
            .onFailure { Log.w(TAG, "Daily updateAll failed: ${it.message}", it) }

        runCatching { WeatherCurrentWidget().updateAll(appCtx) }
            .onSuccess { Log.d(TAG, "Current updateAll OK") }
            .onFailure { Log.w(TAG, "Current updateAll failed: ${it.message}", it) }

        runCatching { WeatherParamsWidget().updateAll(appCtx) }
            .onSuccess { Log.d(TAG, "Params updateAll OK") }
            .onFailure { Log.w(TAG, "Params updateAll failed: ${it.message}", it) }

        broadcastUpdate(appCtx, WeatherMinimalReceiver::class.java)
        broadcastUpdate(appCtx, WeatherDailyReceiver::class.java)
        broadcastUpdate(appCtx, WeatherCurrentReceiver::class.java)
        broadcastUpdate(appCtx, WeatherParamsReceiver::class.java)
    }

    private fun broadcastUpdate(appCtx: Context, receiver: Class<*>) {
        runCatching {
            val mgr = AppWidgetManager.getInstance(appCtx)
            val component = ComponentName(appCtx, receiver)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(appCtx, receiver).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appCtx.sendBroadcast(intent)
                Log.d(TAG, "Broadcast APPWIDGET_UPDATE sent to ${ids.size} ${receiver.simpleName} widget(s)")
            }
        }.onFailure { Log.w(TAG, "Broadcast update failed for ${receiver.simpleName}: ${it.message}", it) }
    }
}
