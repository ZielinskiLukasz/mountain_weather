package com.ergonomic.mountainweather.widget

import com.ergonomic.mountainweather.util.WeatherParamLine

/** Origin of the resolved location shown in the Compact widget. */
enum class CompactSource { Pinned, FollowMain }

sealed interface CompactData {
    object NoData : CompactData
    data class Ready(
        val cityName: String,
        val latitude: Double,
        val longitude: Double,
        val source: CompactSource,
        val temperature: Double?,
        val apparentTemperature: Double?,
        val temperatureMax: Double?,
        val temperatureMin: Double?,
        val weatherCode: Int?,
        val isDay: Boolean,
        val cachedAt: Long,
        val params: List<WeatherParamLine> = emptyList(),
        val theme: WidgetPrefs.Theme = WidgetPrefs.Theme.SYSTEM,
        val opacityPct: Int = WidgetPrefs.DEFAULT_OPACITY
    ) : CompactData
}

object WidgetCompactLayout {

    enum class Size { S, M, L }

    data class Spec(
        val size: Size,
        val iconDp: Int,
        val tempSp: Int,
        val citySp: Int,
        val secondarySp: Int,
        val paramsSp: Int,
        val cornerIconDp: Int,
        val cornerPaddingDp: Int,
        val showCity: Boolean,
        val showSecondary: Boolean,
        val showHiLo: Boolean,
        val maxParams: Int,
        /** When true, params render in a right-hand column instead of stacked below the temperature. */
        val paramsOnRight: Boolean
    )

    fun resolve(widthDp: Float, heightDp: Float): Spec {
        val minSide = minOf(widthDp, heightDp)
        // Landscape = wide + short, where stacking params under the temp would
        // either truncate them or waste empty right-hand space. Heuristic: width
        // is at least 1.7× height and there is room for a second text column.
        val landscape = heightDp > 0f && widthDp / heightDp >= 1.7f && widthDp >= 220f
        return when {
            // 1×1-ish: just icon + temperature.
            widthDp < 130f || heightDp < 70f -> Spec(
                size = Size.S,
                iconDp = 30,
                tempSp = 22,
                citySp = 11,
                secondarySp = 10,
                paramsSp = 10,
                cornerIconDp = 16,
                cornerPaddingDp = 6,
                showCity = heightDp >= 80f,
                showSecondary = false,
                showHiLo = false,
                maxParams = 0,
                paramsOnRight = false
            )
            // 2×1: icon + temp + city + short description, single row.
            heightDp < 130f -> Spec(
                size = Size.M,
                iconDp = 42,
                tempSp = 32,
                citySp = 13,
                secondarySp = 12,
                paramsSp = 11,
                cornerIconDp = 18,
                cornerPaddingDp = 8,
                showCity = true,
                showSecondary = true,
                showHiLo = false,
                maxParams = if (landscape) 3 else if (widthDp >= 220f) 2 else 1,
                paramsOnRight = landscape
            )
            // 2×2+: add hi/lo + feels-like.
            else -> Spec(
                size = if (minSide < 170f) Size.M else Size.L,
                iconDp = if (minSide < 170f) 56 else 78,
                tempSp = if (minSide < 170f) 40 else 52,
                citySp = if (minSide < 170f) 14 else 16,
                secondarySp = if (minSide < 170f) 12 else 14,
                paramsSp = if (minSide < 170f) 11 else 13,
                cornerIconDp = if (minSide < 170f) 22 else 26,
                cornerPaddingDp = 10,
                showCity = true,
                showSecondary = true,
                showHiLo = true,
                maxParams = if (minSide < 170f) 3 else 4,
                paramsOnRight = landscape
            )
        }
    }
}
