package com.example.mountainweather

import com.example.mountainweather.data.sync.CircuitBreaker
import com.example.mountainweather.data.sync.CircuitOpenException
import com.example.mountainweather.data.sync.CircuitState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CircuitBreakerTest {

    @Test
    fun `starts in CLOSED state`() {
        val cb = CircuitBreaker(failureThreshold = 3)
        assertEquals(CircuitState.CLOSED, cb.state.value)
        assertTrue(cb.canExecute())
    }

    @Test
    fun `stays CLOSED below failure threshold`() {
        val cb = CircuitBreaker(failureThreshold = 3)
        cb.recordFailure()
        cb.recordFailure()
        assertEquals(CircuitState.CLOSED, cb.state.value)
        assertTrue(cb.canExecute())
    }

    @Test
    fun `opens after reaching failure threshold`() {
        val cb = CircuitBreaker(failureThreshold = 3)
        repeat(3) { cb.recordFailure() }
        assertEquals(CircuitState.OPEN, cb.state.value)
        assertFalse(cb.canExecute())
    }

    @Test
    fun `resets to CLOSED on success`() {
        val cb = CircuitBreaker(failureThreshold = 3)
        cb.recordFailure()
        cb.recordFailure()
        cb.recordSuccess()
        assertEquals(CircuitState.CLOSED, cb.state.value)
        assertTrue(cb.canExecute())
    }

    @Test
    fun `transitions to HALF_OPEN after timeout`() {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeoutMs = 0)
        cb.recordFailure()
        assertEquals(CircuitState.OPEN, cb.state.value)
        // resetTimeoutMs = 0 so it should immediately transition
        assertTrue(cb.canExecute())
        assertEquals(CircuitState.HALF_OPEN, cb.state.value)
    }

    @Test
    fun `execute succeeds and records success`() = runTest {
        val cb = CircuitBreaker(failureThreshold = 3)
        val result = cb.execute { "ok" }
        assertEquals("ok", result)
        assertEquals(CircuitState.CLOSED, cb.state.value)
    }

    @Test
    fun `execute failure records failure and rethrows`() = runTest {
        val cb = CircuitBreaker(failureThreshold = 3)
        try {
            cb.execute { throw RuntimeException("err") }
            fail("Should have thrown")
        } catch (e: RuntimeException) {
            assertEquals("err", e.message)
        }
    }

    @Test
    fun `execute throws CircuitOpenException when OPEN`() = runTest {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeoutMs = 60_000)
        cb.recordFailure()
        try {
            cb.execute { "should not run" }
            fail("Should have thrown CircuitOpenException")
        } catch (e: CircuitOpenException) {
            // expected
        }
    }
}
