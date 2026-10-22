package io.github.benji377.timety.uiflow

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.focus.DailyFocusTarget
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.ui.screens.settings.SettingsPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Walks the split settings screen: the landing page lists every [SettingsPage], each page opens
 * from it, a switch on a subpage really persists, and the Focus screen's goal badge lands
 * directly on the Focus page.
 */
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {

    private val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val compose = createAndroidComposeRule<MainActivity>()

    // The notification permission has to be granted before the activity launches, or its prompt
    // covers the screen and swallows the taps below.
    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(permission).around(compose)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val settings get() = (context.applicationContext as TimetyApplication).container.settingsRepository

    private fun str(resId: Int): String = context.getString(resId)

    @Before
    fun seedSettings() {
        runBlocking {
            settings.saveShowHomeGreeting(true)
            // The Focus screen only shows its goal badge on days with a target.
            settings.saveDailyGoalMins(30)
            settings.saveDailyGoalWeekdays(DailyFocusTarget.ALL_WEEKDAYS)
        }
        dismissExactAlarmPrompt()
    }

    @After
    fun restoreSettings() {
        runBlocking {
            settings.saveShowHomeGreeting(true)
            settings.saveDailyGoalMins(DailyFocusTarget.DEFAULT_MINUTES)
        }
    }

    @Test
    fun landingPage_listsAndOpensEveryPage() {
        openSettingsFromProfile()

        SettingsPage.entries.forEach { page ->
            compose.onNodeWithText(str(page.summaryRes)).assertIsDisplayed()
        }

        SettingsPage.entries.forEach { page ->
            compose.onNodeWithText(str(page.titleRes)).performClick()
            compose.waitForIdle()
            // The summary only exists on the landing page, so its absence proves we navigated.
            assertTrue(
                "${page.name} should replace the landing page",
                compose.onAllNodesWithText(str(page.summaryRes)).fetchSemanticsNodes().isEmpty(),
            )
            compose.onNodeWithText(str(page.titleRes)).assertIsDisplayed()
            pressBack()
            compose.onNodeWithText(str(page.summaryRes)).assertIsDisplayed()
        }
    }

    @Test
    fun greetingSwitch_onGeneralPage_persists() {
        openSettingsFromProfile()
        compose.onNodeWithText(str(SettingsPage.GENERAL.titleRes)).performClick()
        compose.waitForIdle()

        val greetingRow = compose.onNodeWithText(str(R.string.settingsLabelShowGreeting))
        greetingRow.assertIsOn()
        greetingRow.performClick()
        compose.waitForIdle()
        greetingRow.assertIsOff()

        assertTrue(
            "turning the switch off should store the setting",
            awaitTrue { runBlocking { settings.showHomeGreetingFlow.first() } == false },
        )

        // Leaving and re-entering the page must show the stored value, not a reset default.
        pressBack()
        compose.onNodeWithText(str(SettingsPage.GENERAL.titleRes)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.settingsLabelShowGreeting)).assertIsOff()
    }

    @Test
    fun focusGoalBadge_opensFocusSettingsPage_andBackReturnsToFocus() {
        compose.onNodeWithText(str(R.string.navigationFocus)).performClick()
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("/ 30 m", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("/ 30 m", substring = true).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(str(SettingsPage.FOCUS.titleRes)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.settingsLabelFocusGoal)).assertIsDisplayed()

        pressBack()
        assertEquals(
            "back should return to the Focus screen, not the settings landing page",
            1,
            compose.onAllNodesWithText("/ 30 m", substring = true).fetchSemanticsNodes().size,
        )
    }

    private fun openSettingsFromProfile() {
        compose.onNodeWithText(str(R.string.navigationProfile)).performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription(str(R.string.settingsTitle)).performClick()
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    // The exact-alarm prompt shows once on a fresh install and would swallow the first tap.
    // It does not appear when the permission is already granted, so a timeout is not a failure.
    private fun dismissExactAlarmPrompt() {
        val dismissLabel = str(R.string.exactAlarmPromptDismiss)
        val appeared = runCatching {
            compose.waitUntil(timeoutMillis = 3_000) {
                compose.onAllNodesWithText(dismissLabel).fetchSemanticsNodes().isNotEmpty()
            }
        }.isSuccess
        if (appeared) {
            compose.onNodeWithText(dismissLabel).performClick()
            compose.waitForIdle()
        }
    }
}
