package io.github.benji377.timety.uiflow

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.testutil.dismissExactAlarmPrompt
import io.github.benji377.timety.ui.screens.stats.StatsPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Walks the statistics: the overview opens every [StatsPage] and back returns to it, pages without
 * data say so, switching periods never moves the dates, and goals open their details.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class StatsNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container get() = (context.applicationContext as TimetyApplication).container

    private fun str(resId: Int): String = context.getString(resId)

    @Before
    fun openOverview() {
        compose.dismissExactAlarmPrompt()
        compose.onNodeWithContentDescription(str(R.string.commonTooltipStats)).performClick()
        compose.waitForIdle()
    }

    @Test
    fun withoutData_everyPageOpens_showsItsEmptyMessage_andBackReturnsToTheOverview() {
        val messages = mapOf(
            StatsPage.TIME to str(R.string.focusStatsEmpty),
            StatsPage.TASKS to str(R.string.taskStatsEmpty),
            StatsPage.HABITS to str(R.string.habitStatsLabelEmpty),
            StatsPage.GOALS to str(R.string.goalsEmpty),
            StatsPage.PATTERNS to context.getString(R.string.statsPatternsEmpty, 12),
        )
        StatsPage.entries.forEach { page ->
            openPage(page)
            compose.onNodeWithText(messages.getValue(page)).assertIsDisplayed()
            pressBack()
            compose.onNodeWithText(str(R.string.statsTitle)).assertIsDisplayed()
        }
    }

    @Test
    fun switchingPeriodsInALoop_staysOnThisYear_andNextStopsAtIt() {
        runBlocking {
            container.taskRepository.insertTask(
                TaskEntity(id = "t", title = "t", createdAt = Instant.now())
            )
        }
        val year = LocalDate.now().year.toString()
        openPage(StatsPage.TASKS)
        awaitText(str(R.string.statsPeriodYear))

        repeat(3) {
            compose.onNodeWithText(str(R.string.statsPeriodWeek)).performClick()
            compose.onNodeWithText(str(R.string.statsPeriodMonth)).performClick()
            compose.onNodeWithText(str(R.string.statsPeriodYear)).performClick()
            compose.waitForIdle()
            compose.onNodeWithText(year).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription(str(R.string.commonLabelNext)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(year).assertIsDisplayed()

        compose.onNodeWithContentDescription(str(R.string.commonLabelPrevious)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText((LocalDate.now().year - 1).toString()).assertIsDisplayed()
    }

    @Test
    fun aGoalRow_opensTheGoal_andBackReturnsToTheGoalsPage() {
        val now = Instant.now()
        runBlocking {
            container.goalRepository.insertGoal(
                GoalEntity(
                    id = "g",
                    name = "Read books",
                    colorValue = 0,
                    targetValue = 10,
                    unitLabel = "books",
                    targetDate = now.plus(30, ChronoUnit.DAYS),
                    createdAt = now,
                )
            )
        }
        openPage(StatsPage.GOALS)
        awaitText("Read books")

        compose.onNodeWithText("Read books").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.goalDetailTitleView)).assertIsDisplayed()

        pressBack()
        compose.onNodeWithText("Read books").assertIsDisplayed()
        assertTrue(
            "back should return to the Goals page, not the overview",
            compose.onAllNodesWithText(str(R.string.statsTitle)).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun openPage(page: StatsPage) {
        val title = str(page.titleRes)
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }

    private fun awaitText(text: String) = compose.waitUntil(timeoutMillis = 5_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
