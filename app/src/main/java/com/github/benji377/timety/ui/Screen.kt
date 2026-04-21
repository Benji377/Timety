package com.github.benji377.timety.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Focus : Screen("focus", "Focus", Icons.Default.Coffee)
    object Tasks : Screen("tasks", "Tasks", Icons.AutoMirrored.Filled.List)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object User : Screen("user", "User", Icons.Default.Person)

    object Settings : Screen("settings", "Settings")
    object AddTask : Screen("addTask", "Add Task")
    object TaskDetail : Screen("task/{taskId}", "Task Detail") {
        fun createRoute(taskId: Int) = "task/$taskId"
    }

    object FocusModes : Screen("focus_modes", "Focus Modes")
    object AddFocusMode : Screen("add_focus_mode", "Add Focus Mode")
    object DailyStats : Screen("daily_stats/{date}", "Daily Stats") {
        fun createRoute(date: Long) = "daily_stats/$date"
    }
}
