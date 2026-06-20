package com.ergonomic.mountainweather.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

/**
 * Per-`appWidgetId` configuration for configurable widgets (Compact and future ones).
 * A widget with no pinned location follows the main app (`lastLocation`).
 */
object WidgetPrefs {

    data class Pin(val name: String, val latitude: Double, val longitude: Double)

    enum class Theme { SYSTEM, LIGHT, DARK }

    private fun latKey(id: Int) = doublePreferencesKey("pin_lat_$id")
    private fun lonKey(id: Int) = doublePreferencesKey("pin_lon_$id")
    private fun nameKey(id: Int) = stringPreferencesKey("pin_name_$id")
    private fun paramsKey(id: Int) = stringSetPreferencesKey("params_$id")
    private fun themeKey(id: Int) = stringPreferencesKey("theme_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")

    // Globalne (nie per-id) ostatnio użyte ustawienia, jako preselekcja przy nowej instancji.
    private val LAST_USED_PARAMS = stringSetPreferencesKey("last_used_params")
    private val LAST_USED_THEME = stringPreferencesKey("last_used_theme")
    private val LAST_USED_OPACITY = intPreferencesKey("last_used_opacity")

    /** Default extra parameters shown on Compact widget when user has not chosen any. */
    val DEFAULT_PARAMS: Set<String> = setOf(
        com.ergonomic.mountainweather.util.WeatherParams.WIND,
        com.ergonomic.mountainweather.util.WeatherParams.HUMIDITY
    )

    /** Default background opacity (matches the legacy 0xA6 alpha in widget_background.xml ≈ 65%). */
    const val DEFAULT_OPACITY: Int = 65

    fun observePin(context: Context, appWidgetId: Int): Flow<Pin?> {
        val store = context.applicationContext.widgetDataStore
        return store.data.map { prefs ->
            val lat = prefs[latKey(appWidgetId)] ?: return@map null
            val lon = prefs[lonKey(appWidgetId)] ?: return@map null
            val name = prefs[nameKey(appWidgetId)] ?: return@map null
            Pin(name, lat, lon)
        }
    }

    suspend fun getPin(context: Context, appWidgetId: Int): Pin? =
        observePin(context, appWidgetId).first()

    suspend fun savePin(context: Context, appWidgetId: Int, pin: Pin) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs[latKey(appWidgetId)] = pin.latitude
            prefs[lonKey(appWidgetId)] = pin.longitude
            prefs[nameKey(appWidgetId)] = pin.name
        }
    }

    suspend fun clearPin(context: Context, appWidgetId: Int) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs.remove(latKey(appWidgetId))
            prefs.remove(lonKey(appWidgetId))
            prefs.remove(nameKey(appWidgetId))
        }
    }

    fun observeParams(context: Context, appWidgetId: Int): Flow<Set<String>?> {
        return context.applicationContext.widgetDataStore.data.map { prefs ->
            prefs[paramsKey(appWidgetId)]
        }
    }

    suspend fun getParams(context: Context, appWidgetId: Int): Set<String>? =
        observeParams(context, appWidgetId).first()

    suspend fun saveParams(context: Context, appWidgetId: Int, params: Set<String>) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs[paramsKey(appWidgetId)] = params
        }
    }

    fun observeTheme(context: Context, appWidgetId: Int): Flow<Theme> {
        return context.applicationContext.widgetDataStore.data.map { prefs ->
            prefs[themeKey(appWidgetId)]?.let {
                runCatching { Theme.valueOf(it) }.getOrNull()
            } ?: Theme.SYSTEM
        }
    }

    suspend fun getTheme(context: Context, appWidgetId: Int): Theme =
        observeTheme(context, appWidgetId).first()

    suspend fun saveTheme(context: Context, appWidgetId: Int, theme: Theme) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs[themeKey(appWidgetId)] = theme.name
        }
    }

    fun observeOpacity(context: Context, appWidgetId: Int): Flow<Int> {
        return context.applicationContext.widgetDataStore.data.map { prefs ->
            (prefs[opacityKey(appWidgetId)] ?: DEFAULT_OPACITY).coerceIn(0, 100)
        }
    }

    suspend fun getOpacity(context: Context, appWidgetId: Int): Int =
        observeOpacity(context, appWidgetId).first()

    suspend fun saveOpacity(context: Context, appWidgetId: Int, opacity: Int) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs[opacityKey(appWidgetId)] = opacity.coerceIn(0, 100)
        }
    }

    /** Read last-used global defaults (used to preselect form when adding a new widget instance). */
    suspend fun getLastUsedDefaults(context: Context): Defaults {
        val prefs = context.applicationContext.widgetDataStore.data.first()
        val theme = prefs[LAST_USED_THEME]?.let { runCatching { Theme.valueOf(it) }.getOrNull() }
            ?: Theme.SYSTEM
        val opacity = (prefs[LAST_USED_OPACITY] ?: DEFAULT_OPACITY).coerceIn(0, 100)
        val params = prefs[LAST_USED_PARAMS] ?: DEFAULT_PARAMS
        return Defaults(params = params, theme = theme, opacity = opacity)
    }

    suspend fun saveLastUsedDefaults(
        context: Context,
        params: Set<String>,
        theme: Theme,
        opacity: Int
    ) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs[LAST_USED_PARAMS] = params
            prefs[LAST_USED_THEME] = theme.name
            prefs[LAST_USED_OPACITY] = opacity.coerceIn(0, 100)
        }
    }

    data class Defaults(val params: Set<String>, val theme: Theme, val opacity: Int)

    suspend fun clearAll(context: Context, appWidgetId: Int) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs.remove(latKey(appWidgetId))
            prefs.remove(lonKey(appWidgetId))
            prefs.remove(nameKey(appWidgetId))
            prefs.remove(paramsKey(appWidgetId))
            prefs.remove(themeKey(appWidgetId))
            prefs.remove(opacityKey(appWidgetId))
        }
    }
}
