package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.util.weatherCodeToInfo
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Minimalist 2×1 "day almanac" widget: sunrise, sunset, a progress bar with a
 * "now" marker between them and the UV index for the day. Tap opens the app.
 */
class WeatherSunWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = runCatching { WidgetSunDataLoader.loadCurrent(context) }
            .getOrDefault(SunWidgetData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            val data by WidgetSunDataLoader.widgetDataFlow(context)
                .collectAsState(initial = initial)
            SunContent(data, context)
        }
    }

    @Composable
    private fun SunContent(data: SunWidgetData, context: Context) {
        val palette = WidgetCompactPalette.resolve(
            context = context,
            theme = WidgetPrefs.Theme.SYSTEM,
            opacityPct = WidgetPrefs.DEFAULT_OPACITY
        )
        val size = LocalSize.current
        val spec = WidgetSunLayout.resolve(size.width.value, size.height.value)
        val tap: Action = actionStartActivity<MainActivity>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(palette.background))
                .clickable(tap)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (data) {
                    SunWidgetData.NoFavorites -> Text(
                        text = context.getString(R.string.widget_no_favorites),
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.timeSp.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    is SunWidgetData.NoData -> Text(
                        text = context.getString(R.string.widget_no_data),
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.timeSp.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    is SunWidgetData.Ready -> SunReady(data, spec, palette, context, size.width.value)
                }
            }
        }
    }

    @Composable
    private fun SunReady(
        data: SunWidgetData.Ready,
        spec: WidgetSunLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        context: Context,
        widgetWidthDp: Float
    ) {
        val progress = WidgetSunWindow.dayProgress(data.sunrise, data.sunset, data.now)

        val duration = Duration.between(data.sunrise, data.sunset)
        val dayHours = duration.toHours().coerceAtLeast(0)
        val dayMinutes = (duration.toMinutes() - dayHours * 60).coerceAtLeast(0)
        val dayLengthText = "${dayHours}h ${dayMinutes}m"

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Row 0 (2×2+): hero — weather icon + current temp + city name.
            if (spec.showHero) {
                HeroRow(data = data, spec = spec, palette = palette)
            }

            // Row 1: times (sunrise on the left, sunset on the right).
            // Middle: city when there is no hero row above; day length otherwise.
            val middleText = if (spec.showHero) dayLengthText else data.cityName
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeLabel(
                    time = TIME_FORMATTER.format(data.sunrise),
                    label = context.getString(R.string.sun_sunrise),
                    spec = spec,
                    palette = palette,
                    alignEnd = false,
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = middleText,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.citySp.sp,
                        textAlign = TextAlign.Center
                    )
                )
                TimeLabel(
                    time = TIME_FORMATTER.format(data.sunset),
                    label = context.getString(R.string.sun_sunset),
                    spec = spec,
                    palette = palette,
                    alignEnd = true,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            // Row 2: progress bar with "now" marker.
            ProgressRow(
                progress = progress,
                spec = spec,
                palette = palette,
                widgetWidthDp = widgetWidthDp
            )

            // Row 3: UV index + textual label (only when UV is known).
            val category = WidgetSunWindow.categorize(data.uvIndexMax)
            if (category != null && data.uvIndexMax != null) {
                UvRow(
                    uv = data.uvIndexMax,
                    category = category,
                    spec = spec,
                    palette = palette,
                    context = context
                )
            }
        }
    }

    @Composable
    private fun HeroRow(
        data: SunWidgetData.Ready,
        spec: WidgetSunLayout.Spec,
        palette: WidgetCompactPalette.Palette
    ) {
        val info = data.weatherCode?.let { weatherCodeToInfo(it, isDay = data.isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            data.weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (data.temperature != null) {
                Text(
                    text = "${data.temperature.toInt()}\u00B0",
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.tempSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Box(modifier = GlanceModifier.size(8.dp)) {}
            }
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(spec.weatherIconDp.dp)
                )
                Box(modifier = GlanceModifier.size(10.dp)) {}
            }
            Text(
                text = data.cityName,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.tempSp.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    @Composable
    private fun TimeLabel(
        time: String,
        label: String,
        spec: WidgetSunLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        alignEnd: Boolean,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
        ) {
            Text(
                text = time,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.timeSp.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.labelSp.sp
                )
            )
        }
    }

    @Composable
    private fun ProgressRow(
        progress: Float,
        spec: WidgetSunLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        widgetWidthDp: Float
    ) {
        // Glance has no proportional weights (only equal `defaultWeight`) and no
        // absolute offset. We approximate proportional positioning of the "now"
        // dot with `padding(start=...)` computed from `LocalSize` width.
        //
        // Inner width = widget width minus horizontal content padding (2×10dp)
        // and a small safety margin so the dot doesn't clip on the edges.
        val innerWidth = (widgetWidthDp - 20f).coerceAtLeast(1f)
        val filledWidthDp = (innerWidth * progress).coerceIn(0f, innerWidth)
        val markerStartDp = ((innerWidth - spec.dotSizeDp) * progress)
            .coerceIn(0f, innerWidth - spec.dotSizeDp)
        val trackColor = Color(if (palette.isDark) 0x40FFFFFF else 0x33000000)
        val fillColor = Color(0xFFF2A83B.toInt()) // warm sun-gold

        Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Box(modifier = GlanceModifier.fillMaxWidth().height(spec.dotSizeDp.dp)) {
                // Full-width track (unfilled).
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(spec.dotSizeDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(spec.barHeightDp.dp)
                            .cornerRadius(spec.barHeightDp.dp / 2)
                            .background(ColorProvider(trackColor))
                    ) {}
                }
                // Filled portion from sunrise → now.
                if (filledWidthDp > 0f) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(spec.dotSizeDp.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(filledWidthDp.dp)
                                .height(spec.barHeightDp.dp)
                                .cornerRadius(spec.barHeightDp.dp / 2)
                                .background(ColorProvider(fillColor))
                        ) {}
                    }
                }
                // Marker: rendered as a fixed-width spacer + a dot Box.
                // `padding(start=…)` in Glance is not reliable for absolute offset,
                // so we split the row into two children with explicit widths.
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(spec.dotSizeDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (markerStartDp > 0f) {
                        Box(modifier = GlanceModifier.width(markerStartDp.dp).height(1.dp)) {}
                    }
                    Box(
                        modifier = GlanceModifier
                            .size(spec.dotSizeDp.dp)
                            .cornerRadius(spec.dotSizeDp.dp)
                            .background(ColorProvider(fillColor))
                    ) {}
                }
            }
        }
    }

    @Composable
    private fun UvRow(
        uv: Double,
        category: WidgetSunWindow.UvCategory,
        spec: WidgetSunLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        context: Context
    ) {
        val labelRes = when (category) {
            WidgetSunWindow.UvCategory.Low -> R.string.sun_uv_low
            WidgetSunWindow.UvCategory.Moderate -> R.string.sun_uv_moderate
            WidgetSunWindow.UvCategory.High -> R.string.sun_uv_high
            WidgetSunWindow.UvCategory.VeryHigh -> R.string.sun_uv_very_high
            WidgetSunWindow.UvCategory.Extreme -> R.string.sun_uv_extreme
        }
        val uvText = if (uv >= 10.0) uv.toInt().toString() else String.format("%.1f", uv)
        val dotColor = Color(WidgetSunWindow.uvColorArgb(category))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = GlanceModifier
                    .size(spec.uvDotDp.dp)
                    .cornerRadius(spec.uvDotDp.dp)
                    .background(ColorProvider(dotColor))
            ) {}
            Box(modifier = GlanceModifier.size(6.dp)) {}
            Text(
                text = context.getString(R.string.sun_uv_prefix, uvText),
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.uvSp.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Box(modifier = GlanceModifier.size(8.dp)) {}
            Text(
                text = context.getString(labelRes),
                style = TextStyle(
                    color = ColorProvider(palette.text),
                    fontSize = spec.uvLabelSp.sp
                )
            )
        }
    }

    companion object {
        private const val TAG = "WeatherSunWidget"
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
