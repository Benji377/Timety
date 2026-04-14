package com.github.benji377.timety.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Focus : Screen("focus", "Focus", Icons.Default.PlayArrow)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.DateRange)
    object Stats : Screen("stats", "Stats", Icons.Default.Star)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object TaskDetail : Screen("task/{taskId}", "Task Detail") {
        fun createRoute(taskId: Int) = "task/$taskId"
    }
}
