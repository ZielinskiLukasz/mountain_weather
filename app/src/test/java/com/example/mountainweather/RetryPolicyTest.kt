package com.example.mountainweather

import com.example.mountainweather.data.sync.RetryPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun `succeeds on first attempt`() = runTest {
        val policy = RetryPolicy(maxRetries = 3, initialDelayMs = 0)
        val result = policy.execute { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun `succeeds after transient failures`() = runTest {
        var attempt = 0
        val policy = RetryPolicy(maxRetries = 3, initialDelayMs = 0)
        val result = policy.execute {
            attempt++
            if (attempt < 3) throw RuntimeException("fail #$attempt")
            "recovered"
        }
        assertEquals("recovered", result)
        assertEquals(3, attempt)
    }

    @Test
    fun `throws after max retries exhausted`() = runTest {
        val policy = RetryPolicy(maxRetries = 2, initialDelayMs = 0)
        try {
            policy.execute { throw RuntimeException("always fails") }
            fail("Should have thrown")
        } catch (e: RuntimeException) {
            assertEquals("always fails", e.message)
        }
    }

    @Test
    fun `single retry succeeds`() = runTest {
        val policy = RetryPolicy(maxRetries = 1, initialDelayMs = 0)
        val result = policy.execute { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `single retry fails`() = runTest {
        val policy = RetryPolicy(maxRetries = 1, initialDelayMs = 0)
        try {
            policy.execute { throw IllegalStateException("boom") }
            fail("Should have thrown")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }
}
