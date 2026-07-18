package com.ergonomic.mountainweather.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ergonomic.mountainweather.data.sync.SyncScheduler

class WeatherSunReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherSunWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SyncScheduler.runOnce(context)
    }
}
