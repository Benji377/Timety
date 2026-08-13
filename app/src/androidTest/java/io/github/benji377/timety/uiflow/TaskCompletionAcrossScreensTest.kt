package io.github.benji377.timety.uiflow

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.ui.components.task.taskCheckboxTag
import io.github.benji377.timety.util.stats.ExperienceEngine
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
import java.time.Instant

/**
 * Checks off a task on the task list and follows the consequences to the profile screen.
 *
 * Unlike the headless workflow tests this runs against the real activity and the real application
 * container, because the couplings it covers only exist inside composition: the checkbox is wired
 * through `rememberTaskCompletionToggle`, and the focus-log prompt it raises has no entry point
 * outside the UI. It seeds and clears the on-device database around itself, the way
 * `ScreenshotTest` already does.
 */
@RunWith(AndroidJUnit4::class)
class TaskCompletionAcrossScreensTest {

    private val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val compose = createAndroidComposeRule<MainActivity>()

    // The notification permission has to be granted before the activity launches, or its prompt
    // covers the screen and swallows the taps below.
    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(permission).around(compose)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container get() = (context.applicationContext as TimetyApplication).container

    private val task = TaskEntity(
        id = "uiflow-task-1",
        title = "Renew the passport",
        createdAt = Instant.now(),
    )

    @Before
    fun seedRealDatabase() {
        runBlocking {
            container.taskRepository.clearAll()
            container.userRepository.insertUserProfile(
                UserProfileEntity(name = "Tester", accountCreated = Instant.now(), totalXp = 0),
            )
            container.taskRepository.insertTask(task)
        }
    }

    @After
    fun clearRealDatabase() {
        runBlocking { container.taskRepository.clearAll() }
    }

    @Test
    fun checkingOffATask_awardsXp_promptsToLogFocus_andTheProfileStillRenders() {
        compose.onNodeWithText(context.getString(R.string.navigationTasks)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(task.title).assertIsDisplayed()

        compose.onNodeWithTag(taskCheckboxTag(task.id)).performClick()

        assertTrue(
            "tapping the checkbox should complete the task",
            awaitTrue { runBlocking { container.taskRepository.getTaskById(task.id) }?.isCompleted == true },
        )
        assertTrue(
            "completing a task from the list should award task XP",
            awaitTrue {
                runBlocking { container.userRepository.userProfile.first() }?.totalXp ==
                    ExperienceEngine.XP_PER_TASK
            },
        )

        // The "ask to log focus time" setting is on by default, so checking a task off offers to
        // attach a focus session to it. This prompt exists only inside the toggle composable.
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.taskFocusLogTitle)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.commonLabelCancel)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(context.getString(R.string.navigationProfile)).performClick()
        compose.waitForIdle()

        // Asserted on the seeded profile name rather than a stat card: the stat cards are below
        // the fold and not composed yet, and the screen's own title is the literal string
        // "Profile", which also labels the tab that navigated here.
        compose.onNodeWithText("Tester").assertIsDisplayed()
        assertEquals(
            "the completion should still be the only one after navigating away and back",
            1,
            runBlocking { container.taskRepository.allTasks.first() }.count { it.task.isCompleted },
        )
    }
}
