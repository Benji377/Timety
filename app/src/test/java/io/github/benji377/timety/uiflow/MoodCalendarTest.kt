package io.github.benji377.timety.uiflow

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.util.datetime.AppDateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Rates today from the profile's prompt card, then removes the rating again from the calendar,
 * checking the stored rating and the card at each step.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class MoodCalendarTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container get() = (context.applicationContext as TimetyApplication).container

    private fun str(resId: Int): String = context.getString(resId)

    private fun todaysRating(): DayRating? = runBlocking {
        container.dayRatingRepository.get(AppDateUtils.dayKey(LocalDate.now()))
    }?.let { DayRating.fromValue(it.rating) }

    private fun cardShown(shown: Boolean) = compose.waitUntil(timeoutMillis = 5_000) {
        compose.onAllNodesWithText(str(R.string.moodRateTodayTitle)).fetchSemanticsNodes().isNotEmpty() == shown
    }

    @Before
    fun openProfile() {
        // A midnight checkup makes the prompt card show at any time of day.
        runBlocking { container.settingsRepository.saveEndOfDayCheckupTime("00:00") }
        dismissExactAlarmPrompt()
        compose.onNodeWithText(str(R.string.navigationProfile)).performClick()
        compose.waitForIdle()
    }

    @Test
    fun ratingToday_fromTheCard_thenRemovingIt_fromTheCalendar() {
        cardShown(true)
        compose.onNodeWithText(str(R.string.moodRateTodayTitle)).assertIsDisplayed()

        compose.onNodeWithText(str(R.string.dayRatingGreat)).performClick()
        assertTrue("tapping a face should store today's mood", awaitTrue { todaysRating() == DayRating.GREAT })
        cardShown(false)

        // Today's cell now reads as Great; tapping it offers to remove the rating.
        compose.onNodeWithContentDescription(": ${str(R.string.dayRatingGreat)}", substring = true)
            .performClick()
        compose.onNodeWithText(str(R.string.commonLabelRemove)).performClick()
        assertTrue("removing should delete today's mood", awaitTrue { todaysRating() == null })
        cardShown(true)
    }

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
