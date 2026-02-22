package com.example.mountainweather

import com.example.mountainweather.util.weatherCodeToInfo
import com.example.mountainweather.util.windDirectionToArrow
import org.junit.Assert.*
import org.junit.Test

class WeatherCodeTest {

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
