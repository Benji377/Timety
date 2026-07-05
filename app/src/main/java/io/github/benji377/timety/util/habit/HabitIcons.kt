package io.github.benji377.timety.util.habit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material.icons.outlined.Brush as BrushOutlined


object HabitIcons {
    val availableIcons: List<ImageVector> = listOf(
        // Health & Fitness
        Icons.Filled.FitnessCenter,
        Icons.Filled.SportsGymnastics,
        Icons.AutoMirrored.Filled.DirectionsRun,
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
        Icons.AutoMirrored.Filled.LibraryBooks,
        Icons.Filled.AutoStories,
        Icons.AutoMirrored.Filled.Notes,
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
        Icons.AutoMirrored.Filled.Chat,

        // Food & Nutrition
        Icons.Filled.Restaurant,
        Icons.Filled.LocalDining,
        Icons.Filled.LunchDining,
        Icons.Filled.LocalCafe,
    )


    val defaultIcon: ImageVector = Icons.Filled.Circle


    fun iconAt(index: Int?): ImageVector {
        if (index == null || index !in availableIcons.indices) return defaultIcon
        return availableIcons[index]
    }
}
