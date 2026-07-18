package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Computes extra API fields required by currently installed widgets that are
 * outside the user's configured parameter set (i.e. widget-only data needs).
 */
object WidgetDataRequirements {

    fun extraDailyFields(context: Context): Set<String> {
        val ctx = context.applicationContext
        val fields = mutableSetOf<String>()
        if (hasActiveWidget(ctx, WeatherSunReceiver::class.java)) {
            fields.add("sunrise")
            fields.add("sunset")
            fields.add("uv_index_max")
        }
        return fields
    }

    private fun hasActiveWidget(context: Context, receiver: Class<*>): Boolean {
        return try {
            val mgr = AppWidgetManager.getInstance(context)
            mgr.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
