package com.example.mountainweather

import com.example.mountainweather.data.repository.ForecastSettings
import org.junit.Assert.*
import org.junit.Test

class ForecastSettingsTest {

    @Test
    fun `default settings have hourly enabled`() {
        val s = ForecastSettings()
        assertTrue(s.showHourly)
    }

    @Test
    fun `default settings have daily5 enabled`() {
        val s = ForecastSettings()
        assertTrue(s.showDaily5)
    }

    @Test
    fun `default settings have daily3 disabled`() {
        val s = ForecastSettings()
        assertFalse(s.showDaily3)
    }

    @Test
    fun `default settings have resilient sync off`() {
        val s = ForecastSettings()
        assertFalse(s.resilientSync)
    }

    @Test
    fun `default settings have background sync off`() {
        val s = ForecastSettings()
        assertEquals(0, s.syncIntervalMinutes)
    }

    @Test
    fun `daily3 and daily5 can both be false`() {
        val s = ForecastSettings(showDaily3 = false, showDaily5 = false)
        assertFalse(s.showDaily3)
        assertFalse(s.showDaily5)
    }

    @Test
    fun `copy preserves other fields`() {
        val s = ForecastSettings(showHourly = false, showDaily5 = false, resilientSync = true)
        val s2 = s.copy(syncIntervalMinutes = 60)
        assertFalse(s2.showHourly)
        assertFalse(s2.showDaily5)
        assertTrue(s2.resilientSync)
        assertEquals(60, s2.syncIntervalMinutes)
    }
}
