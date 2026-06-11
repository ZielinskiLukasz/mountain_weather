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
import com.ergonomic.mountainweather.util.WeatherParamLine
import com.ergonomic.mountainweather.util.weatherCodeToInfo

/**
 * Widget #4: one city at a time; tap cycles favorites. When resized, shows enabled weather
 * parameters from settings to the right (wide) or below (tall).
 */
class WeatherParamsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = runCatching { WidgetParamsDataLoader.loadCurrent(context) }
            .getOrDefault(WidgetParamsData.NoFavorites)
        Log.d(TAG, "provideGlance: id=$id initial=$initial")
        provideContent {
            GlanceTheme {
                val data by WidgetParamsDataLoader.widgetDataFlow(context)
                    .collectAsState(initial = initial)
                ParamsContent(data, context, id)
            }
        }
    }

    private data class LayoutSizes(
        val iconDp: Int,
        val tempSp: Int,
        val citySp: Int,
        val showCity: Boolean
    )

    @Composable
    private fun ParamsContent(data: WidgetParamsData, context: Context, glanceId: GlanceId) {
        val size = LocalSize.current
        val (w, h) = WidgetParamsLayout.resolveLayoutSize(
            context,
            glanceId,
            size.width.value,
            size.height.value
        )
        val baseLayout = WidgetParamsLayout.spec(w, h)
        val effectivePlacement = if (WidgetParamsLayout.isHorizontalStrip(w, h)) {
            WidgetParamsPlacement.Right
        } else {
            baseLayout.placement
        }
        val layout = when {
            data is WidgetParamsData.Ready && effectivePlacement == WidgetParamsPlacement.Split ->
                WidgetParamsLayout.splitForParams(w, h, data.params.size)
            data is WidgetParamsData.Ready && effectivePlacement == WidgetParamsPlacement.Bottom ->
                WidgetParamsLayout.bottomForParams(w, h, data.params.size)
            data is WidgetParamsData.Ready && effectivePlacement == WidgetParamsPlacement.Right ->
                WidgetParamsLayout.rightForParams(w, h, data.params.size)
            else -> baseLayout
        }

        val citySizes = LayoutSizes(
            iconDp = layout.cityIconDp,
            tempSp = layout.cityTempSp,
            citySp = layout.cityLabelSp,
            showCity = layout.showCity
        )
        val sideBySide = when {
            WidgetParamsLayout.isHorizontalStrip(w, h) -> false
            WidgetParamsLayout.isSquareWidget(w, h) -> false
            layout.sideBySideParams -> true
            WidgetParamsLayout.usesSideBySideParams(w, h) -> true
            else -> false
        }

        val cycle: Action = actionRunCallback<CycleFavoriteAction>()
        val openApp: Action = actionStartActivity<MainActivity>()
        val tap: Action = if (data is WidgetParamsData.Ready) cycle else openApp
        val contentAlignment = when {
            data is WidgetParamsData.Ready && effectivePlacement == WidgetParamsPlacement.Right ->
                Alignment.TopStart
            data is WidgetParamsData.Ready && effectivePlacement == WidgetParamsPlacement.Bottom ->
                Alignment.TopCenter
            data is WidgetParamsData.Ready && sideBySide -> Alignment.Center
            data is WidgetParamsData.Ready &&
                effectivePlacement != WidgetParamsPlacement.Compact -> Alignment.TopStart
            else -> Alignment.Center
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(tap)
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(tap),
                contentAlignment = contentAlignment
            ) {
                when (data) {
                    WidgetParamsData.NoFavorites -> WidgetMessage(
                        text = context.getString(R.string.widget_no_favorites),
                        fontSizeSp = citySizes.citySp + 1,
                        tap = tap
                    )

                    is WidgetParamsData.NoData -> WidgetNoDataContent(
                        cityName = data.cityName,
                        sizes = citySizes,
                        tap = tap,
                        context = context
                    )

                    is WidgetParamsData.Ready -> {
                        val visibleParams = data.params.take(
                            layout.maxParamLines.coerceAtLeast(
                                if (effectivePlacement == WidgetParamsPlacement.Right && data.params.isNotEmpty()) 1 else 0
                            )
                        )
                        when (effectivePlacement) {
                            WidgetParamsPlacement.Compact -> WidgetIconTempColumn(
                                temperature = data.temperature,
                                weatherCode = data.weatherCode,
                                isDay = data.isDay,
                                sizes = citySizes,
                                tap = tap
                            )

                            WidgetParamsPlacement.Split -> if (sideBySide) {
                                SideBySideStripRow(
                                    cityName = data.cityName,
                                    temperature = data.temperature,
                                    weatherCode = data.weatherCode,
                                    isDay = data.isDay,
                                    sizes = citySizes,
                                    cityWidthFraction = 0.38f,
                                    widgetWidthDp = w,
                                    panelHeightDp = h,
                                    params = visibleParams,
                                    paramSp = layout.paramSp,
                                    compact = layout.compactParams,
                                    linePaddingDp = layout.paramLinePaddingDp,
                                    compactLeft = false,
                                    tap = tap
                                )
                            } else Column(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                    .clickable(tap),
                                horizontalAlignment = Alignment.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (layout.stackedCityHeader) {
                                    WidgetCityStackedHeader(
                                        cityName = data.cityName,
                                        temperature = data.temperature,
                                        weatherCode = data.weatherCode,
                                        isDay = data.isDay,
                                        sizes = citySizes,
                                        widgetWidthDp = w,
                                        verticalPaddingDp = layout.headerPaddingVerticalDp,
                                        tap = tap
                                    )
                                } else {
                                    WidgetCityRow(
                                        cityName = data.cityName,
                                        temperature = data.temperature,
                                        weatherCode = data.weatherCode,
                                        isDay = data.isDay,
                                        sizes = citySizes,
                                        widgetWidthDp = w,
                                        verticalPaddingDp = layout.headerPaddingVerticalDp,
                                        tap = tap
                                    )
                                }
                                if (visibleParams.isNotEmpty()) {
                                    val paramColumns = if (h >= w * 1.2f) 1 else layout.paramColumns
                                    ParamsSplitSection(
                                        params = visibleParams,
                                        columns = paramColumns,
                                        paramSp = layout.paramSp,
                                        compact = layout.compactParams,
                                        linePaddingDp = layout.paramLinePaddingDp,
                                        tap = tap,
                                        modifier = GlanceModifier.defaultWeight().clickable(tap)
                                    )
                                }
                            }

                            WidgetParamsPlacement.Right -> HorizontalStripRow(
                                cityName = data.cityName,
                                temperature = data.temperature,
                                weatherCode = data.weatherCode,
                                isDay = data.isDay,
                                sizes = citySizes,
                                widgetWidthDp = w,
                                params = visibleParams,
                                paramSp = layout.paramSp,
                                compact = layout.compactParams,
                                linePaddingDp = layout.paramLinePaddingDp,
                                tap = tap
                            )

                            WidgetParamsPlacement.Bottom -> if (sideBySide) {
                                SideBySideStripRow(
                                    cityName = data.cityName,
                                    temperature = data.temperature,
                                    weatherCode = data.weatherCode,
                                    isDay = data.isDay,
                                    sizes = citySizes,
                                    cityWidthFraction = 0.42f,
                                    widgetWidthDp = w,
                                    panelHeightDp = h,
                                    params = visibleParams,
                                    paramSp = layout.paramSp,
                                    compact = layout.compactParams,
                                    linePaddingDp = layout.paramLinePaddingDp,
                                    compactLeft = false,
                                    tap = tap
                                )
                            } else Column(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .clickable(tap),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.Top
                            ) {
                                WidgetIconTempColumn(
                                    temperature = data.temperature,
                                    weatherCode = data.weatherCode,
                                    isDay = data.isDay,
                                    sizes = citySizes,
                                    tap = tap
                                )
                                if (visibleParams.isNotEmpty()) {
                                    Box(
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .fillMaxSize()
                                            .padding(top = 4.dp)
                                            .clickable(tap),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        ParamsColumn(
                                            params = visibleParams,
                                            paramSp = layout.paramSp,
                                            compact = layout.compactParams,
                                            tap = tap,
                                            linePaddingDp = layout.paramLinePaddingDp,
                                            modifier = GlanceModifier.clickable(tap)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** User 1×2 horizontal: fixed-width left (icon+temp+city) + weighted params column. */
    @Composable
    private fun HorizontalStripRow(
        cityName: String,
        temperature: Double,
        weatherCode: Int,
        isDay: Boolean,
        sizes: LayoutSizes,
        widgetWidthDp: Float,
        params: List<WeatherParamLine>,
        paramSp: Int,
        compact: Boolean,
        linePaddingDp: Int,
        tap: Action
    ) {
        val leftWidthDp = ((widgetWidthDp - 8f) / 2f).toInt()
            .coerceIn(sizes.iconDp + 28, (widgetWidthDp * 0.52f).toInt())

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 1.dp)
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
                WidgetCityColumn(
                    cityName = cityName,
                    temperature = temperature,
                    weatherCode = weatherCode,
                    isDay = isDay,
                    sizes = sizes.copy(showCity = true),
                    widgetWidthDp = leftWidthDp.toFloat(),
                    tap = tap,
                    stripCompact = true
                )
            }
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(tap),
                contentAlignment = Alignment.CenterStart
            ) {
                ParamsColumn(
                    params = params,
                    paramSp = paramSp,
                    compact = compact,
                    tap = tap,
                    linePaddingDp = linePaddingDp,
                    modifier = GlanceModifier.clickable(tap)
                )
            }
        }
    }

    @Composable
    private fun SideBySideStripRow(
        cityName: String,
        temperature: Double,
        weatherCode: Int,
        isDay: Boolean,
        sizes: LayoutSizes,
        cityWidthFraction: Float,
        widgetWidthDp: Float,
        panelHeightDp: Float,
        params: List<WeatherParamLine>,
        paramSp: Int,
        compact: Boolean,
        linePaddingDp: Int,
        compactLeft: Boolean,
        tap: Action
    ) {
        if (compactLeft) {
            HorizontalStripRow(
                cityName = cityName,
                temperature = temperature,
                weatherCode = weatherCode,
                isDay = isDay,
                sizes = sizes,
                widgetWidthDp = widgetWidthDp,
                params = params,
                paramSp = paramSp,
                compact = compact,
                linePaddingDp = linePaddingDp,
                tap = tap
            )
        } else {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clickable(tap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxSize()
                        .clickable(tap),
                    contentAlignment = Alignment.Center
                ) {
                    WidgetCityColumn(
                        cityName = cityName,
                        temperature = temperature,
                        weatherCode = weatherCode,
                        isDay = isDay,
                        sizes = sizes,
                        widgetWidthDp = widgetWidthDp * cityWidthFraction,
                        tap = tap
                    )
                }
                if (params.isNotEmpty()) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxSize()
                            .padding(start = 8.dp, end = 2.dp)
                            .clickable(tap),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        ParamsColumn(
                            params = params,
                            paramSp = paramSp,
                            compact = compact,
                            tap = tap,
                            linePaddingDp = linePaddingDp,
                            modifier = GlanceModifier.clickable(tap)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ParamsSplitSection(
        params: List<WeatherParamLine>,
        columns: Int,
        paramSp: Int,
        compact: Boolean,
        linePaddingDp: Int,
        tap: Action,
        modifier: GlanceModifier
    ) {
        val contentAlignment = if (columns <= 1) Alignment.CenterStart else Alignment.TopStart
        Box(
            modifier = modifier.padding(top = 2.dp),
            contentAlignment = contentAlignment
        ) {
            if (columns <= 1) {
                ParamsColumn(
                    params = params,
                    paramSp = paramSp,
                    compact = compact,
                    tap = tap,
                    linePaddingDp = linePaddingDp,
                    modifier = GlanceModifier.fillMaxSize().clickable(tap)
                )
            } else {
                val perColumn = (params.size + columns - 1) / columns
                Row(
                    modifier = GlanceModifier.fillMaxSize().clickable(tap),
                    verticalAlignment = Alignment.Top
                ) {
                    for (col in 0 until columns) {
                        val slice = params.drop(col * perColumn).take(perColumn)
                        if (slice.isNotEmpty()) {
                            ParamsColumn(
                                params = slice,
                                paramSp = paramSp,
                                compact = compact,
                                tap = tap,
                                linePaddingDp = linePaddingDp,
                                modifier = GlanceModifier.defaultWeight().clickable(tap)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetCityNameText(
        cityName: String,
        preferredSp: Int,
        availableWidthDp: Float,
        tap: Action,
        modifier: GlanceModifier = GlanceModifier,
        textAlign: TextAlign = TextAlign.Start
    ) {
        val fontSp = WidgetCityNameFit.fontSp(cityName, availableWidthDp, preferredSp)
        Text(
            text = cityName,
            maxLines = 1,
            modifier = modifier.clickable(tap),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = fontSp.sp,
                textAlign = textAlign
            )
        )
    }

    @Composable
    private fun WidgetCityStackedHeader(
        cityName: String,
        temperature: Double?,
        weatherCode: Int?,
        isDay: Boolean,
        sizes: LayoutSizes,
        widgetWidthDp: Float,
        verticalPaddingDp: Int = 4,
        tap: Action
    ) {
        val info = weatherCode?.let { weatherCodeToInfo(it, isDay = isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }
        val tempText = temperature?.let { "${it.toInt()}\u00B0" } ?: "–"
        val textBlockWidth = (widgetWidthDp - sizes.iconDp - 14f).coerceAtLeast(24f)

        Row(
            modifier = GlanceModifier
                .padding(horizontal = 2.dp, vertical = verticalPaddingDp.dp)
                .clickable(tap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(sizes.iconDp.dp)
                        .padding(end = 10.dp)
                        .clickable(tap)
                )
            }
            Column(
                modifier = GlanceModifier.clickable(tap),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tempText,
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(tap),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = sizes.tempSp.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                )
                if (sizes.showCity) {
                    WidgetCityNameText(
                        cityName = cityName,
                        preferredSp = sizes.citySp,
                        availableWidthDp = textBlockWidth,
                        tap = tap,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetCityRow(
        cityName: String,
        temperature: Double?,
        weatherCode: Int?,
        isDay: Boolean,
        sizes: LayoutSizes,
        widgetWidthDp: Float,
        verticalPaddingDp: Int = 4,
        expandCityName: Boolean = true,
        tap: Action
    ) {
        val info = weatherCode?.let { weatherCodeToInfo(it, isDay = isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }

        Row(
            modifier = GlanceModifier
                .padding(horizontal = 2.dp, vertical = verticalPaddingDp.dp)
                .clickable(tap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconResId != 0) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(sizes.iconDp.dp)
                        .padding(end = 8.dp)
                        .clickable(tap)
                )
            }
            Text(
                text = temperature?.let { "${it.toInt()}\u00B0" } ?: "–",
                maxLines = 1,
                modifier = GlanceModifier.padding(end = 10.dp).clickable(tap),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = sizes.tempSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            )
            if (sizes.showCity) {
                val tempText = temperature?.let { "${it.toInt()}\u00B0" } ?: "–"
                val cityWidth = WidgetCityNameFit.availableForRow(
                    widgetWidthDp = widgetWidthDp,
                    iconDp = sizes.iconDp,
                    tempSp = sizes.tempSp,
                    tempText = tempText,
                    hasIcon = iconResId != 0,
                    outerPaddingDp = 16f
                )
                WidgetCityNameText(
                    cityName = cityName,
                    preferredSp = sizes.citySp,
                    availableWidthDp = cityWidth,
                    tap = tap,
                    modifier = if (expandCityName) {
                        GlanceModifier.defaultWeight()
                    } else {
                        GlanceModifier
                    },
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    @Composable
    private fun ParamsColumn(
        params: List<WeatherParamLine>,
        paramSp: Int,
        compact: Boolean,
        tap: Action,
        modifier: GlanceModifier,
        linePaddingDp: Int = 0
    ) {
        Column(
            modifier = modifier.padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.Top
        ) {
            params.forEachIndexed { index, param ->
                Text(
                    text = param.displayText(compact),
                    maxLines = 1,
                    modifier = GlanceModifier
                        .clickable(tap)
                        .padding(bottom = if (index < params.lastIndex) linePaddingDp.dp else 0.dp),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = paramSp.sp,
                        textAlign = TextAlign.Start
                    )
                )
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
    private fun WidgetIconTempColumn(
        temperature: Double?,
        weatherCode: Int?,
        isDay: Boolean,
        sizes: LayoutSizes,
        tap: Action,
        verticalPaddingDp: Int = 4
    ) {
        val info = weatherCode?.let { weatherCodeToInfo(it, isDay = isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }

        Column(
            modifier = GlanceModifier
                .padding(horizontal = 4.dp, vertical = verticalPaddingDp.dp)
                .clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top
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
        }
    }

    @Composable
    private fun WidgetCityColumn(
        cityName: String,
        temperature: Double?,
        weatherCode: Int?,
        isDay: Boolean,
        sizes: LayoutSizes,
        widgetWidthDp: Float,
        tap: Action,
        stripCompact: Boolean = false
    ) {
        val info = weatherCode?.let { weatherCodeToInfo(it, isDay = isDay) }
        val iconResId = when {
            info != null && info.iconRes != 0 -> info.iconRes
            weatherCode != null -> R.drawable.ic_weather_overcast
            else -> 0
        }
        val verticalPad = if (stripCompact) 0 else 2

        Column(
            modifier = GlanceModifier
                .padding(horizontal = 2.dp, vertical = verticalPad.dp)
                .clickable(tap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top
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
                WidgetCityNameText(
                    cityName = cityName,
                    preferredSp = sizes.citySp,
                    availableWidthDp = WidgetCityNameFit.availableForColumn(widgetWidthDp),
                    tap = tap,
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 1.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    companion object {
        private const val TAG = "WeatherParamsWidget"
    }
}
