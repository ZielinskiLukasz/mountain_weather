package com.ergonomic.mountainweather.widget

/** Estimates city-name font size — shrinks slightly before truncating very long names. */
object WidgetCityNameFit {

    private const val CITY_CHAR_RATIO = 0.48f
    private const val TEMP_CHAR_RATIO = 0.42f
    private const val ABSOLUTE_MIN_SP = 11
    private const val MAX_SHRINK_STEPS = 3

    fun fontSp(cityName: String, availableWidthDp: Float, preferredSp: Int): Int {
        if (availableWidthDp <= 0f || cityName.isEmpty()) return preferredSp
        val floorSp = maxOf(ABSOLUTE_MIN_SP, preferredSp - MAX_SHRINK_STEPS)
        if (fits(cityName, preferredSp, availableWidthDp)) return preferredSp
        for (sp in preferredSp - 1 downTo floorSp) {
            if (fits(cityName, sp, availableWidthDp)) return sp
        }
        return floorSp
    }

    fun availableForRow(
        widgetWidthDp: Float,
        iconDp: Int,
        tempSp: Int,
        tempText: String,
        hasIcon: Boolean,
        outerPaddingDp: Float = 12f
    ): Float {
        var used = outerPaddingDp + 4f
        if (hasIcon) used += iconDp + 6f
        used += tempText.length * tempSp * TEMP_CHAR_RATIO + 8f
        return (widgetWidthDp - used).coerceAtLeast(32f)
    }

    fun availableForColumn(widgetWidthDp: Float, outerPaddingDp: Float = 12f): Float =
        (widgetWidthDp - outerPaddingDp).coerceAtLeast(32f)

    private fun fits(text: String, sp: Int, availableWidthDp: Float): Boolean =
        text.length * sp * CITY_CHAR_RATIO <= availableWidthDp
}
