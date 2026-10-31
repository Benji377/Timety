package io.github.benji377.timety.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel

/** Task settings: the Home upcoming-task window. */
@Composable
internal fun TasksSettings(
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
) {
    val upcomingTasksHorizon by settingsViewModel.upcomingTasksHorizon.collectAsState()

    val upcomingTaskTitle = stringResource(R.string.settingsLabelUpcomingTask)
    val daysUnit = stringResource(R.string.settingsDialogUnitDays)
    var numberDialogSpec by remember { mutableStateOf<NumberDialogSpec?>(null) }

    numberDialogSpec?.let { spec ->
        NumberPickerDialog(spec = spec, onDismiss = { numberDialogSpec = null })
    }

    Spacer(modifier = Modifier.height(8.dp))
    NavSettingsItem(
        title = upcomingTaskTitle,
        subtitle = stringResource(R.string.settingsLabelUpcomingTaskSubtitle, upcomingTasksHorizon),
        icon = Icons.Outlined.Schedule,
        iconTint = TaskColor,
        onClick = {
            numberDialogSpec = NumberDialogSpec(
                title = upcomingTaskTitle,
                current = upcomingTasksHorizon, min = 1, max = 60, unit = daysUnit,
                onSave = { settingsViewModel.setUpcomingTasksHorizon(it) }
            )
        }
    )
}
