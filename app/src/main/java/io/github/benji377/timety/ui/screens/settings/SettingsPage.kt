package io.github.benji377.timety.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.navigation.AppRoute
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningAccent

/**
 * A settings subpage. The landing list and the nav graph are both built from [entries], so a new
 * page needs only an entry here and a branch in [SettingsPageScreen].
 */
enum class SettingsPage(
    private val routeSegment: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int,
    val icon: ImageVector,
    val iconTint: Color,
) {
    GENERAL(
        "general",
        R.string.settingsSectionGeneral,
        R.string.settingsPageGeneralSummary,
        Icons.Outlined.Tune,
        TaskColor,
    ),
    FOCUS(
        "focus",
        R.string.settingsSectionFocus,
        R.string.settingsPageFocusSummary,
        Icons.Outlined.Timer,
        FocusColor,
    ),
    TASKS(
        "tasks",
        R.string.settingsSectionTasks,
        R.string.settingsPageTasksSummary,
        Icons.Outlined.TaskAlt,
        TaskColor,
    ),
    NOTIFICATIONS(
        "notifications",
        R.string.settingsSectionNotifications,
        R.string.settingsPageNotificationsSummary,
        Icons.Outlined.Notifications,
        WarningAccent,
    ),
    DATA_BACKUP(
        "data_backup",
        R.string.settingsSectionDataBackup,
        R.string.settingsPageDataBackupSummary,
        Icons.Outlined.SettingsBackupRestore,
        HabitColor,
    ),
    ADVANCED(
        "advanced",
        R.string.settingsSectionAdvanced,
        R.string.settingsPageAdvancedSummary,
        Icons.Outlined.Build,
        WarningAccent,
    ),
    ABOUT(
        "about",
        R.string.settingsSectionAbout,
        R.string.settingsPageAboutSummary,
        Icons.Outlined.Info,
        FocusColor,
    );

    /** Nav route of this page, nested under [AppRoute.SETTINGS]. */
    val route: String get() = "${AppRoute.SETTINGS.route}/$routeSegment"
}
