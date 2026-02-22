package com.example.mountainweather.data.sync

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

class RetryPolicy(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000,
    private val backoffMultiplier: Double = 2.0
) {
    suspend fun <T> execute(block: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delayMs = min(
                        (initialDelayMs * backoffMultiplier.pow(attempt.toDouble())).toLong(),
                        maxDelayMs
                    )
                    delay(delayMs)
                }
            }
        }
        throw lastException!!
    }
}
