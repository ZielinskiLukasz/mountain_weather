package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.util.weatherCodeToInfo

/**
 * Configurable single-city widget (1×1 .. 2×2+). Each instance can be pinned
 * to a favorite city in `WidgetConfigActivity` or follow the main app.
 */
class WeatherCompactWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = runCatching {
            GlanceAppWidgetManager(context.applicationContext).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

        val initial = runCatching { WidgetCompactDataLoader.load(context, widgetId) }
            .getOrDefault(CompactData.NoData)
        Log.d(TAG, "provideGlance: id=$id appWidgetId=$widgetId initial=$initial")

        provideContent {
            val data by WidgetCompactDataLoader.flow(context, widgetId)
                .collectAsState(initial = initial)
            CompactContent(data, context)
        }
    }

    @Composable
    private fun CompactContent(data: CompactData, context: Context) {
        val size = LocalSize.current
        val spec = WidgetCompactLayout.resolve(size.width.value, size.height.value)
        val palette = resolvePalette(context, data)

        val tapAction: Action = when (data) {
            is CompactData.Ready -> openAppFor(data)
            CompactData.NoData -> actionStartActivity<MainActivity>()
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(palette.background))
                .clickable(tapAction)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .clickable(tapAction),
                contentAlignment = Alignment.Center
            ) {
                when (data) {
                    CompactData.NoData -> Text(
                        text = context.getString(R.string.widget_no_data),
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.citySp.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    is CompactData.Ready -> CompactReady(data, spec, palette, tapAction, context)
                }
            }
        }
    }

    @Composable
    private fun CompactReady(
        data: CompactData.Ready,
        spec: WidgetCompactLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        tap: Action,
        context: Context
    ) {
        val info = data.weatherCode?.let { weatherCodeToInfo(it, isDay = data.isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            data.weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }

        when (spec.size) {
            WidgetCompactLayout.Size.S -> CompactSmallLayout(data, spec, palette, iconResId, tap)
            WidgetCompactLayout.Size.M, WidgetCompactLayout.Size.L ->
                CompactWideLayout(data, spec, palette, iconResId, info?.descriptionRes, tap, context)
        }
    }

    private fun resolvePalette(context: Context, data: CompactData): WidgetCompactPalette.Palette {
        val theme = (data as? CompactData.Ready)?.theme ?: WidgetPrefs.Theme.SYSTEM
        val opacity = (data as? CompactData.Ready)?.opacityPct ?: WidgetPrefs.DEFAULT_OPACITY
        return WidgetCompactPalette.resolve(context, theme, opacity)
    }

    @Composable
    private fun CompactSmallLayout(
        data: CompactData.Ready,
        spec: WidgetCompactLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        iconResId: Int,
        tap: Action
    ) {
        Column(
            modifier = GlanceModifier.clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(spec.iconDp.dp).clickable(tap)
                )
            }
            Text(
                text = data.temperature?.let { "${it.toInt()}\u00B0" } ?: "–",
                maxLines = 1,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.tempSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            if (spec.showCity) {
                Text(
                    text = data.cityName,
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.citySp.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    @Composable
    private fun CompactWideLayout(
        data: CompactData.Ready,
        spec: WidgetCompactLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        iconResId: Int,
        descriptionRes: Int?,
        tap: Action,
        context: Context
    ) {
        Row(
            modifier = GlanceModifier.clickable(tap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(spec.iconDp.dp).clickable(tap)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
            MainInfoColumn(
                data = data,
                spec = spec,
                palette = palette,
                descriptionRes = descriptionRes,
                tap = tap,
                context = context,
                includeParams = !spec.paramsOnRight
            )
            if (spec.paramsOnRight && spec.maxParams > 0 && data.params.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.width(12.dp))
                ParamsColumn(data = data, spec = spec, palette = palette, tap = tap)
            }
        }
    }

    @Composable
    private fun MainInfoColumn(
        data: CompactData.Ready,
        spec: WidgetCompactLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        descriptionRes: Int?,
        tap: Action,
        context: Context,
        includeParams: Boolean
    ) {
        Column(
            modifier = GlanceModifier.clickable(tap),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.temperature?.let { "${it.toInt()}\u00B0" } ?: "–",
                maxLines = 1,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.tempSp.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (spec.showCity) {
                Text(
                    text = data.cityName,
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.citySp.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            if (spec.showSecondary && descriptionRes != null) {
                Text(
                    text = context.getString(descriptionRes),
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.secondarySp.sp
                    )
                )
            }
            if (spec.showHiLo && (data.temperatureMax != null || data.temperatureMin != null)) {
                val hi = data.temperatureMax?.toInt()?.let { "↑${it}\u00B0" } ?: ""
                val lo = data.temperatureMin?.toInt()?.let { "↓${it}\u00B0" } ?: ""
                val feels = data.apparentTemperature?.toInt()
                    ?.let { context.getString(R.string.feels_like, it.toString()) }
                Text(
                    text = listOf(hi, lo).filter { it.isNotEmpty() }.joinToString("  "),
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.secondarySp.sp
                    )
                )
                if (feels != null) {
                    Text(
                        text = feels,
                        maxLines = 1,
                        modifier = GlanceModifier.clickable(tap),
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.secondarySp.sp
                        )
                    )
                }
            }
            if (includeParams && spec.maxParams > 0 && data.params.isNotEmpty()) {
                data.params.take(spec.maxParams).forEach { line ->
                    Text(
                        text = line.displayText(compact = true),
                        maxLines = 1,
                        modifier = GlanceModifier.clickable(tap),
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.paramsSp.sp
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun ParamsColumn(
        data: CompactData.Ready,
        spec: WidgetCompactLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        tap: Action
    ) {
        Column(
            modifier = GlanceModifier.clickable(tap),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            data.params.take(spec.maxParams).forEach { line ->
                Text(
                    text = line.displayText(compact = true),
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.paramsSp.sp
                    )
                )
            }
        }
    }

    private fun openAppFor(data: CompactData.Ready): Action {
        // Pinned widgets open the app on the pinned city; follow-main widgets
        // open the app on whatever city the user is currently viewing.
        return if (data.source == CompactSource.Pinned) {
            actionStartActivity<MainActivity>(
                actionParametersOf(
                    LAT_PARAM to data.latitude,
                    LON_PARAM to data.longitude,
                    NAME_PARAM to data.cityName
                )
            )
        } else {
            actionStartActivity<MainActivity>()
        }
    }

    companion object {
        private const val TAG = "WeatherCompactWidget"

        // Intent extras consumed by MainActivity to scroll/select the pinned city.
        const val EXTRA_LAT = "widget_lat"
        const val EXTRA_LON = "widget_lon"
        const val EXTRA_NAME = "widget_name"

        // Glance ActionParameters keys (mirrored into Intent extras by Glance).
        internal val LAT_PARAM = ActionParameters.Key<Double>(EXTRA_LAT)
        internal val LON_PARAM = ActionParameters.Key<Double>(EXTRA_LON)
        internal val NAME_PARAM = ActionParameters.Key<String>(EXTRA_NAME)

        fun broadcastUpdate(context: Context) {
            val appCtx = context.applicationContext
            runCatching {
                val mgr = AppWidgetManager.getInstance(appCtx)
                val component = ComponentName(appCtx, WeatherCompactReceiver::class.java)
                val ids = mgr.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    val intent = Intent(appCtx, WeatherCompactReceiver::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    appCtx.sendBroadcast(intent)
                }
            }
        }
    }
}

