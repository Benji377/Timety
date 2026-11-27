package io.github.benji377.timety.widget

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.services.ReminderScheduler

/**
 * Tap intent for a widget element: opens [MainActivity] and navigates to [route] (an AppRoute or
 * bottom-tab route string) via the one-shot [MainActivity.EXTRA_NAV_TARGET] extra.
 */
internal fun widgetNavIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        // Extras are ignored by Intent.filterEquals, so without a distinct data URI every row's
        // intent would collapse into the same PendingIntent and reuse one stale extra.
        data = "timety://navigate/$route".toUri()
        putExtra(MainActivity.EXTRA_NAV_TARGET, route)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

internal val TaskIdKey = ActionParameters.Key<String>("taskId")
internal val RecurringTaskIdKey = ActionParameters.Key<String>("recurringTaskId")
internal val HabitIdKey = ActionParameters.Key<String>("habitId")

/** Completes a plain task from the widget checkbox; see [CompletionActions.completeTask]. */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val container = (context.applicationContext as TimetyApplication).container
        CompletionActions.completeTask(
            context, container.taskRepository, taskId
        )
    }
}

/**
 * Completes a recurring task's current occurrence from the widget checkbox; see
 * [CompletionActions.completeRecurringOccurrence].
 */
class CompleteRecurringTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[RecurringTaskIdKey] ?: return
        val container = (context.applicationContext as TimetyApplication).container
        val task = container.recurringTaskRepository.getTaskById(taskId) ?: return
        CompletionActions.completeRecurringOccurrence(
            context,
            container.recurringTaskRepository,
            task,
            autoSkip = container.settingsRepository.recurringAutoSkipWindow(),
        )
    }
}

/**
 * One check-in tap on a habit from its widget row, the same as
 * [HabitViewModel.toggleCompletionToday][io.github.benji377.timety.ui.viewmodel.HabitViewModel].
 */
class ToggleHabitCompletionAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val container = (context.applicationContext as TimetyApplication).container
        container.habitRepository.tapCheckIn(habitId) ?: return
        // Done drops today's remaining reminders; undone brings them back.
        ReminderScheduler.refreshHabitReminder(context, habitId)
        HabitWidget().updateAll(context)
    }
}
