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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.R
import java.time.format.DateTimeFormatter

/**
 * Horizontal 3×1 bar chart of next 24h precipitation.
 * Tap opens the app — no favorite cycling, follows the main location.
 */
class WeatherRainWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = runCatching { WidgetRainDataLoader.loadCurrent(context) }
            .getOrDefault(RainWidgetData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            val data by WidgetRainDataLoader.widgetDataFlow(context)
                .collectAsState(initial = initial)
            RainContent(data, context)
        }
    }

    @Composable
    private fun RainContent(data: RainWidgetData, context: Context) {
        // Rain widget uses the same palette helper as Compact, but always with
        // the System theme and default opacity. A per-instance config can be
        // added later in a separate stage (analogous to Compact).
        val palette = WidgetCompactPalette.resolve(
            context = context,
            theme = WidgetPrefs.Theme.SYSTEM,
            opacityPct = WidgetPrefs.DEFAULT_OPACITY
        )
        val size = LocalSize.current
        val spec = WidgetRainLayout.resolve(size.width.value, size.height.value)
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
                    RainWidgetData.NoFavorites -> Text(
                        text = context.getString(R.string.widget_no_favorites),
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.citySp.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    is RainWidgetData.NoData -> Text(
                        text = context.getString(R.string.widget_no_data),
                        maxLines = 2,
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = spec.citySp.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    is RainWidgetData.Ready -> RainReady(data, spec, palette, context)
                }
            }
        }
    }

    @Composable
    private fun RainReady(
        data: RainWidgetData.Ready,
        spec: WidgetRainLayout.Spec,
        palette: WidgetCompactPalette.Palette,
        context: Context
    ) {
        val bars = WidgetRainLayout.fitToMaxBars(data.bars, spec.maxBars)
        val maxMm = bars.maxOfOrNull { it.mm } ?: 0.0
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.cityName,
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.citySp.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = context.getString(R.string.widget_rain_total_mm, formatMm(data.sumMm)),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(palette.text),
                        fontSize = spec.sumSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            BarChartRow(bars = bars, maxMm = maxMm, spec = spec, palette = palette)
            AxisRow(bars = bars, spec = spec, palette = palette)
        }
    }

    @Composable
    private fun BarChartRow(
        bars: List<RainBar>,
        maxMm: Double,
        spec: WidgetRainLayout.Spec,
        palette: WidgetCompactPalette.Palette
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(spec.barChartHeightDp.dp)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                // Each bar gets equal horizontal weight; height proportional to
                // mm relative to the local max. Empty hours are shown as a 1dp
                // baseline so the axis is readable even on dry days.
                val fraction = if (maxMm <= 0.0) 0f else (bar.mm / maxMm).toFloat().coerceIn(0f, 1f)
                val barHeightDp = (spec.barChartHeightDp * fraction).toInt().coerceAtLeast(1)
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(horizontal = (spec.barGapDp / 2f).dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(barHeightDp.dp)
                            .cornerRadius(2.dp)
                            .background(ColorProvider(palette.text))
                    ) {}
                }
            }
        }
    }

    @Composable
    private fun AxisRow(
        bars: List<RainBar>,
        spec: WidgetRainLayout.Spec,
        palette: WidgetCompactPalette.Palette
    ) {
        if (bars.isEmpty()) return
        val labels = pickAxisLabels(bars, desired = 5)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bars.forEachIndexed { index, bar ->
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (index in labels) {
                        Text(
                            text = AXIS_FORMATTER.format(bar.time),
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(palette.text),
                                fontSize = spec.axisSp.sp
                            )
                        )
                    } else {
                        // Empty placeholder keeps slot widths aligned with bars.
                        Text(
                            text = " ",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(palette.text),
                                fontSize = spec.axisSp.sp
                            )
                        )
                    }
                }
            }
        }
    }

    /** Choose evenly spaced indices in [bars] to render as axis ticks. */
    private fun pickAxisLabels(bars: List<RainBar>, desired: Int): Set<Int> {
        if (bars.isEmpty()) return emptySet()
        val n = bars.size
        val count = desired.coerceAtMost(n)
        if (count <= 1) return setOf(0)
        val step = (n - 1).toFloat() / (count - 1).toFloat()
        return (0 until count).map { (it * step).toInt() }.toSet()
    }

    private fun formatMm(value: Double): String {
        // Keep "0", show one decimal for < 10 mm, integer otherwise.
        return when {
            value <= 0.0 -> "0"
            value < 10.0 -> String.format("%.1f", value)
            else -> value.toInt().toString()
        }
    }

    companion object {
        private const val TAG = "WeatherRainWidget"
        private val AXIS_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")
    }
}
