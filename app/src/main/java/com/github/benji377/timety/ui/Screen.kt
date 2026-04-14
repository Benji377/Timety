package com.github.benji377.timety.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Coffee

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Focus : Screen("focus", "Focus", Icons.Default.Coffee)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.DateRange)
    object Stats : Screen("stats", "Stats", Icons.Default.PieChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddTask : Screen("addTask", "Add Task")
    object TaskDetail : Screen("task/{taskId}", "Task Detail") {
        fun createRoute(taskId: Int) = "task/$taskId"
    }
}
