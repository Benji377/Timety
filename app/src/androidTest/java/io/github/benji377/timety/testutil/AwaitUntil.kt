package io.github.benji377.timety.testutil

private const val DEFAULT_TIMEOUT_MS = 5_000L
private const val POLL_INTERVAL_MS = 50L

/**
 * Blocks until [block] returns true; returns false if it never does within [timeoutMs]. Tests run
 * on the instrumentation thread, so polling observes main-thread work without blocking it.
 */
fun awaitTrue(timeoutMs: Long = DEFAULT_TIMEOUT_MS, block: () -> Boolean): Boolean {
    val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt()
    repeat(attempts) {
        if (block()) return true
        Thread.sleep(POLL_INTERVAL_MS)
    }
    return false
}
