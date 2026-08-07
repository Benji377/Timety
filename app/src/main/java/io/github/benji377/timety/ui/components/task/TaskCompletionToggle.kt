package io.github.benji377.timety.ui.components.task

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.ui.components.focus.SessionEditorDialog
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import kotlinx.coroutines.launch

/**
 * The completion toggle shared by every surface with a task checkbox: it flips the task through
 * [taskViewModel] and, when the task was just checked off and the "ask to log focus time" setting
 * is on, offers to attach a focus session covering the time that went into it.
 *
 * Only completing a task prompts; unchecking one never does.
 */
@Composable
fun rememberTaskCompletionToggle(
    taskViewModel: TaskViewModel,
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
): (TaskEntity) -> Unit {
    val askToLogFocus by settingsViewModel.askFocusOnTaskComplete.collectAsState()
    val modes by focusViewModel.allModes.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val loggedMessage = stringResource(R.string.taskFocusLogSnackbar)
    var pendingTask by remember { mutableStateOf<TaskEntity?>(null) }

    pendingTask?.let { task ->
        SessionEditorDialog(
            title = stringResource(R.string.taskFocusLogTitle),
            icon = Icons.Outlined.Timer,
            modes = modes,
            // Sessions logged this way are attributed to the task, so there is no tag to pick.
            tags = emptyList(),
            fixedTargetLabel = task.title,
            onDismiss = { pendingTask = null },
            onSave = { mode, start, end, _ ->
                focusViewModel.logSessionForTask(mode, start, end, task.id, task.title)
                pendingTask = null
                scope.launch { snackbarHostState.showSnackbar(loggedMessage) }
            },
        )
    }

    // The setting is read through a snapshot holder so the returned lambda stays stable while
    // still seeing the current value when the user toggles it mid-session.
    val promptEnabled = rememberUpdatedState(askToLogFocus)
    return remember(taskViewModel) {
        { task ->
            taskViewModel.toggleTaskCompletion(task)
            if (!task.isCompleted && promptEnabled.value) pendingTask = task
        }
    }
}
