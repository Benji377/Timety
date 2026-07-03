package io.github.benji377.timety

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun generateScreenshots() {
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // 1. Home Screen
        takeScreenshot("01_home_screen")

        // 2. Stats
        try {
            composeTestRule.onNodeWithContentDescription("Statistics", ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
            takeScreenshot("07_stats_screen")
            composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeTestRule.waitForIdle()
        } catch (e: Exception) { println("Stats navigation failed") }

        // 3. Calendar
        try {
            composeTestRule.onNodeWithContentDescription("Calendar", ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
            takeScreenshot("08_calendar_screen")
            composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeTestRule.waitForIdle()
        } catch (e: Exception) { println("Calendar navigation failed") }

        // 4. Focus Screen
        composeTestRule.onNodeWithText("Focus", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("02_focus_screen")

        // 5. Focus Modes
        try {
            composeTestRule.onNodeWithContentDescription("Modes", ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
            takeScreenshot("06_focus_modes_screen")
            composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeTestRule.waitForIdle()
        } catch (e: Exception) { println("Focus Modes navigation failed") }

        // 6. Tasks
        composeTestRule.onNodeWithText("Tasks", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("03_tasks_screen")

        // 7. Habits
        composeTestRule.onNodeWithText("Habits", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("04_habits_screen")

        // 8. Profile
        composeTestRule.onNodeWithText("Profile", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        takeScreenshot("05_profile_screen")
    }

    private fun takeScreenshot(name: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        
        // Save to external storage (e.g. app's external files dir)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(dir, "$name.png")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        println("Screenshot saved to: ${file.absolutePath}")
    }
}
