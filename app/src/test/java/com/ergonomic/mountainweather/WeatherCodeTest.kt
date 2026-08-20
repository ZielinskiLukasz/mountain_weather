package com.ergonomic.mountainweather

import com.ergonomic.mountainweather.util.dryEquivalentWeatherCode
import com.ergonomic.mountainweather.util.formatPrecipitationMm
import com.ergonomic.mountainweather.util.resolveIsDay
import com.ergonomic.mountainweather.util.weatherCodeToInfo
import com.ergonomic.mountainweather.util.windDirectionToArrow
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class WeatherCodeTest {

    @Test
    fun `resolveIsDay uses API flag when available`() {
        assertTrue(resolveIsDay(isDayFromApi = 1))
        assertFalse(resolveIsDay(isDayFromApi = 0))
    }

    @Test
    fun `resolveIsDay uses sunrise and sunset window`() {
        assertTrue(resolveIsDay(
            timeIso = "2024-06-09T12:00",
            sunriseIso = "2024-06-09T05:00",
            sunsetIso = "2024-06-09T21:00"
        ))
        assertFalse(resolveIsDay(
            timeIso = "2024-06-09T22:00",
            sunriseIso = "2024-06-09T05:00",
            sunsetIso = "2024-06-09T21:00"
        ))
    }

    @Test
    fun `resolveIsDay falls back to hour heuristic`() {
        assertTrue(resolveIsDay(timeIso = "2024-06-09T12:00"))
        assertFalse(resolveIsDay(timeIso = "2024-06-09T22:00"))
    }

    @Test
    fun `clear sky returns sun icon`() {
        val info = weatherCodeToInfo(0, isDay = true)
        assertEquals("☀️", info.icon)
        assertEquals(R.string.wc_clear, info.descriptionRes)
    }

    @Test
    fun `clear sky at night returns moon icon`() {
        val info = weatherCodeToInfo(0, isDay = false)
        assertEquals("🌙", info.icon)
    }

    @Test
    fun `overcast returns cloud icon`() {
        val info = weatherCodeToInfo(3)
        assertEquals("☁️", info.icon)
        assertEquals(R.string.wc_overcast, info.descriptionRes)
    }

    @Test
    fun `fog codes return fog icon`() {
        assertEquals("🌫️", weatherCodeToInfo(45).icon)
        assertEquals("🌫️", weatherCodeToInfo(48).icon)
    }

    @Test
    fun `heavy snow returns snowflake`() {
        val info = weatherCodeToInfo(75)
        assertEquals("❄️", info.icon)
        assertEquals(R.string.wc_heavy_snow, info.descriptionRes)
    }

    @Test
    fun `thunderstorm with hail`() {
        val info = weatherCodeToInfo(96)
        assertEquals("⛈️", info.icon)
        assertEquals(R.string.wc_thunderstorm_hail, info.descriptionRes)
    }

    @Test
    fun `unknown code returns question mark`() {
        val info = weatherCodeToInfo(999)
        assertEquals("❓", info.icon)
        assertEquals(R.string.wc_unknown, info.descriptionRes)
    }

    @Test
    fun `wind direction north`() {
        assertEquals("↑ N", windDirectionToArrow(0))
        assertEquals("↑ N", windDirectionToArrow(10))
    }

    @Test
    fun `wind direction east`() {
        assertEquals("→ E", windDirectionToArrow(90))
    }

    @Test
    fun `wind direction south`() {
        assertEquals("↓ S", windDirectionToArrow(180))
    }

    @Test
    fun `wind direction west`() {
        assertEquals("← W", windDirectionToArrow(270))
    }

    @Test
    fun `wind direction northeast`() {
        assertEquals("↗ NE", windDirectionToArrow(45))
    }

    @Test
    fun `wind direction southwest`() {
        assertEquals("↙ SW", windDirectionToArrow(225))
    }

    @Test
    fun `zero precipitation hides millimetres`() {
        assertNull(formatPrecipitationMm(0.0, Locale.US))
        assertNull(formatPrecipitationMm(-0.0, Locale.US))
    }

    @Test
    fun `trace precipitation displays as 0_1mm not 0_0`() {
        assertEquals("0.1mm", formatPrecipitationMm(0.01, Locale.US))
        assertEquals("0.1mm", formatPrecipitationMm(0.04, Locale.US))
        assertEquals("0.1mm", formatPrecipitationMm(0.05, Locale.US))
        assertEquals("0.1mm", formatPrecipitationMm(0.1, Locale.US))
        assertEquals("0.2mm", formatPrecipitationMm(0.15, Locale.US))
        assertEquals("1.2mm", formatPrecipitationMm(1.23, Locale.US))
    }

    @Test
    fun `zero precipitation replaces rain icon with dry equivalent`() {
        assertEquals(3, dryEquivalentWeatherCode(61, 0.0))
        assertEquals(2, dryEquivalentWeatherCode(80, 0.0))
        assertEquals(61, dryEquivalentWeatherCode(61, 0.01))
        assertEquals(3, dryEquivalentWeatherCode(3, 0.0))
    }
}
