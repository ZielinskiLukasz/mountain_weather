package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.util.weatherCodeToInfo

/**
 * Widget #3: always one city on screen; tap cycles to the next favorite (no carousel).
 */
class WeatherCurrentWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = runCatching { WidgetDataLoader.loadCurrent(context) }
            .getOrDefault(WidgetData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            GlanceTheme {
                val data by WidgetDataLoader.widgetDataFlow(context)
                    .collectAsState(initial = initial)
                CurrentContent(data, context)
            }
        }
    }

    private data class LayoutSizes(
        val iconDp: Int,
        val tempSp: Int,
        val citySp: Int,
        val cornerDp: Int,
        val cornerPaddingDp: Int,
        val showCity: Boolean
    )

    @Composable
    private fun CurrentContent(data: WidgetData, context: Context) {
        val size = LocalSize.current
        val w = size.width.value
        val h = size.height.value
        val minSide = minOf(w, h)

        val sizes = when {
            minSide < 70 -> LayoutSizes(26, 16, 9, 14, 6, h >= 70)
            minSide < 110 -> LayoutSizes(34, 22, 11, 18, 8, true)
            minSide < 170 -> LayoutSizes(54, 36, 14, 24, 10, true)
            minSide < 240 -> LayoutSizes(78, 52, 18, 30, 12, true)
            else -> LayoutSizes(100, 64, 22, 36, 14, true)
        }

        val cycle: Action = actionRunCallback<CycleFavoriteAction>()
        val openApp: Action = actionStartActivity<MainActivity>()
        val tap: Action = if (data is WidgetData.Ready) cycle else openApp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(tap)
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(tap),
                contentAlignment = Alignment.Center
            ) {
                when (data) {
                    WidgetData.NoFavorites -> WidgetMessage(
                        text = context.getString(R.string.widget_no_favorites),
                        fontSizeSp = sizes.citySp + 1,
                        tap = tap
                    )

                    is WidgetData.NoData -> WidgetNoDataContent(
                        cityName = data.cityName,
                        sizes = sizes,
                        tap = tap,
                        context = context
                    )

                    is WidgetData.Ready -> WidgetCityColumn(
                        cityName = data.cityName,
                        temperature = data.temperature,
                        weatherCode = data.weatherCode,
                        isDay = data.isDay,
                        sizes = sizes,
                        tap = tap
                    )
                }
            }

            if (data is WidgetData.Ready) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(top = sizes.cornerPaddingDp.dp, end = sizes.cornerPaddingDp.dp)
                        .clickable(cycle),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_open_app),
                        contentDescription = context.getString(R.string.widget_open_app),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier
                            .size(sizes.cornerDp.dp)
                            .clickable(openApp)
                    )
                }
            }
        }
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
        sizes: LayoutSizes,
        tap: Action,
        context: Context
    ) {
        Column(
            modifier = GlanceModifier.padding(horizontal = 6.dp, vertical = 4.dp).clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "–",
                maxLines = 1,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.tempSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = cityName ?: context.getString(R.string.widget_no_data),
                maxLines = 2,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.citySp.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun WidgetCityColumn(
        cityName: String,
        temperature: Double?,
        weatherCode: Int?,
        isDay: Boolean,
        sizes: LayoutSizes,
        tap: Action
    ) {
        val info = weatherCode?.let { weatherCodeToInfo(it, isDay = isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }

        Column(
            modifier = GlanceModifier.padding(horizontal = 2.dp, vertical = 2.dp).clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier.size(sizes.iconDp.dp).clickable(tap)
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
            if (sizes.showCity) {
                Text(
                    text = cityName,
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = sizes.citySp.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "WeatherCurrentWidget"
    }
}
