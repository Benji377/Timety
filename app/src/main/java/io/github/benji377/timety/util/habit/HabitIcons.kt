package io.github.benji377.timety.util.habit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.util.habit.HabitIcons.availableIcons
import io.github.benji377.timety.util.habit.HabitIcons.defaultIcon
import io.github.benji377.timety.util.habit.HabitIcons.iconAt
import androidx.compose.material.icons.outlined.Brush as BrushOutlined

/**
 * Curated list of icons suitable for habits. Mirrors `utils/habit/habit_icons.dart`
 * (`HabitIcons.availableIcons`) - same order and grouping/meaning as the Flutter list.
 *
 * IMPORTANT (repurposed field - see report): [io.github.benji377.timety.data.model.habit.HabitEntity.iconCodePoint]
 * originally mirrored Flutter's raw `IconData.codePoint`, which Compose cannot resolve back
 * into an [ImageVector]. Instead, the stored integer is now treated as an INDEX into
 * [availableIcons] (0-based, same order as below). Use [iconAt] to safely resolve a stored
 * value, and the index of an icon in [availableIcons] when persisting a selection.
 */
object HabitIcons {
    val availableIcons: List<ImageVector> = listOf(
        // Health & Fitness
        Icons.Filled.FitnessCenter,
        Icons.Filled.SportsGymnastics,
        Icons.Filled.DirectionsRun,
        Icons.Filled.SportsSoccer,
        Icons.Filled.SportsBasketball,
        Icons.Filled.WaterDrop,
        Icons.Filled.Favorite,

        // Mental & Wellness
        Icons.Filled.SelfImprovement,
        Icons.Filled.Spa,
        Icons.Filled.Psychology,
        Icons.Filled.SentimentSatisfied,
        Icons.Filled.Mood,
        Icons.Filled.FavoriteBorder,

        // Learning & Knowledge
        Icons.Filled.School,
        Icons.Filled.LibraryBooks,
        Icons.Filled.AutoStories,
        Icons.Filled.Notes,
        Icons.Filled.Edit,
        Icons.Filled.Lightbulb,
        Icons.Filled.Code,

        // Creativity & Hobbies
        Icons.Filled.Palette,
        Icons.Filled.MusicNote,
        Icons.Filled.Brush,
        Icons.Filled.PhotoCamera,
        Icons.Filled.Videocam,
        Icons.Outlined.BrushOutlined,

        // Organization & Productivity
        Icons.Filled.Checklist,
        Icons.Filled.PendingActions,
        Icons.Filled.CalendarToday,
        Icons.Filled.Schedule,
        Icons.Filled.CleaningServices,

        // Social & Family
        Icons.Filled.People,
        Icons.Filled.Group,
        Icons.Filled.Call,
        Icons.Filled.Chat,

        // Food & Nutrition
        Icons.Filled.Restaurant,
        Icons.Filled.LocalDining,
        Icons.Filled.LunchDining,
        Icons.Filled.LocalCafe,
    )

    /** Fallback icon shown when no icon has been chosen. Mirrors Flutter's `Icons.circle` fallback. */
    val defaultIcon: ImageVector = Icons.Filled.Circle

    /**
     * Resolves a stored index (see class doc) into an [ImageVector], guarding null/out-of-range
     * values with [defaultIcon] - the same fallback behavior as Flutter's `_getIconFromCodePoint`.
     */
    fun iconAt(index: Int?): ImageVector {
        if (index == null || index !in availableIcons.indices) return defaultIcon
        return availableIcons[index]
    }
}
