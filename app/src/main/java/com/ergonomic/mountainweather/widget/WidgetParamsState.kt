package com.ergonomic.mountainweather.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.ergonomic.mountainweather.util.WeatherParamLine

sealed interface WidgetParamsData {
    data object NoFavorites : WidgetParamsData
    data class NoData(val cityName: String?) : WidgetParamsData
    data class Ready(
        val cityName: String,
        val temperature: Double,
        val weatherCode: Int,
        val isDay: Boolean,
        val params: List<WeatherParamLine>
    ) : WidgetParamsData
}

enum class WidgetParamsPlacement {
    /** 1×1 — city only. */
    Compact,
    /** Params to the right of the city (e.g. 2×1). */
    Right,
    /** Params below the city (e.g. 1×2). */
    Bottom,
    /** City header row + params below (e.g. 2×2, 2×3, 3×2, 3×3). */
    Split
}

data class WidgetParamsLayoutSpec(
    val placement: WidgetParamsPlacement,
    val cityIconDp: Int,
    val cityTempSp: Int,
    val cityLabelSp: Int,
    val paramSp: Int,
    val maxParamLines: Int,
    val paramColumns: Int = 1,
    val paramLinePaddingDp: Int = 3,
    val headerPaddingVerticalDp: Int = 4,
    val stackedCityHeader: Boolean = false,
    val sideBySideParams: Boolean = false,
    val compactParams: Boolean,
    val showCity: Boolean
)

object WidgetParamsLayout {

    private const val INFLATE_THRESHOLD = 1.25f
    /** Host option can lag behind resize; do not shrink a clearly larger LocalSize. */
    private const val STALE_HOST_THRESHOLD = 0.75f

