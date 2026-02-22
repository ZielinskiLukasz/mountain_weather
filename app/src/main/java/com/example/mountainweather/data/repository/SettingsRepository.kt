package com.example.mountainweather.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ForecastSettings(
    val showHourly: Boolean = true,
    val showDaily3: Boolean = false,
    val showDaily5: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SHOW_HOURLY = booleanPreferencesKey("show_hourly")
        val SHOW_DAILY_3 = booleanPreferencesKey("show_daily_3")
        val SHOW_DAILY_5 = booleanPreferencesKey("show_daily_5")
    }

    val forecastSettings: Flow<ForecastSettings> = context.dataStore.data.map { prefs ->
        ForecastSettings(
            showHourly = prefs[Keys.SHOW_HOURLY] ?: true,
            showDaily3 = prefs[Keys.SHOW_DAILY_3] ?: false,
            showDaily5 = prefs[Keys.SHOW_DAILY_5] ?: true
        )
    }

    suspend fun setShowHourly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HOURLY] = enabled }
    }

    suspend fun setShowDaily3(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_DAILY_3] = enabled }
    }

    suspend fun setShowDaily5(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_DAILY_5] = enabled }
    }
}
