package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll

object WeatherWidgetUpdater {

    private const val TAG = "WidgetUpdater"

    /**
     * Trigger a fresh render of every widget instance.
     *
     * The composition observes DataStore + Room directly via collectAsState, so
     * while it is alive most updates propagate automatically. This call is the
     * "kick" needed when the Glance worker has been idle long enough to die
     * (~45s after the last activity) — calling update() spins it back up and
     * provideGlance runs again, which re-attaches the collector to live data.
     */
    suspend fun refreshAll(context: Context) {
        val appCtx = context.applicationContext

        runCatching { WeatherMinimalWidget().updateAll(appCtx) }
            .onSuccess { Log.d(TAG, "Glance updateAll OK") }
            .onFailure { Log.w(TAG, "Glance updateAll failed: ${it.message}", it) }

        // Belt-and-braces broadcast for launchers (notably some Android 12 OEM
        // builds) where Glance's internal update doesn't reliably propagate.
        runCatching {
            val mgr = AppWidgetManager.getInstance(appCtx)
            val component = ComponentName(appCtx, WeatherMinimalReceiver::class.java)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(appCtx, WeatherMinimalReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appCtx.sendBroadcast(intent)
                Log.d(TAG, "Broadcast APPWIDGET_UPDATE sent to ${ids.size} widget(s)")
            }
        }.onFailure { Log.w(TAG, "Broadcast update failed: ${it.message}", it) }
    }
}
