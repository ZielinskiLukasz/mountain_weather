package com.ergonomic.mountainweather.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.ergonomic.mountainweather.MainActivity

class CycleDailyAction : ActionCallback {

    private val tag = "CycleDailyAction"

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appCtx = context.applicationContext
        try {
            val dayCount = WidgetDailyDataLoader.dayCountForCurrentLocation(appCtx)
            if (dayCount <= 0) {
                openApp(appCtx)
                return
            }
            val manager = GlanceAppWidgetManager(appCtx)
            val sizes = manager.getAppWidgetSizes(glanceId)
            val localWidth = sizes.minOfOrNull { it.width.value }
                ?: sizes.firstOrNull()?.width?.value
                ?: 0f
            val widthDp = WidgetDailyWindow.resolveLayoutWidthDp(appCtx, glanceId, localWidth)
            val columns = WidgetDailyWindow.visibleColumnCount(widthDp)
            var nextIndex = 0
            updateAppWidgetState(appCtx, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                val current = prefs[WidgetDailyKeys.DAY_INDEX] ?: 0
                nextIndex = WidgetDailyWindow.nextStartIndex(current, dayCount, columns)
                prefs.toMutablePreferences().apply {
                    this[WidgetDailyKeys.DAY_INDEX] = nextIndex
                }
            }
            Log.d(tag, "onAction: start -> $nextIndex (days=$dayCount columns=$columns)")
            WeatherDailyWidget().update(appCtx, glanceId)
        } catch (e: Exception) {
            Log.w(tag, "Cycle daily failed: ${e.message}", e)
        }
    }

    private fun openApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}
