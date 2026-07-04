package io.github.benji377.timety.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val activeColor: Color
) {
    object Home : BottomNavItem(
        "home",
        R.string.navigationHome,
        Icons.Outlined.Home,
        Icons.Filled.Home,
        WarningColor
    )

    object Focus : BottomNavItem(
        "focus",
        R.string.navigationFocus,
        Icons.Outlined.Coffee,
        Icons.Filled.Coffee,
        FocusColor
    )

    object Tasks : BottomNavItem(
        "tasks",
        R.string.navigationTasks,
        Icons.Outlined.Task,
        Icons.Filled.Task,
        TaskColor
    )

    object Habits : BottomNavItem(
        "habits",
        R.string.navigationHabits,
        Icons.Outlined.Alarm,
        Icons.Filled.Alarm,
        HabitColor
    )

    object Profile : BottomNavItem(
        "profile",
        R.string.navigationProfile,
        Icons.Outlined.Person,
        Icons.Filled.Person,
        UserColor
    )

    object Calendar : BottomNavItem(
        "calendar",
        R.string.calendarTitle,
        Icons.Outlined.CalendarToday,
        Icons.Filled.CalendarToday,
        TaskColor
    )

    object Statistics : BottomNavItem(
        "statistics",
        R.string.statsTitle,
        Icons.Outlined.BarChart,
        Icons.Filled.BarChart,
        WarningColor
    )
}

val BottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Focus,
    BottomNavItem.Tasks,
    BottomNavItem.Habits,
    BottomNavItem.Profile
)
