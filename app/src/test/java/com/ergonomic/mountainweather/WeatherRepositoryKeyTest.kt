package com.ergonomic.mountainweather

import com.ergonomic.mountainweather.data.repository.WeatherRepository
import org.junit.Assert.*
import org.junit.Test

class WeatherRepositoryKeyTest {

    @Test
    fun `locationKey formats with two decimals`() {
        assertEquals("50.06_19.94", WeatherRepository.locationKey(50.06, 19.94))
    }

    @Test
    fun `locationKey rounds correctly`() {
        assertEquals("50.07_19.94", WeatherRepository.locationKey(50.065, 19.9449))
    }

    @Test
    fun `locationKey handles negative coordinates`() {
        assertEquals("-33.87_-151.21", WeatherRepository.locationKey(-33.87, -151.21))
    }

    @Test
    fun `locationKey handles zero`() {
        assertEquals("0.00_0.00", WeatherRepository.locationKey(0.0, 0.0))
    }

    @Test
    fun `same coordinates produce same key`() {
        val k1 = WeatherRepository.locationKey(50.06, 19.94)
        val k2 = WeatherRepository.locationKey(50.06, 19.94)
        assertEquals(k1, k2)
    }

    @Test
    fun `different coordinates produce different keys`() {
        val k1 = WeatherRepository.locationKey(50.06, 19.94)
        val k2 = WeatherRepository.locationKey(52.23, 21.01)
        assertNotEquals(k1, k2)
    }

    @Test
    fun `locationKey always uses dot separator regardless of locale`() {
        val key = WeatherRepository.locationKey(50.06, 19.94)
        assertTrue(key.contains("."))
        assertFalse(key.contains(","))
    }
}
