package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.util.resolveIsDay
import com.ergonomic.mountainweather.util.weatherCodeToInfo

/**
 * Fixed horizontal 4×1 hourly strip for the currently selected city.
 * Tap anywhere opens the app — no favorite cycling.
 */
class WeatherHourlyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val initial = runCatching { WidgetHourlyDataLoader.loadCurrent(context) }
            .getOrDefault(HourlyWidgetData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            GlanceTheme {
                val data by WidgetHourlyDataLoader.widgetDataFlow(context)
                    .collectAsState(initial = initial)
                HourlyContent(data, context)
            }
        }
    }

    private data class HourlyLayoutSizes(
        val citySp: Int,
        val tempSp: Int,
        val headerIconDp: Int,
        val hourLabelSp: Int,
        val hourTempSp: Int,
        val hourIconDp: Int
    )

    private fun layoutSizes(widthDp: Float, heightDp: Float): HourlyLayoutSizes {
        val h = heightDp.coerceIn(40f, 130f)
        val hourIconDp = (h * 0.44f).toInt().coerceIn(24, 42)
        val hourLabelSp = (h * 0.12f).toInt().coerceIn(9, 12)
        val hourTempSp = (h * 0.18f).toInt().coerceIn(12, 17)
        val headerIconDp = (h * 0.36f).toInt().coerceIn(22, 34)
        val tempSp = (h * 0.18f).toInt().coerceIn(14, 20)
        val citySp = (h * 0.11f).toInt().coerceIn(9, 12)
        return HourlyLayoutSizes(citySp, tempSp, headerIconDp, hourLabelSp, hourTempSp, hourIconDp)
    }

    private fun leftPanelWidthDp(widgetWidthDp: Float, iconDp: Int): Int =
        ((widgetWidthDp - 8f) * 0.22f).toInt().coerceIn(iconDp + 32, (widgetWidthDp * 0.28f).toInt())

    @Composable
    private fun HourlyContent(data: HourlyWidgetData, context: Context) {
        val size = LocalSize.current
        val w = size.width.value
        val h = size.height.value
        val sizes = layoutSizes(w, h)
        val openApp: Action = actionStartActivity<MainActivity>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(openApp)
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(openApp),
                contentAlignment = Alignment.CenterStart
            ) {
                when (data) {
                    HourlyWidgetData.NoFavorites -> WidgetMessage(
                        text = context.getString(R.string.widget_no_favorites),
                        fontSizeSp = sizes.citySp + 1,
                        tap = openApp
                    )

                    is HourlyWidgetData.NoData -> WidgetNoDataContent(
                        cityName = data.cityName,
                        message = if (data.hourlyDisabled) {
                            context.getString(R.string.widget_hourly_disabled)
                        } else {
                            context.getString(R.string.widget_no_data)
                        },
                        fontSizeSp = sizes.citySp + 1,
                        tap = openApp
                    )

                    is HourlyWidgetData.Ready -> HourlyStripRow(
                        data = data,
                        widgetWidthDp = w,
                        sizes = sizes,
                        tap = openApp
                    )
                }
            }
        }
    }

    @Composable
    private fun HourlyStripRow(
        data: HourlyWidgetData.Ready,
        widgetWidthDp: Float,
        sizes: HourlyLayoutSizes,
        tap: Action
    ) {
        val leftWidthDp = leftPanelWidthDp(widgetWidthDp, sizes.headerIconDp)

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clickable(tap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(leftWidthDp.dp)
                    .fillMaxHeight()
                    .clickable(tap),
                contentAlignment = Alignment.Center
            ) {
                HourlyCityColumn(
                    cityName = data.cityName,
                    temperature = data.currentTemp,
                    weatherCode = data.currentWeatherCode,
                    isDay = data.currentIsDay,
                    sizes = sizes,
                    panelWidthDp = leftWidthDp.toFloat(),
                    tap = tap
                )
            }
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(start = 4.dp, end = 2.dp)
                    .clickable(tap),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                data.hours.forEachIndexed { index, hour ->
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(horizontal = 5.dp)
                            .clickable(tap),
                        contentAlignment = Alignment.Center
                    ) {
                        HourColumn(
                            hour = hour,
                            sizes = sizes,
                            isCurrent = index == 0,
                            tap = tap
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HourlyCityColumn(
        cityName: String,
        temperature: Double?,
        weatherCode: Int?,
        isDay: Boolean,
        sizes: HourlyLayoutSizes,
        panelWidthDp: Float,
        tap: Action
    ) {
        val info = weatherCode?.let { weatherCodeToInfo(it, isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }

        Column(
            modifier = GlanceModifier
                .padding(horizontal = 2.dp)
                .clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(sizes.headerIconDp.dp).clickable(tap)
                )
            }
            Text(
                text = temperature?.let { "${it.toInt()}\u00B0" } ?: "–",
                maxLines = 1,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.tempSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            HourlyCityNameText(
                cityName = cityName,
                preferredSp = sizes.citySp,
                availableWidthDp = WidgetCityNameFit.availableForColumn(panelWidthDp, outerPaddingDp = 6f),
                tap = tap
            )
        }
    }

    @Composable
    private fun HourColumn(
        hour: HourlyHourSnapshot,
        sizes: HourlyLayoutSizes,
        isCurrent: Boolean,
        tap: Action
    ) {
        val isDay = resolveIsDay(timeIso = hour.time)
        val info = weatherCodeToInfo(hour.weatherCode, isDay)
        val iconResId = if (info.iconRes != 0) info.iconRes else R.drawable.ic_weather_overcast
        val label = WidgetHourlyWindow.formatHourLabel(hour.time)
        val weight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

        Box(
            modifier = GlanceModifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
                .clickable(tap),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.clickable(tap),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    modifier = GlanceModifier
                        .padding(bottom = 1.dp)
                        .clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = sizes.hourLabelSp.sp,
                        fontWeight = weight,
                        textAlign = TextAlign.Center
                    )
                )
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(sizes.hourIconDp.dp)
                        .clickable(tap)
                )
                Text(
                    text = "${hour.temperature.toInt()}\u00B0",
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = sizes.hourTempSp.sp,
                        fontWeight = weight,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    @Composable
    private fun HourlyCityNameText(
        cityName: String,
        preferredSp: Int,
        availableWidthDp: Float,
        tap: Action
    ) {
        val fontSp = WidgetCityNameFit.fontSp(cityName, availableWidthDp, preferredSp)
        Text(
            text = cityName,
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth().padding(top = 1.dp).clickable(tap),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = fontSp.sp,
                textAlign = TextAlign.Center
            )
        )
    }

    @Composable
    private fun WidgetMessage(text: String, fontSizeSp: Int, tap: Action) {
        Text(
            text = text,
            maxLines = 3,
            modifier = GlanceModifier.clickable(tap).padding(horizontal = 8.dp, vertical = 6.dp),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = fontSizeSp.sp,
                textAlign = TextAlign.Center
            )
        )
    }

    @Composable
    private fun WidgetNoDataContent(
        cityName: String?,
        message: String,
        fontSizeSp: Int,
        tap: Action
    ) {
        Column(
            modifier = GlanceModifier.padding(horizontal = 8.dp, vertical = 6.dp).clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cityName != null) {
                Text(
                    text = cityName,
                    maxLines = 2,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Text(
                text = message,
                maxLines = 3,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = (fontSizeSp - 1).coerceAtLeast(8).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    companion object {
        private const val TAG = "WeatherHourlyWidget"
    }
}
