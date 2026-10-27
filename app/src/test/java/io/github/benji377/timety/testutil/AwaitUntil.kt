package io.github.benji377.timety.testutil

import android.os.Looper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf

private const val DEFAULT_TIMEOUT_MS = 5_000L
private const val POLL_INTERVAL_MS = 20L

/**
 * Blocks until [block] returns true; returns false if it never does within [timeoutMs].
 *
 * Robolectric runs the test on the main looper, so each poll drains it: `viewModelScope` work
 * posted there would otherwise never run while the test waits.
 */
fun awaitTrue(timeoutMs: Long = DEFAULT_TIMEOUT_MS, block: () -> Boolean): Boolean {
    val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt()
    repeat(attempts) {
        shadowOf(Looper.getMainLooper()).idle()
        if (block()) return true
        Thread.sleep(POLL_INTERVAL_MS)
    }
    return false
}

/** Lets pending main-looper and background work settle for [ms] before a negative assertion. */
fun settle(ms: Long = 300L) {
    awaitTrue(ms) { false }
}

/** The flow's current value, for assertions that read repository state synchronously. */
fun <T> Flow<T>.value(): T = runBlocking { first() }
