package io.github.benji377.timety.util.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import kotlin.math.floor
import kotlin.math.sqrt


/**
 * Converts completed tasks, habits, and focus time into XP, and XP into a level, title, and
 * badge color for the stats screen.
 *
 * Levels use a quadratic curve (`xpForLevel(n) = 100 * (n - 1)^2`) so each level requires
 * progressively more XP than the last, keeping early levels quick and later ones a longer grind.
 * [calculateLevel] is the algebraic inverse of that curve.
 */
object ExperienceEngine {
    /** XP granted for completing a task. */
    const val XP_PER_TASK = 15

    /** XP granted for completing a habit occurrence. */
    const val XP_PER_HABIT = 10

    /** XP granted per minute of completed focus-timer time. */
    const val XP_PER_FOCUS_MINS = 1


    /** Derives the current level from total XP by inverting the [getXpForLevel] curve. */
    fun calculateLevel(totalXp: Int): Int {
        return floor(sqrt(totalXp / 100.0)).toInt() + 1
    }


    /** Total XP required to reach [level]; the quadratic curve levels are based on. */
    fun getXpForLevel(level: Int): Int {
        val base = (level - 1)
        return 100 * base * base
    }


    /** Player-facing title for a level, escalating from "Novice Planner" to "Time God". */
    fun getTitle(level: Int): String {
        return when {
            level < 5 -> "Novice Planner"
            level < 10 -> "Focus Apprentice"
            level < 20 -> "Deep Work Adept"
            level < 35 -> "Time Master"
            level < 50 -> "Productivity Lord"
            level < 100 -> "Timety Legend"
            else -> "Time God"
        }
    }


    /** Badge icon matching the title tier returned by [getTitle]. */
    fun getTitleIcon(level: Int): ImageVector {
        return when {
            level < 5 -> Icons.Outlined.EmojiEvents
            level < 10 -> Icons.Outlined.AutoAwesome
            level < 20 -> Icons.Filled.Bolt
            level < 35 -> Icons.Filled.LocalFireDepartment
            level < 50 -> Icons.Outlined.WorkspacePremium
            level < 100 -> Icons.Outlined.RocketLaunch
            else -> Icons.Rounded.Star
        }
    }


    /** Badge color matching the title tier returned by [getTitle]. */
    fun getTitleColor(level: Int): Color {
        return when {
            level < 5 -> WarningColor
            level < 10 -> Color(0xFF8E6CFF)
            level < 20 -> TaskColor
            level < 35 -> Color(0xFFFF7A45)
            level < 50 -> Color(0xFF4E9F3D)
            level < 100 -> Color(0xFFB23A48)
            else -> Color(0xFF1E88E5)
        }
    }


    /** Fraction (0.0-1.0) of progress through the current level, for a level-up progress bar. */
    fun getLevelProgress(totalXp: Int): Double {
        val currentLevel = calculateLevel(totalXp)
        val currentTierXp = getXpForLevel(currentLevel)
        val nextTierXp = getXpForLevel(currentLevel + 1)

        val xpIntoLevel = totalXp - currentTierXp
        val xpNeededForNext = nextTierXp - currentTierXp

        return xpIntoLevel.toDouble() / xpNeededForNext.toDouble()
    }
}
