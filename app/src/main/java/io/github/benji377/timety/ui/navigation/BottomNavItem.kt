package io.github.benji377.timety.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val activeColor: Color
) {
    object Home : BottomNavItem(
        "home",
        "Home",
        Icons.Outlined.Home,
        Icons.Filled.Home,
        WarningColor
    )

    object Focus : BottomNavItem(
        "focus",
        "Focus",
        Icons.Outlined.Coffee,
        Icons.Filled.Coffee,
        FocusColor
    )

    object Tasks : BottomNavItem(
        "tasks",
        "Tasks",
        Icons.Outlined.Task,
        Icons.Filled.Task,
        TaskColor
    )

    object Habits : BottomNavItem(
        "habits",
        "Habits",
        Icons.Outlined.Alarm,
        Icons.Filled.Alarm,
        HabitColor
    )

    object Profile : BottomNavItem(
        "profile",
        "Profile",
        Icons.Outlined.Person,
        Icons.Filled.Person,
        UserColor
    )
}

val BottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Focus,
    BottomNavItem.Tasks,
    BottomNavItem.Habits,
    BottomNavItem.Profile
)
