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

class WeatherMinimalWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Resolve a starting value synchronously so the first frame already shows
        // the right data instead of flickering through NoFavorites.
        val initial = runCatching { WidgetDataLoader.loadCurrent(context) }
            .getOrDefault(WidgetData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            GlanceTheme {
                // collectAsState keeps the widget in sync with DataStore + Room
                // for as long as the Glance composition is alive (~45s after
                // any interaction or update). External updates re-trigger
                // provideGlance which restarts this collector.
                val data by WidgetDataLoader.widgetDataFlow(context)
                    .collectAsState(initial = initial)
                MinimalContent(data, context)
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
    private fun MinimalContent(data: WidgetData, context: Context) {
        val size = LocalSize.current
        val w = size.width.value
        val h = size.height.value
        val minSide = minOf(w, h)

        val sizes = when {
            minSide < 70 -> LayoutSizes(iconDp = 26, tempSp = 16, citySp = 9, cornerDp = 14, cornerPaddingDp = 6, showCity = h >= 70)
            minSide < 110 -> LayoutSizes(iconDp = 34, tempSp = 22, citySp = 11, cornerDp = 18, cornerPaddingDp = 8, showCity = true)
            minSide < 170 -> LayoutSizes(iconDp = 54, tempSp = 36, citySp = 14, cornerDp = 24, cornerPaddingDp = 10, showCity = true)
            minSide < 240 -> LayoutSizes(iconDp = 78, tempSp = 52, citySp = 18, cornerDp = 30, cornerPaddingDp = 12, showCity = true)
            else -> LayoutSizes(iconDp = 100, tempSp = 64, citySp = 22, cornerDp = 36, cornerPaddingDp = 14, showCity = true)
        }

        // Glance bug: clicks on inner Image/Text do NOT bubble to a parent's clickable.
        // Workaround: attach the action to every leaf composable.
        val cycle: Action = actionRunCallback<CycleFavoriteAction>()
        val openApp: Action = actionStartActivity<MainActivity>()
        // In NoFavorites/NoData states the cycle action is useless, so the whole
        // widget routes taps to opening the app.
        val tap: Action = if (data is WidgetData.Ready) cycle else openApp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(tap)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(tap),
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

                    is WidgetData.Ready -> WidgetReadyContent(
                        data = data,
                        sizes = sizes,
                        tap = tap
                    )
                }
            }

            // Corner "open app" icon is only useful when the rest of the widget
            // cycles favorites — in NoData/NoFavorites states the entire surface
            // already routes to openApp, so the icon would be redundant noise.
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
            modifier = GlanceModifier
                .clickable(tap)
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
            modifier = GlanceModifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "–",
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
    private fun WidgetReadyContent(
        data: WidgetData.Ready,
        sizes: LayoutSizes,
        tap: Action
    ) {
        val info = weatherCodeToInfo(data.weatherCode, isDay = true)
        val iconResId = if (info.iconRes != 0) info.iconRes else R.drawable.ic_weather_overcast
        Column(
            modifier = GlanceModifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(iconResId),
                contentDescription = null,
                modifier = GlanceModifier
                    .size(sizes.iconDp.dp)
                    .clickable(tap)
            )
            Text(
                text = "${data.temperature.toInt()}°",
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
                    text = data.cityName,
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
        private const val TAG = "WeatherMinimalWidget"
    }
}
