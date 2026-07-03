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

/** Engine for calculating user experience points (XP), levels, and titles. Mirrors xp_calculator.dart. */
object ExperienceEngine {
    const val xpPerTask = 15
    const val xpPerHabit = 10
    const val xpPerFocusMin = 1

    /** Calculates the user level based on total XP. */
    fun calculateLevel(totalXp: Int): Int {
        return floor(sqrt(totalXp / 100.0)).toInt() + 1
    }

    /** Calculates the required XP to reach a specific level. */
    fun getXpForLevel(level: Int): Int {
        val base = (level - 1)
        return 100 * base * base
    }

    /** Generates a descriptive title based on the user level. */
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

    /** Returns the corresponding icon for the user level. */
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

    /** Returns the corresponding color theme for the user level. */
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

    /** Returns progress to the NEXT level as a fraction (0.0 to 1.0). */
    fun getLevelProgress(totalXp: Int): Double {
        val currentLevel = calculateLevel(totalXp)
        val currentTierXp = getXpForLevel(currentLevel)
        val nextTierXp = getXpForLevel(currentLevel + 1)

        val xpIntoLevel = totalXp - currentTierXp
        val xpNeededForNext = nextTierXp - currentTierXp

        return xpIntoLevel.toDouble() / xpNeededForNext.toDouble()
    }
}
