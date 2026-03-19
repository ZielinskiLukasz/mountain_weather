package com.ergonomic.mountainweather.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ergonomic.mountainweather.util.WeatherParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ForecastSettings(
    val showHourly: Boolean = true,
    val showDaily3: Boolean = false,
    val showDaily5: Boolean = true,
    val showDaily7: Boolean = false,
    val showDaily14: Boolean = false,
    val resilientSync: Boolean = false,
    val syncIntervalMinutes: Int = 0,
    val enabledCurrentParams: Set<String> = WeatherParams.DEFAULTS,
    val paramOrder: List<String> = WeatherParams.ALL.map { it.key }
) {
    val dailyForecastDays: Int get() = when {
        showDaily14 -> 14
        showDaily7 -> 7
        showDaily5 -> 5
        showDaily3 -> 3
        else -> 0
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SHOW_HOURLY = booleanPreferencesKey("show_hourly")
        val SHOW_DAILY_3 = booleanPreferencesKey("show_daily_3")
        val SHOW_DAILY_5 = booleanPreferencesKey("show_daily_5")
        val SHOW_DAILY_7 = booleanPreferencesKey("show_daily_7")
        val SHOW_DAILY_14 = booleanPreferencesKey("show_daily_14")
        val RESILIENT_SYNC = booleanPreferencesKey("resilient_sync")
        val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val LAST_LOCATION_NAME = stringPreferencesKey("last_location_name")
        val LAST_LOCATION_LAT = doublePreferencesKey("last_location_lat")
        val LAST_LOCATION_LON = doublePreferencesKey("last_location_lon")
        val ENABLED_CURRENT_PARAMS = stringSetPreferencesKey("enabled_current_params")
        val PARAM_ORDER = stringPreferencesKey("param_order")
    }

    val forecastSettings: Flow<ForecastSettings> = context.dataStore.data.map { prefs ->
        ForecastSettings(
            showHourly = prefs[Keys.SHOW_HOURLY] ?: true,
            showDaily3 = prefs[Keys.SHOW_DAILY_3] ?: false,
            showDaily5 = prefs[Keys.SHOW_DAILY_5] ?: true,
            showDaily7 = prefs[Keys.SHOW_DAILY_7] ?: false,
            showDaily14 = prefs[Keys.SHOW_DAILY_14] ?: false,
            resilientSync = prefs[Keys.RESILIENT_SYNC] ?: false,
            syncIntervalMinutes = prefs[Keys.SYNC_INTERVAL_MINUTES] ?: 0,
            enabledCurrentParams = prefs[Keys.ENABLED_CURRENT_PARAMS] ?: WeatherParams.DEFAULTS,
            paramOrder = prefs[Keys.PARAM_ORDER]?.split(",")?.filter { it.isNotBlank() }
                ?: WeatherParams.ALL.map { it.key }
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

    suspend fun setDailyMode(days: Int) {
        context.dataStore.edit {
            it[Keys.SHOW_DAILY_3] = days == 3
            it[Keys.SHOW_DAILY_5] = days == 5
            it[Keys.SHOW_DAILY_7] = days == 7
            it[Keys.SHOW_DAILY_14] = days == 14
        }
    }

    suspend fun setResilientSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESILIENT_SYNC] = enabled }
    }

    suspend fun setSyncInterval(minutes: Int) {
        context.dataStore.edit { it[Keys.SYNC_INTERVAL_MINUTES] = minutes }
    }

    suspend fun saveParamOrder(order: List<String>) {
        context.dataStore.edit { it[Keys.PARAM_ORDER] = order.joinToString(",") }
    }

    suspend fun toggleCurrentParam(key: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_CURRENT_PARAMS] ?: WeatherParams.DEFAULTS
            prefs[Keys.ENABLED_CURRENT_PARAMS] = if (key in current) current - key else current + key
        }
    }

    suspend fun saveLastLocation(name: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[Keys.LAST_LOCATION_NAME] = name
            it[Keys.LAST_LOCATION_LAT] = lat
            it[Keys.LAST_LOCATION_LON] = lon
        }
    }

    data class SavedLocation(val name: String, val latitude: Double, val longitude: Double)

    suspend fun getLastLocation(): SavedLocation? {
        val prefs = context.dataStore.data.first()
        val name = prefs[Keys.LAST_LOCATION_NAME] ?: return null
        val lat = prefs[Keys.LAST_LOCATION_LAT] ?: return null
        val lon = prefs[Keys.LAST_LOCATION_LON] ?: return null
        return SavedLocation(name, lat, lon)
    }
}
