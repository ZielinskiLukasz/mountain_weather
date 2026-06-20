package com.ergonomic.mountainweather.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color

/**
 * Resolves background and text colors for the Compact widget based on the
 * per-instance [WidgetPrefs.Theme] override and [opacityPct] (0..100).
 *
 * Used in two places:
 *  - Glance UI ([WeatherCompactWidget]) to paint the actual widget.
 *  - Compose live preview in `WidgetConfigActivity` so the picker shows the
 *    same colors the widget will end up using.
 *
 * Keeping this in one place avoids drift between preview and runtime widget.
 */
object WidgetCompactPalette {

    data class Palette(val background: Color, val text: Color, val isDark: Boolean)

    fun resolve(
        context: Context,
        theme: WidgetPrefs.Theme,
        opacityPct: Int
    ): Palette {
        val isDark = when (theme) {
            WidgetPrefs.Theme.LIGHT -> false
            WidgetPrefs.Theme.DARK -> true
            WidgetPrefs.Theme.SYSTEM -> {
                val uiMode = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        // Same RGB values as @color/widget_bg_light / widget_bg_dark; alpha is per‑instance.
        val rgb: Long = if (isDark) 0x1F2128L else 0xF6F6F6L
        val alphaInt: Long = (opacityPct.coerceIn(0, 100) * 255L / 100L)
        val argb: Long = (alphaInt shl 24) or rgb
        val textColor = if (isDark) Color.White else Color(0xFF1F2128)
        return Palette(background = Color(argb.toInt()), text = textColor, isDark = isDark)
    }
}
