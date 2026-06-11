package com.ergonomic.mountainweather.widget

/** Shared size buckets for 1×1 resizable Glance widgets. */
internal data class WidgetLayoutSizes(
    val iconDp: Int,
    val tempSp: Int,
    val labelSp: Int,
    val minTempSp: Int,
    val cornerDp: Int,
    val cornerPaddingDp: Int,
    val showCity: Boolean,
    val showMinTemp: Boolean
)

internal object WidgetLayout {

    fun computeSizes(widthDp: Float, heightDp: Float): WidgetLayoutSizes {
        val minSide = minOf(widthDp, heightDp)
        return when {
            minSide < 70 -> WidgetLayoutSizes(
                iconDp = 26, tempSp = 16, labelSp = 9, minTempSp = 8,
                cornerDp = 14, cornerPaddingDp = 6,
                showCity = false, showMinTemp = false
            )
            minSide < 110 -> WidgetLayoutSizes(
                iconDp = 34, tempSp = 22, labelSp = 11, minTempSp = 10,
                cornerDp = 18, cornerPaddingDp = 8,
                showCity = heightDp >= 80, showMinTemp = heightDp >= 90
            )
            minSide < 170 -> WidgetLayoutSizes(
                iconDp = 54, tempSp = 36, labelSp = 14, minTempSp = 12,
                cornerDp = 24, cornerPaddingDp = 10,
                showCity = true, showMinTemp = true
            )
            minSide < 240 -> WidgetLayoutSizes(
                iconDp = 78, tempSp = 52, labelSp = 18, minTempSp = 14,
                cornerDp = 30, cornerPaddingDp = 12,
                showCity = true, showMinTemp = true
            )
            else -> WidgetLayoutSizes(
                iconDp = 100, tempSp = 64, labelSp = 22, minTempSp = 16,
                cornerDp = 36, cornerPaddingDp = 14,
                showCity = true, showMinTemp = true
            )
        }
    }

    fun sideSizes(center: WidgetLayoutSizes, widgetWidthDp: Float): WidgetLayoutSizes {
        val colWidth = widgetWidthDp / 3f
        return center.copy(
            iconDp = (center.iconDp * 0.55f).toInt().coerceAtLeast(14),
            tempSp = fitTempSp((center.tempSp * 0.65f).toInt(), colWidth),
            labelSp = (center.labelSp * 0.75f).toInt().coerceAtLeast(8),
            minTempSp = (center.minTempSp * 0.75f).toInt().coerceAtLeast(8),
            showCity = false,
            showMinTemp = false
        )
    }

    fun centerSizesForCarousel(base: WidgetLayoutSizes, widgetWidthDp: Float): WidgetLayoutSizes {
        val colWidth = widgetWidthDp / 3f
        return base.copy(tempSp = fitTempSp(base.tempSp, colWidth))
    }

    fun fitTempSp(requestedSp: Int, columnWidthDp: Float): Int {
        if (columnWidthDp <= 0f) return requestedSp
        val maxByWidth = (columnWidthDp / 2.5f).toInt().coerceAtLeast(8)
        return minOf(requestedSp, maxByWidth)
    }

    fun shouldShowWideLayout(widthDp: Float, hasPrevious: Boolean, hasNext: Boolean): Boolean {
        if (widthDp < 95f) return false
        return hasPrevious || hasNext
    }
}