    fun resolveLayoutSize(
        context: Context,
        glanceId: GlanceId,
        localWidthDp: Float,
        localHeightDp: Float
    ): Pair<Float, Float> {
        val hostW = hostOptionDp(context, glanceId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val hostH = hostOptionDp(context, glanceId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        var w = resolveDimension(localWidthDp, hostW)
        var h = resolveDimension(localHeightDp, hostH)
        if (hostW != null && hostH != null && hostW > 0f && hostH > 0f) {
            val hostMin = minOf(hostW, hostH)
            val hostMax = maxOf(hostW, hostH)
            // LocalSize can report a thin strip while host is square (2×2 misread as 2×1).
            if (hostMin >= 110f && hostMax / hostMin < 2.05f &&
                minOf(w, h) < hostMin * 0.85f
            ) {
                w = hostW
                h = hostH
            }
            // 2×3 etc.: width ok but height reported as ~2×1 strip (~82 dp).
            if (hostW >= 110f && hostH >= hostW * 1.4f && h < hostH * 0.55f) {
                w = hostW
                h = hostH
            }
            // 2×1 (user 1×2): height reported as square while host is wide + short.
            if (hostW >= 95f && hostH <= 130f && hostW >= hostH * 1.25f && h > hostH * 1.25f) {
                w = hostW
                h = hostH
            }
        }
        return w to h
    }

    /** User 1×2 = 2 cells wide × 1 tall (Android 2×1). */
    fun isHorizontalStrip(widthDp: Float, heightDp: Float): Boolean =
        widthDp >= 95f && widthDp >= heightDp * 1.25f && heightDp <= 130f

    /** True 1×2 vertical = 1 wide × 2 tall (Android 1×2). */
    fun isVerticalStrip(widthDp: Float, heightDp: Float): Boolean =
        heightDp >= 95f && heightDp >= widthDp * 1.25f && widthDp <= 130f

    /** 2×2, 3×3 — square-ish layouts use header row + params below. */
    fun isSquareWidget(widthDp: Float, heightDp: Float): Boolean {
        val min = minOf(widthDp, heightDp)
        val max = maxOf(widthDp, heightDp)
        return min >= 110f && max / min < 1.25f
    }

    private fun resolveDimension(localDp: Float, hostDp: Float?): Float {
        if (hostDp == null || hostDp <= 0f) return localDp
        if (localDp > hostDp * INFLATE_THRESHOLD) return hostDp
        if (hostDp < localDp * STALE_HOST_THRESHOLD) return localDp
        return localDp
    }

    private fun hostOptionDp(context: Context, glanceId: GlanceId, option: String): Float? {
        return try {
            val appCtx = context.applicationContext
            val widgetId = GlanceAppWidgetManager(appCtx).getAppWidgetId(glanceId)
            val value = AppWidgetManager.getInstance(appCtx)
                .getAppWidgetOptions(widgetId)
                .getInt(option, 0)
            if (value > 0) value.toFloat() else null
        } catch (_: Exception) {
            null
        }
    }

    /** Layout tuned to launcher sizes (~82 / ~179 / ~276 dp per cell). */
    fun spec(widthDp: Float, heightDp: Float): WidgetParamsLayoutSpec {
        val minSide = minOf(widthDp, heightDp)
        val placement = placement(widthDp, heightDp, minSide)
        if (placement == WidgetParamsPlacement.Compact) {
            return WidgetParamsLayoutSpec(
                placement = placement,
                cityIconDp = iconFor(minSide),
                cityTempSp = tempFor(minSide),
                cityLabelSp = labelFor(minSide),
                paramSp = 8,
                maxParamLines = 0,
                compactParams = true,
                showCity = false
            )
        }

        val compactParams = when (placement) {
            WidgetParamsPlacement.Split -> true
            WidgetParamsPlacement.Right -> true
            else -> widthDp < 220f || heightDp < 220f
        }
        val paramSp = when (placement) {
            WidgetParamsPlacement.Split -> 12
            else -> when {
                minSide < 120 -> 8
                minSide < 180 -> 9
                compactParams -> 10
                else -> 11
            }
        }
        val cityBlock = cityBlockFor(placement, widthDp, heightDp, minSide)
        val maxLines = when (placement) {
            WidgetParamsPlacement.Split -> 12
            else -> maxParamLines(placement, widthDp, heightDp, paramSp, cityBlock)
        }
        return WidgetParamsLayoutSpec(
            placement = placement,
            cityIconDp = cityBlock.iconDp,
            cityTempSp = cityBlock.tempSp,
            cityLabelSp = cityBlock.labelSp,
            paramSp = paramSp,
            maxParamLines = maxLines,
            compactParams = compactParams,
            showCity = true
        )
    }

    /** Tall portrait side-by-side (1×3, 2×3) — not 1×2 vertical stack or 2×2 square. */
    fun usesSideBySideParams(widthDp: Float, heightDp: Float): Boolean {
        if (isSquareWidget(widthDp, heightDp)) return false
        if (isHorizontalStrip(widthDp, heightDp)) return false
        if (heightDp < 220f || heightDp < widthDp * 1.35f) return false
        // 1×2 vertical (~82×179): icon + temp on top, params below
        if (widthDp < 95f && heightDp < 200f) return false
        return true
    }

    /** Square layout tuned to actual param count — scales header and param area to fill height. */
    fun splitForParams(widthDp: Float, heightDp: Float, paramCount: Int): WidgetParamsLayoutSpec {
        val count = paramCount.coerceAtLeast(1)
        val minSide = minOf(widthDp, heightDp)
        val large = minSide >= 240f
        val tall = !large && heightDp >= 240f
        val medium = minSide in 165f..239f && !tall
        val sideBySide = usesSideBySideParams(widthDp, heightDp)

        val headerHeight = when {
            sideBySide -> 0f
            large -> (heightDp * 0.26f).coerceIn(56f, 76f)
            tall -> (heightDp * 0.22f).coerceIn(56f, 68f)
            medium -> (heightDp * 0.24f).coerceIn(42f, 52f)
            else -> (heightDp * 0.30f).coerceIn(48f, 58f)
        }
        val iconCap = when { large -> 68; tall -> 60; medium -> 48; else -> 52 }
        val iconFloor = when { large -> 48; tall -> 44; medium -> 36; else -> 40 }
        val iconDp = when {
            sideBySide -> {
                val leftWidth = widthDp * 0.38f
                val contentH = heightDp - 8f
                val iconCapSide = minOf((leftWidth * 0.90f).toInt(), 52)
                (contentH * 0.38f).toInt().coerceIn(32, iconCapSide)
            }
            tall -> (headerHeight * 0.88f).toInt().coerceIn(iconFloor, iconCap)
            else -> (headerHeight * 0.82f).toInt().coerceIn(iconFloor, iconCap)
        }
        val tempCap = when { large -> 38; tall -> 36; medium -> 30; else -> 32 }
        val tempSp = when {
            sideBySide -> {
                val contentH = heightDp - 8f
                (contentH * 0.16f).toInt().coerceIn(20, 28)
            }
            tall -> (headerHeight * 0.58f).toInt().coerceIn(28, tempCap)
            else -> (headerHeight * 0.52f).toInt().coerceIn(22, tempCap)
        }
        val citySp = when {
            sideBySide -> (tempSp * 0.48f).toInt().coerceIn(11, 14)
            tall -> (tempSp * 0.52f).toInt().coerceIn(14, 18)
            else -> (tempSp * 0.55f).toInt().coerceIn(13, if (large) 20 else 17)
        }
        val headerPadV = if (sideBySide) {
            2
        } else {
            ((headerHeight - iconDp) / 2f).toInt().coerceIn(4, if (large) 14 else 10)
        }

        val paramArea = if (sideBySide) heightDp - 8f else heightDp - headerHeight - 10f
        val columns = if (sideBySide || heightDp >= widthDp * 1.2f) {
            1
        } else {
            val singleColLine = paramArea / count
            val useOneColumn = widthDp < 130f || count <= 8 || singleColLine >= 15f
            if (useOneColumn) 1 else 2
        }
        val rows = if (columns == 1) count else (count + columns - 1) / 2
        val lineHeight = paramArea / rows.coerceAtLeast(1)
        val paramSpMin = when { large -> 13; tall -> 14; medium -> 13; else -> 12 }
        val paramSpMax = when { large -> 18; sideBySide -> 15; tall -> 17; medium -> 15; else -> 16 }
        val linePadMax = when { large -> 24; tall -> 22; medium -> 18; else -> 16 }
        val paramSp = ((lineHeight - 3f) / 1.45f).toInt().coerceIn(
            if (sideBySide) 11 else paramSpMin,
            paramSpMax
        )
        val linePadding = (lineHeight - paramSp * 1.35f).toInt().coerceIn(
            if (sideBySide) 2 else 4,
            linePadMax
        )

        val maxLineHeight = paramSp * 1.35f + linePadding
        val maxRows = (paramArea / maxLineHeight).toInt().coerceAtLeast(1)
        val capacity = maxRows * columns

        return WidgetParamsLayoutSpec(
            placement = WidgetParamsPlacement.Split,
            cityIconDp = iconDp,
            cityTempSp = tempSp,
            cityLabelSp = citySp,
            paramSp = paramSp,
            maxParamLines = capacity.coerceAtLeast(count),
            paramColumns = columns,
            paramLinePaddingDp = linePadding,
            headerPaddingVerticalDp = headerPadV,
            stackedCityHeader = false,
            sideBySideParams = sideBySide,
            compactParams = sideBySide || columns > 1,
            showCity = true
        )
    }

    /** Tall narrow layout (1×2, 1×3): city column + params; 1×3 uses side-by-side row. */
    fun bottomForParams(widthDp: Float, heightDp: Float, paramCount: Int): WidgetParamsLayoutSpec {
        val count = paramCount.coerceAtLeast(1)
        val tall = heightDp >= 220f
        val sideBySide = usesSideBySideParams(widthDp, heightDp)

        if (sideBySide) {
            val leftWidth = widthDp * 0.42f
            val contentH = heightDp - 8f
            val iconDp = (contentH * 0.22f).toInt().coerceIn(28, minOf((leftWidth * 0.88f).toInt(), 44))
            val tempSp = (contentH * 0.12f).toInt().coerceIn(18, 24)
            val citySp = (tempSp * 0.48f).toInt().coerceIn(10, 12)

            val paramArea = heightDp - 8f
            val lineHeight = paramArea / count
            val paramSp = ((lineHeight - 2f) / 1.45f).toInt().coerceIn(10, 13)
            val linePadding = (lineHeight - paramSp * 1.30f).toInt().coerceIn(1, 5)
            val maxLineHeight = paramSp * 1.30f + linePadding
            val maxRows = (paramArea / maxLineHeight).toInt().coerceAtLeast(1)

            return WidgetParamsLayoutSpec(
                placement = WidgetParamsPlacement.Bottom,
                cityIconDp = iconDp,
                cityTempSp = tempSp,
                cityLabelSp = citySp,
                paramSp = paramSp,
                maxParamLines = maxRows.coerceAtLeast(count),
                paramColumns = 1,
                paramLinePaddingDp = linePadding,
                headerPaddingVerticalDp = 2,
                stackedCityHeader = false,
                sideBySideParams = true,
                compactParams = true,
                showCity = true
            )
        }

        val cellSide = widthDp.coerceIn(70f, 110f)
        val iconDp = iconFor(cellSide)
        val tempSp = tempFor(cellSide)
        val citySp = labelFor(cellSide)
        val headerBlock = iconDp + tempSp * 1.35f + 8f

        val paramArea = (heightDp - headerBlock - 8f).coerceAtLeast(40f)
        val lineHeight = paramArea / count
        val paramSpMin = if (tall) 10 else 9
        val paramSpMax = if (tall) 13 else 12
        val paramSp = ((lineHeight - 3f) / 1.45f).toInt().coerceIn(paramSpMin, paramSpMax)
        val linePadding = (lineHeight - paramSp * 1.30f).toInt().coerceIn(1, if (tall) 6 else 3)

        val maxLineHeight = paramSp * 1.30f + linePadding
        val maxRows = (paramArea / maxLineHeight).toInt().coerceAtLeast(1)

        return WidgetParamsLayoutSpec(
            placement = WidgetParamsPlacement.Bottom,
            cityIconDp = iconDp,
            cityTempSp = tempSp,
            cityLabelSp = citySp,
            paramSp = paramSp,
            maxParamLines = maxRows.coerceAtLeast(count),
            paramColumns = 1,
            paramLinePaddingDp = linePadding,
            headerPaddingVerticalDp = 4,
            stackedCityHeader = false,
            compactParams = true,
            showCity = false
        )
    }

    /** Wide strip (2×1): left half = 1×1 (icon + temp), right half = params. */
    fun rightForParams(widthDp: Float, heightDp: Float, paramCount: Int): WidgetParamsLayoutSpec {
        val count = paramCount.coerceAtLeast(1)
        val short = heightDp < 100f
        val wide = widthDp >= 240f
        val cellSide = minOf(heightDp, widthDp * 0.48f).coerceIn(70f, 110f)

        val iconDp = iconFor(cellSide)
        val tempSp = tempFor(cellSide)
        val citySp = labelFor(cellSide)

        // Scale all param lines to fit height — show every enabled param when possible.
        val paramAreaH = (heightDp - 4f).coerceAtLeast(36f)
        val rawLineH = paramAreaH / count
        val paramSpMin = 7
        val paramSpMax = when { wide -> 11; short -> 10; else -> 11 }
        var paramSp = ((rawLineH - 1f) / 1.30f).toInt().coerceIn(paramSpMin, paramSpMax)
        var linePadding = (rawLineH - paramSp * 1.22f).toInt().coerceIn(0, if (short) 1 else 2)
        while (paramSp > paramSpMin &&
            (paramSp * 1.22f + linePadding) * count > paramAreaH + 0.5f
        ) {
            paramSp--
            linePadding = (rawLineH - paramSp * 1.22f).toInt().coerceIn(0, 2)
        }

        return WidgetParamsLayoutSpec(
            placement = WidgetParamsPlacement.Right,
            cityIconDp = iconDp,
            cityTempSp = tempSp,
            cityLabelSp = citySp,
            paramSp = paramSp,
            maxParamLines = count,
            paramColumns = 1,
            paramLinePaddingDp = linePadding,
            headerPaddingVerticalDp = 0,
            stackedCityHeader = false,
            compactParams = true,
            showCity = true
        )
    }

    private fun placement(widthDp: Float, heightDp: Float, minSide: Float): WidgetParamsPlacement {
        if (widthDp < 95f && heightDp < 95f) return WidgetParamsPlacement.Compact
        if (isHorizontalStrip(widthDp, heightDp)) return WidgetParamsPlacement.Right
        if (isVerticalStrip(widthDp, heightDp)) return WidgetParamsPlacement.Bottom
        if (usesSplitLayout(widthDp, heightDp)) return WidgetParamsPlacement.Split
        if (minSide >= 110f) return WidgetParamsPlacement.Split
        return WidgetParamsPlacement.Compact
    }

    /** Square or moderately tall/wide with enough width for a horizontal header row. */
    private fun usesSplitLayout(widthDp: Float, heightDp: Float): Boolean {
        if (isHorizontalStrip(widthDp, heightDp)) return false
        if (isVerticalStrip(widthDp, heightDp) && heightDp < 220f) return false
        val min = minOf(widthDp, heightDp)
        val max = maxOf(widthDp, heightDp)
        if (min < 110f) return false
        // 2×3 portrait — header row + params below
        if (widthDp >= 110f && heightDp >= widthDp * 1.45f) return true
        val ratio = max / min
        if (ratio < 1.85f) return true
        if (min >= 240f && ratio < 1.15f) return true
        if (min >= 150f && ratio < 2.1f) return true
        return false
    }

    private data class CityBlock(val iconDp: Int, val tempSp: Int, val labelSp: Int)

    private fun cityBlockFor(
        placement: WidgetParamsPlacement,
        widthDp: Float,
        heightDp: Float,
        minSide: Float
    ): CityBlock {
        return when (placement) {
            WidgetParamsPlacement.Split -> CityBlock(40, 24, 12)
            WidgetParamsPlacement.Right -> when {
                heightDp < 95f -> CityBlock(22, 14, 8)
                heightDp < 130f -> CityBlock(28, 18, 9)
                else -> CityBlock(
                    (iconFor(minSide) * 0.85f).toInt().coerceAtLeast(24),
                    (tempFor(minSide) * 0.9f).toInt().coerceAtLeast(16),
                    labelFor(minSide)
                )
            }
            WidgetParamsPlacement.Bottom -> when {
                heightDp < 150f -> CityBlock(24, 16, 8)
                heightDp < 220f -> CityBlock(30, 20, 9)
                else -> CityBlock(
                    (iconFor(minSide) * 0.75f).toInt().coerceAtLeast(28),
                    (tempFor(minSide) * 0.85f).toInt().coerceAtLeast(18),
                    labelFor(minSide)
                )
            }
            WidgetParamsPlacement.Compact -> CityBlock(iconFor(minSide), tempFor(minSide), labelFor(minSide))
        }
    }

    private fun maxParamLines(
        placement: WidgetParamsPlacement,
        widthDp: Float,
        heightDp: Float,
        paramSp: Int,
        city: CityBlock
    ): Int {
        val lineHeight = paramSp * 1.35f + 3f
        val cityHeight = city.iconDp + city.tempSp * 1.3f + city.labelSp * 1.3f + 16f
        val available = when (placement) {
            WidgetParamsPlacement.Right -> heightDp - 8f
            WidgetParamsPlacement.Bottom -> heightDp - cityHeight - 18f
            else -> 0f
        }
        if (available <= lineHeight) return 0
        return (available / lineHeight).toInt().coerceIn(1, 12)
    }

    private fun iconFor(minSide: Float) = when {
        minSide < 70 -> 26
        minSide < 110 -> 34
        minSide < 170 -> 48
        minSide < 240 -> 64
        else -> 80
    }

    private fun tempFor(minSide: Float) = when {
        minSide < 70 -> 16
        minSide < 110 -> 22
        minSide < 170 -> 32
        minSide < 240 -> 44
        else -> 56
    }

    private fun labelFor(minSide: Float) = when {
        minSide < 70 -> 9
        minSide < 110 -> 10
        minSide < 170 -> 11
        else -> 12
    }
}
