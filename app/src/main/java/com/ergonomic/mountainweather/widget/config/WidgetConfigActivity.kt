package com.ergonomic.mountainweather.widget.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.data.local.AppDatabase
import com.ergonomic.mountainweather.data.local.SavedLocationEntity
import com.ergonomic.mountainweather.util.WeatherParams
import com.ergonomic.mountainweather.widget.WeatherCompactWidget
import com.ergonomic.mountainweather.widget.WidgetCompactPalette
import com.ergonomic.mountainweather.widget.WidgetPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        // Default to CANCELED: if the user backs out, the framework removes
        // the freshly placed widget instance.
        setResult(Activity.RESULT_CANCELED)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                ConfigScreen(
                    appWidgetId = appWidgetId,
                    onSave = { selection, params, theme, opacity ->
                        persistAndFinish(selection, params, theme, opacity)
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun persistAndFinish(
        selection: Selection,
        params: Set<String>,
        theme: WidgetPrefs.Theme,
        opacity: Int
    ) {
        val ctx = applicationContext
        val widgetId = appWidgetId
        // Persist synchronously so the first widget update reads the new pin.
        runBlocking(Dispatchers.IO) {
            when (selection) {
                Selection.FollowMain -> WidgetPrefs.clearPin(ctx, widgetId)
                is Selection.Pinned -> WidgetPrefs.savePin(
                    ctx,
                    widgetId,
                    WidgetPrefs.Pin(selection.name, selection.lat, selection.lon)
                )
            }
            WidgetPrefs.saveParams(ctx, widgetId, params)
            WidgetPrefs.saveTheme(ctx, widgetId, theme)
            WidgetPrefs.saveOpacity(ctx, widgetId, opacity)
            // Remember last used preferences so the next added widget can preselect them.
            WidgetPrefs.saveLastUsedDefaults(ctx, params, theme, opacity)
        }
        // Trigger an immediate update for this widget instance.
        WeatherCompactWidget.broadcastUpdate(ctx)

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}

/** Hard cap on selected parameters; the widget shows fewer based on its size. */
private const val MAX_PARAMS = 4

private sealed interface Selection {
    object FollowMain : Selection
    data class Pinned(val name: String, val lat: Double, val lon: Double) : Selection
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConfigScreen(
    appWidgetId: Int,
    onSave: (Selection, Set<String>, WidgetPrefs.Theme, Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf<List<SavedLocationEntity>>(emptyList()) }
    var selection by remember { mutableStateOf<Selection>(Selection.FollowMain) }
    var selectedParams by remember { mutableStateOf<Set<String>>(WidgetPrefs.DEFAULT_PARAMS) }
    var theme by remember { mutableStateOf(WidgetPrefs.Theme.SYSTEM) }
    var opacity by remember { mutableStateOf(WidgetPrefs.DEFAULT_OPACITY) }

    LaunchedEffect(appWidgetId) {
        val db = AppDatabase.getInstance(context.applicationContext)
        val pin = WidgetPrefs.getPin(context.applicationContext, appWidgetId)
        val savedParams = WidgetPrefs.getParams(context.applicationContext, appWidgetId)
        val list = withContext(Dispatchers.IO) { db.savedLocationDao().getFavorites() }
        favorites = list
        selection = if (pin != null) {
            Selection.Pinned(pin.name, pin.latitude, pin.longitude)
        } else {
            Selection.FollowMain
        }
        // Treat "no pin AND no saved params" as a brand-new instance; in that case
        // preselect with the user's last-used choices instead of bare defaults.
        if (pin == null && savedParams == null) {
            val defaults = WidgetPrefs.getLastUsedDefaults(context.applicationContext)
            selectedParams = defaults.params
            theme = defaults.theme
            opacity = defaults.opacity
        } else {
            selectedParams = savedParams ?: WidgetPrefs.DEFAULT_PARAMS
            theme = WidgetPrefs.getTheme(context.applicationContext, appWidgetId)
            opacity = WidgetPrefs.getOpacity(context.applicationContext, appWidgetId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_config_preview),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                CompactPreview(
                    cityLabel = when (val s = selection) {
                        is Selection.Pinned -> s.name
                        Selection.FollowMain -> stringResource(R.string.widget_config_follow_main)
                    },
                    params = selectedParams.toList().take(MAX_PARAMS),
                    theme = theme,
                    opacityPct = opacity
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                SelectableRow(
                    label = stringResource(R.string.widget_config_follow_main),
                    selected = selection is Selection.FollowMain,
                    onClick = { selection = Selection.FollowMain }
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_config_favorites),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (favorites.isEmpty()) {
                    Text(
                        text = stringResource(R.string.widget_config_no_favorites),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    favorites.forEach { fav ->
                        val isSelected = (selection as? Selection.Pinned)?.let {
                            kotlin.math.abs(it.lat - fav.latitude) < 0.005 &&
                                kotlin.math.abs(it.lon - fav.longitude) < 0.005
                        } ?: false
                        SelectableRow(
                            label = fav.name,
                            selected = isSelected,
                            onClick = {
                                selection = Selection.Pinned(fav.name, fav.latitude, fav.longitude)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_config_params),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = stringResource(R.string.widget_config_params_hint, MAX_PARAMS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WeatherParams.ALL.forEach { def ->
                        val checked = def.key in selectedParams
                        val canAddMore = selectedParams.size < MAX_PARAMS
                        FilterChip(
                            selected = checked,
                            onClick = {
                                selectedParams = if (checked) {
                                    selectedParams - def.key
                                } else if (canAddMore) {
                                    selectedParams + def.key
                                } else {
                                    selectedParams
                                }
                            },
                            label = {
                                Text(
                                    text = "${def.icon} ${stringResource(def.labelRes)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            enabled = checked || canAddMore,
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_config_theme),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                val themeOptions = listOf(
                    WidgetPrefs.Theme.SYSTEM to R.string.widget_config_theme_system,
                    WidgetPrefs.Theme.LIGHT to R.string.widget_config_theme_light,
                    WidgetPrefs.Theme.DARK to R.string.widget_config_theme_dark
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { index, (value, labelRes) ->
                        SegmentedButton(
                            selected = theme == value,
                            onClick = { theme = value },
                            shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size)
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.widget_config_opacity),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.widget_config_opacity_label, opacity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Slider(
                    value = opacity.toFloat(),
                    onValueChange = { opacity = it.toInt().coerceIn(0, 100) },
                    valueRange = 0f..100f,
                    steps = 19, // 5%-step granularity
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    selection = Selection.FollowMain
                    selectedParams = WidgetPrefs.DEFAULT_PARAMS
                    theme = WidgetPrefs.Theme.SYSTEM
                    opacity = WidgetPrefs.DEFAULT_OPACITY
                }) {
                    Text(stringResource(R.string.widget_config_restore_defaults))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onCancel) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Button(onClick = { onSave(selection, selectedParams, theme, opacity) }) {
                    Text(stringResource(R.string.widget_config_save))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CompactPreview(
    cityLabel: String,
    params: List<String>,
    theme: WidgetPrefs.Theme,
    opacityPct: Int
) {
    val context = LocalContext.current
    val palette = WidgetCompactPalette.resolve(context, theme, opacityPct)
    val paramDefs = remember(params) {
        params.mapNotNull { key -> WeatherParams.ALL.firstOrNull { it.key == key } }
    }
    // Checker pattern under the preview helps to make transparency visible
    // (otherwise on the white config screen low opacity is hard to read).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = palette.text.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2601", fontSize = 24.sp, color = palette.text)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "20\u00B0",
                        color = palette.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cityLabel,
                        color = palette.text,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
                if (paramDefs.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        paramDefs.take(4).forEach { def ->
                            Text(
                                text = "${def.icon} ${stringResource(def.labelRes)}",
                                color = palette.text,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.padding(start = 8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
