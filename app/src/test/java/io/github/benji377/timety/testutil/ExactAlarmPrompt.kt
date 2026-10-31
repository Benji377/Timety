package io.github.benji377.timety.testutil

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.R

/**
 * Dismisses the exact-alarm prompt, which shows once on a fresh install and would swallow the first
 * tap. It does not appear when the permission is already granted, so a timeout is not a failure.
 */
fun ComposeTestRule.dismissExactAlarmPrompt() {
    val label = InstrumentationRegistry.getInstrumentation().targetContext
        .getString(R.string.exactAlarmPromptDismiss)
    val appeared = runCatching {
        waitUntil(timeoutMillis = 3_000) { onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
    }.isSuccess
    if (appeared) {
        onNodeWithText(label).performClick()
        waitForIdle()
    }
}
