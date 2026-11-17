package io.github.benji377.timety.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextContrastTest {

    private val schemes = listOf(LightColorScheme, DarkColorScheme)
    private val colors = listOf(TaskColor, FocusColor, HabitColor, GoalColor, WarningColor, ErrorColor)

    @Test
    fun forText_readsOnEveryBackgroundOfBothThemes() {
        for (scheme in schemes) for (color in colors) {
            val text = color.forText(scheme)
            for (background in listOf(scheme.background, scheme.surface, scheme.surfaceContainerHighest)) {
                assertTrue(
                    "$color on $background gives ${contrastRatio(text, background)}",
                    contrastRatio(text, background) >= 4.5f,
                )
            }
        }
    }

    @Test
    fun forText_leavesAlreadyReadableColorsAlone() {
        // Habit purple already reads on the light backgrounds, so it must not shift at all.
        assertEquals(HabitColor, HabitColor.forText(LightColorScheme))
    }

    @Test
    fun forText_keepsUnspecified() {
        assertEquals(Color.Unspecified, Color.Unspecified.forText(LightColorScheme))
    }
}
