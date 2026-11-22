package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.benji377.timety.data.model.form.FormSkeleton
import io.github.benji377.timety.data.model.form.TaskInput
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskSize

/**
 * The task inputs switched on in Settings > Forms, for list tiles that show task attributes.
 * Defaults to the state of an update from before Settings > Forms, for previews and tests.
 */
val LocalTaskSkeleton = staticCompositionLocalOf { FormSkeleton.parse<TaskInput>(null) }

/**
 * Whether a task tile shows its priority and effort: when the input is on, or when the task has
 * other values than the default, the same rule as the editors. Otherwise a user who never sees
 * the input would find unexplained glyphs on every task.
 */
@Composable
@ReadOnlyComposable
fun showsPriorityEffort(priority: Priority, size: TaskSize): Boolean =
    LocalTaskSkeleton.current.shows(
        TaskInput.PRIORITY_EFFORT,
        hasValue = priority != Priority.MEDIUM || size != TaskSize.MEDIUM,
    )
