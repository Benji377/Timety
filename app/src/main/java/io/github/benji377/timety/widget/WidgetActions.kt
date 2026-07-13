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
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.util.task.RecurrenceUtils
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

/**
 * Completes a plain task from the widget checkbox, mirroring
 * [TaskViewModel.markTaskCompleted][io.github.benji377.timety.ui.viewmodel.TaskViewModel]:
 * reminders re-synced, XP awarded, widget refreshed.
 */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val container = (context.applicationContext as TimetyApplication).container
        val task = container.taskRepository.getTaskById(taskId) ?: return
        if (task.isCompleted) return
        val updated = task.copy(isCompleted = true, completedAt = Instant.now())
        container.taskRepository.updateTask(updated)
        ReminderScheduler.create(context).scheduleTaskReminders(updated)
        container.userRepository.addXp(ExperienceEngine.XP_PER_TASK)
        TaskWidget().updateAll(context)
    }
}

/**
 * Completes a recurring task's current occurrence from the widget checkbox, mirroring
 * [RecurringTaskViewModel.completeOccurrence][io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel]:
 * occurrence logged, due date rolled forward, reminders re-synced, XP awarded.
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
        val now = Instant.now()
        container.recurringTaskRepository.insertOccurrence(
            RecurringOccurrenceEntity(recurringTaskId = task.id, completedAt = now)
        )
        val advanced = task.copy(dueDate = RecurrenceUtils.nextDueDate(task, now))
        container.recurringTaskRepository.updateTask(advanced)
        ReminderScheduler.create(context).scheduleRecurringTaskReminders(advanced)
        container.userRepository.addXp(ExperienceEngine.XP_PER_TASK)
        TaskWidget().updateAll(context)
    }
}

/**
 * Toggles today's completion of a habit from its widget row, mirroring
 * [HabitViewModel.toggleCompletionToday][io.github.benji377.timety.ui.viewmodel.HabitViewModel]
 * including the XP award/revert.
 */
class ToggleHabitCompletionAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val container = (context.applicationContext as TimetyApplication).container
        val habitRepository = container.habitRepository
        if (habitRepository.getHabitById(habitId) == null) return
        val today = LocalDate.now()
        val todayCompletion = habitRepository.getCompletionsForHabit(habitId).first()
            .find { it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == today }
        if (todayCompletion != null) {
            habitRepository.deleteCompletion(todayCompletion)
            container.userRepository.addXp(-ExperienceEngine.XP_PER_HABIT)
        } else {
            habitRepository.insertCompletion(
                HabitCompletionEntity(habitId = habitId, completionDate = Instant.now())
            )
            container.userRepository.addXp(ExperienceEngine.XP_PER_HABIT)
        }
        HabitWidget().updateAll(context)
    }
}
