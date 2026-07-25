package com.ergonomic.mountainweather.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.ergonomic.mountainweather.MainActivity
import com.ergonomic.mountainweather.R
import com.ergonomic.mountainweather.util.weatherCodeToInfo

/**
 * Daily-forecast widget: tap scrolls the visible day window forward through the forecast
 * configured in settings (3 / 5 / 7 / 14 days + today).
 *
 * Layout (horizontal grid cells):
 * - **1×1**: one day at the current scroll offset.
 * - **1×2**: two consecutive days; **1×3**: three; **1×N**: N days side-by-side.
 */
class WeatherDailyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = runCatching { WidgetDailyDataLoader.loadCurrent(context) }
            .getOrDefault(DailyWidgetData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val dayIndex = prefs[WidgetDailyKeys.DAY_INDEX] ?: 0
                val data by WidgetDailyDataLoader.widgetDataFlow(context)
                    .collectAsState(initial = initial)
                DailyContent(data, dayIndex, context, glanceId = id)
            }
        }
    }

    /** How many forecast days to show side-by-side for the current widget width. */
    private fun visibleDayCount(widthDp: Float): Int = WidgetDailyWindow.visibleColumnCount(widthDp)

    /** Equal column sizes — temp scaled to column width so digits stay on one line. */
    private fun equalColumnSizes(base: WidgetLayoutSizes, widthDp: Float, columns: Int): WidgetLayoutSizes {
        val colWidth = widthDp / columns.coerceAtLeast(1)
        return base.copy(
            tempSp = WidgetLayout.fitTempSp(base.tempSp, colWidth),
            showCity = false,
            showMinTemp = base.showMinTemp && columns == 1
        )
    }

    @Composable
    private fun DailyContent(data: DailyWidgetData, dayIndex: Int, context: Context, glanceId: GlanceId) {
        val size = LocalSize.current
        val localW = size.width.value
        val layoutW = WidgetDailyWindow.resolveLayoutWidthDp(context, glanceId, localW)
        val h = size.height.value
        val baseSizes = WidgetLayout.computeSizes(localW, h)

        val cycle: Action = actionRunCallback<CycleDailyAction>()
        val openApp: Action = actionStartActivity<MainActivity>()
        val tap: Action = if (data is DailyWidgetData.Ready) cycle else openApp

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
                    DailyWidgetData.NoFavorites -> WidgetMessage(
                        text = context.getString(R.string.widget_no_favorites),
                        fontSizeSp = baseSizes.labelSp + 1,
                        tap = tap
                    )

                    is DailyWidgetData.NoData -> WidgetNoDataContent(
                        cityName = data.cityName,
                        sizes = baseSizes,
                        tap = tap,
                        context = context
                    )

                    is DailyWidgetData.Ready -> {
                        if (data.days.isEmpty()) {
                            WidgetNoDataContent(
                                cityName = data.cityName,
                                sizes = baseSizes,
                                tap = tap,
                                context = context
                            )
                        } else {
                            val columns = visibleDayCount(layoutW)
                            val startIndex = WidgetDailyWindow.coerceStartIndex(
                                dayIndex,
                                data.days.size,
                                columns
                            )
                            if (columns == 1) {
                                DailyDayColumn(
                                    day = data.days[startIndex],
                                    cityName = data.cityName,
                                    context = context,
                                    sizes = baseSizes,
                                    showCity = baseSizes.showCity,
                                    tap = tap
                                )
                            } else {
                                val strip = WidgetDailyWindow.visibleDays(data.days, startIndex, columns)
                                val colSizes = equalColumnSizes(baseSizes, localW, columns)
                                DailyMultiDayRow(
                                    days = strip,
                                    context = context,
                                    sizes = colSizes,
                                    tap = tap
                                )
                            }
                        }
                    }
                }
            }

            if (data is DailyWidgetData.Ready) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(top = baseSizes.cornerPaddingDp.dp, end = baseSizes.cornerPaddingDp.dp)
                        .clickable(cycle),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_open_app),
                        contentDescription = context.getString(R.string.widget_open_app),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier
                            .size(baseSizes.cornerDp.dp)
                            .clickable(openApp)
                    )
                }
            }
        }
    }

    @Composable
    private fun DailyMultiDayRow(
        days: List<DailyDaySnapshot>,
        context: Context,
        sizes: WidgetLayoutSizes,
        tap: Action
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clickable(tap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            days.forEach { day ->
                Box(
                    modifier = GlanceModifier.defaultWeight().clickable(tap),
                    contentAlignment = Alignment.Center
                ) {
                    DailyDayColumn(
                        day = day,
                        cityName = null,
                        context = context,
                        sizes = sizes,
                        showCity = false,
                        tap = tap
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
        sizes: WidgetLayoutSizes,
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
                text = cityName ?: context.getString(R.string.widget_no_daily_data),
                maxLines = 2,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.labelSp.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun DailyDayColumn(
        day: DailyDaySnapshot,
        cityName: String?,
        context: Context,
        sizes: WidgetLayoutSizes,
        showCity: Boolean,
        tap: Action
    ) {
        // For today, use current-hour overrides so the widget matches the main screen.
        val effectiveCode = day.currentWeatherCode ?: day.weatherCode
        val effectiveIsDay = if (day.currentWeatherCode != null) day.currentIsDay else true
        val info = weatherCodeToInfo(effectiveCode, isDay = effectiveIsDay)
        val iconResId = if (info.iconRes != 0) info.iconRes else R.drawable.ic_weather_overcast
        val dateLabel = WidgetDailyDateFormatter.format(context, day.date)

        // For today, show current temp; for future days, show daily max.
        val displayTemp = day.currentTemp ?: day.tempMax

        Column(
            modifier = GlanceModifier.padding(horizontal = 2.dp, vertical = 2.dp).clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(iconResId),
                contentDescription = null,
                modifier = GlanceModifier.size(sizes.iconDp.dp).clickable(tap)
            )
            Text(
                text = "${displayTemp.toInt()}\u00B0",
                maxLines = 1,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.tempSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            if (sizes.showMinTemp) {
                Text(
                    text = "${day.tempMin.toInt()}\u00B0",
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = sizes.minTempSp.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Text(
                text = dateLabel,
                maxLines = 1,
                modifier = GlanceModifier.clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.labelSp.sp,
                    textAlign = TextAlign.Center
                )
            )
            if (showCity && cityName != null) {
                Text(
                    text = cityName,
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = sizes.labelSp.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "WeatherDailyWidget"
    }
}
