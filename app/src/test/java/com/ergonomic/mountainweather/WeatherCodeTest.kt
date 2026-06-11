package com.ergonomic.mountainweather

import com.ergonomic.mountainweather.util.resolveIsDay
import com.ergonomic.mountainweather.util.weatherCodeToInfo
import com.ergonomic.mountainweather.util.windDirectionToArrow
import org.junit.Assert.*
import org.junit.Test

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
}
