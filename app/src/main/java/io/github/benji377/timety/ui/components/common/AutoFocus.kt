package io.github.benji377.timety.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * A [FocusRequester] that focuses its field and opens the keyboard once, the first time the field
 * shows with [enabled] set. Saved across rotation, so the keyboard doesn't pop up again.
 */
@Composable
fun rememberAutoFocus(enabled: Boolean = true): FocusRequester {
    val requester = remember { FocusRequester() }
    var done by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(enabled) {
        if (enabled && !done) {
            // A dialog's window can't take focus on its first frame.
            withFrameNanos {}
            requester.requestFocus()
            keyboard?.show()
            done = true
        }
    }
    return requester
}
