package io.github.benji377.timety.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Task
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.theme.WarningColor

/** A destination in the bottom navigation bar, with its icons and section accent color. */
sealed class BottomNavItem(
    val route: String,
    @param:StringRes val titleRes: Int,
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
}

/** Items shown in the bottom navigation bar, in display order. */
val BottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Focus,
    BottomNavItem.Tasks,
    BottomNavItem.Habits,
    BottomNavItem.Profile
)

/** Routes of [BottomNavItems], for membership checks on every navigation. */
val BottomNavRoutes: Set<String> = BottomNavItems.mapTo(HashSet()) { it.route }
