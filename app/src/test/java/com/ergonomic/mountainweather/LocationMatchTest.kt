package com.ergonomic.mountainweather

import com.ergonomic.mountainweather.util.isSamePlace
import com.ergonomic.mountainweather.util.isSamePoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationMatchTest {

    @Test
    fun `identical coordinates are the same point`() {
        assertTrue(isSamePoint(49.299, 19.949, 49.299, 19.949))
        assertFalse(isSamePoint(49.299, 19.949, 49.310, 19.949))
    }

    @Test
    fun `search coords near a saved favourite with the same name match`() {
        assertTrue(
            isSamePlace(
                lat1 = 49.299, lon1 = 19.958, name1 = "Zakopane",
                lat2 = 49.300, lon2 = 19.949, name2 = "Zakopane",
                country1 = "Poland", country2 = "Poland"
            )
        )
    }

    @Test
    fun `same name far away does not match`() {
        assertFalse(
            isSamePlace(
                lat1 = 50.06, lon1 = 19.94, name1 = "Zakopane",
                lat2 = 49.30, lon2 = 19.95, name2 = "Zakopane"
            )
        )
    }

    @Test
    fun `same name different country does not match`() {
        assertFalse(
            isSamePlace(
                lat1 = 40.0, lon1 = -75.0, name1 = "Paris",
                lat2 = 40.01, lon2 = -75.01, name2 = "Paris",
                country1 = "United States", country2 = "France"
            )
        )
    }
}
