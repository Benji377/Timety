package io.github.benji377.timety.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R

/** Settings landing page: one row per [SettingsPage], each opening that page. */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
) {
    SettingsPageScaffold(
        title = stringResource(R.string.settingsTitle),
        onNavigateBack = onNavigateBack,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsPage.entries.forEach { page ->
            NavSettingsItem(
                title = stringResource(page.titleRes),
                subtitle = stringResource(page.summaryRes),
                icon = page.icon,
                iconTint = page.iconTint,
                onClick = { onOpenPage(page) },
            )
        }
    }
}

/** Renders one settings subpage inside the shared page frame. */
@Composable
fun SettingsPageScreen(
    page: SettingsPage,
    onNavigateBack: () -> Unit,
    onNavigateToTags: () -> Unit,
) {
    SettingsPageScaffold(
        title = stringResource(page.titleRes),
        onNavigateBack = onNavigateBack,
    ) {
        when (page) {
            SettingsPage.GENERAL -> GeneralSettings(onNavigateToTags = onNavigateToTags)
            SettingsPage.FOCUS -> FocusSettings()
            SettingsPage.TASKS -> TasksSettings()
            SettingsPage.NOTIFICATIONS -> NotificationSettings()
            SettingsPage.DATA_BACKUP -> DataBackupSettings()
            SettingsPage.ADVANCED -> AdvancedSettings()
            SettingsPage.ABOUT -> AboutSettings()
        }
    }
}
