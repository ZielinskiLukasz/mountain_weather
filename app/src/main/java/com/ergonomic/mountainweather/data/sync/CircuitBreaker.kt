package com.ergonomic.mountainweather.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeoutMs: Long = 60_000
) {
    private var failureCount = 0
    private var lastFailureTime = 0L

    private val _state = MutableStateFlow(CircuitState.CLOSED)
    val state: StateFlow<CircuitState> = _state

    @Synchronized
    fun recordSuccess() {
        failureCount = 0
        _state.value = CircuitState.CLOSED
    }

    @Synchronized
    fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        if (failureCount >= failureThreshold) {
            _state.value = CircuitState.OPEN
        }
    }

    @Synchronized
    fun canExecute(): Boolean {
        return when (_state.value) {
            CircuitState.CLOSED -> true
            CircuitState.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime >= resetTimeoutMs) {
                    _state.value = CircuitState.HALF_OPEN
                    true
                } else {
                    false
                }
            }
            CircuitState.HALF_OPEN -> true
        }
    }

    suspend fun <T> execute(block: suspend () -> T): T {
        if (!canExecute()) {
            throw CircuitOpenException()
        }
        return try {
            val result = block()
            recordSuccess()
            result
        } catch (e: Exception) {
            recordFailure()
            throw e
        }
    }
}

class CircuitOpenException : Exception("Circuit breaker is open, skipping network request")
