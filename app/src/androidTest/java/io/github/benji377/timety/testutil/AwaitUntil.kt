package io.github.benji377.timety.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val DEFAULT_TIMEOUT_MS = 5_000L
private const val POLL_INTERVAL_MS = 50L

/**
 * Blocks until [block] returns a non-null value, or the timeout expires.
 *
 * View model mutators are fire-and-forget `viewModelScope.launch` calls, and test methods run on
 * the instrumentation thread rather than the main looper, so polling from here observes that work
 * landing without blocking the dispatcher that performs it.
 */
fun <T : Any> awaitNotNull(timeoutMs: Long = DEFAULT_TIMEOUT_MS, block: () -> T?): T? {
    val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt()
    repeat(attempts) {
        block()?.let { return it }
        Thread.sleep(POLL_INTERVAL_MS)
    }
    return null
}

/** Blocks until [block] returns true; returns false if it never does within [timeoutMs]. */
fun awaitTrue(timeoutMs: Long = DEFAULT_TIMEOUT_MS, block: () -> Boolean): Boolean {
    val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt()
    repeat(attempts) {
        if (block()) return true
        Thread.sleep(POLL_INTERVAL_MS)
    }
    return false
}

/** The flow's current value, for assertions that read repository state synchronously. */
fun <T> Flow<T>.value(): T = runBlocking { first() }

/**
 * Polls [this] until [predicate] holds, then returns the matching emission. Fails the calling test
 * with [message] if it never does, which reads better than a bare null-pointer further down.
 */
fun <T> Flow<T>.awaitValue(
    message: String,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    predicate: (T) -> Boolean,
): T {
    val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt()
    var last: T? = null
    repeat(attempts) {
        val current = value()
        last = current
        if (predicate(current)) return current
        Thread.sleep(POLL_INTERVAL_MS)
    }
    throw AssertionError("$message (last value: $last)")
}
