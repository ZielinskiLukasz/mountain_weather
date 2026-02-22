package com.example.mountainweather

import com.example.mountainweather.data.sync.SyncScheduler
import org.junit.Assert.*
import org.junit.Test

class SyncSchedulerTest {

    @Test
    fun `interval options contains expected values`() {
        val expected = listOf(0, 10, 30, 60, 180, 360, 720)
        assertEquals(expected, SyncScheduler.INTERVAL_OPTIONS)
    }

    @Test
    fun `interval options starts with zero (off)`() {
        assertEquals(0, SyncScheduler.INTERVAL_OPTIONS.first())
    }

    @Test
    fun `interval options are sorted ascending`() {
        val sorted = SyncScheduler.INTERVAL_OPTIONS.sorted()
        assertEquals(sorted, SyncScheduler.INTERVAL_OPTIONS)
    }

    @Test
    fun `all intervals are non-negative`() {
        assertTrue(SyncScheduler.INTERVAL_OPTIONS.all { it >= 0 })
    }
}
