package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Today's date that rolls over at midnight while the screen stays open, so "today" figures don't
 * stay on yesterday until the user navigates away and back.
 */
@Composable
fun rememberToday(): State<LocalDate> = produceState(LocalDate.now()) {
    while (true) {
        val nextMidnight = value.plusDays(1).atStartOfDay()
        val wait = Duration.between(LocalDateTime.now(ZoneId.systemDefault()), nextMidnight).toMillis()
        // A small margin past midnight, and a cap so a clock or time-zone change is caught up on.
        delay(wait.coerceIn(0, MAX_WAIT_MS) + MIDNIGHT_MARGIN_MS)
        value = LocalDate.now()
    }
}

private const val MIDNIGHT_MARGIN_MS = 1_000L
private const val MAX_WAIT_MS = 15 * 60_000L
