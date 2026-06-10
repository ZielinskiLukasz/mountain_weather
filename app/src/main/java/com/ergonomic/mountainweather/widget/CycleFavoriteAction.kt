package com.ergonomic.mountainweather.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.repository.SettingsRepository

class CycleFavoriteAction : ActionCallback {

    private val tag = "CycleFavoriteAction"

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appCtx = context.applicationContext
        Log.d(tag, "onAction: triggered for glanceId=$glanceId")
        try {
            val favorites = AppDatabase.getInstance(appCtx).savedLocationDao().getFavorites()
            Log.d(tag, "onAction: favorites count=${favorites.size}")
            if (favorites.isEmpty()) {
                Log.d(tag, "onAction: no favorites -> opening app")
                openApp(appCtx)
                return
            }
            val settingsRepo = SettingsRepository(appCtx)
            val current = settingsRepo.getLastLocation()
            val currentIdx = current?.let {
                favorites.indexOfFirst { fav ->
                    kotlin.math.abs(fav.latitude - it.latitude) < 0.005 &&
                            kotlin.math.abs(fav.longitude - it.longitude) < 0.005
                }
            } ?: -1
            val nextIdx = if (currentIdx < 0 || currentIdx + 1 >= favorites.size) 0 else currentIdx + 1
            val next = favorites[nextIdx]
            Log.d(
                tag,
                "onAction: current=${current?.name} (idx=$currentIdx) -> next=${next.name} (idx=$nextIdx)"
            )
            settingsRepo.saveLastLocation(next.name, next.latitude, next.longitude)
            WeatherWidgetUpdater.refreshAll(appCtx)
            Log.d(tag, "onAction: refreshAll completed for ${next.name}")
        } catch (e: Exception) {
            Log.w(tag, "Cycle favorite failed: ${e.message}", e)
        }
    }

    private fun openApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}
