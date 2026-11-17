package io.github.benji377.timety.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.ui.navigation.AppRoute
import io.github.benji377.timety.ui.navigation.BottomNavItem
import io.github.benji377.timety.ui.components.task.recurringStatusColor
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.task.Due
import io.github.benji377.timety.util.task.due
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One row of the task widget: a plain or recurring task with its tap route and check action. */
private data class TaskWidgetRow(
    val id: String,
    val isRecurring: Boolean,
    val title: String,
    val priorityColor: Color,
    // Same rule as the app's task tiles: red overdue, orange due today, blue otherwise.
    val statusColor: Color,
    val dueLabel: String,
    val isOverdue: Boolean,
)

/**
 * Home-screen widget mirroring the Home screen's task lists: everything due today or earlier,
 * plus tasks (including recurring ones) inside the upcoming-task-window setting. Rows complete
 * directly via their checkbox and deep-link into the matching detail screen on tap.
 */
class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as TimetyApplication).container
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val now = Instant.now()
        // The app's own time-format setting, not the device's, so the widget matches the app.
        val use24Hour = appContainer.settingsRepository.use24HourFormatFlow.first()
        val horizonDays = appContainer.settingsRepository.upcomingTasksHorizonFlow.first()

        // Exclusive cutoff after the horizon window; one query covers overdue + due + upcoming.
        val windowEnd = today.plusDays(horizonDays.toLong() + 1).atStartOfDay(zone).toInstant()
        val openTasks = appContainer.taskRepository.getOpenTasksDueBefore(windowEnd)
        val (duePlain, upcomingPlain) = openTasks.partition { task ->
            val dueDay = task.dueDate?.atZone(zone)?.toLocalDate()
            dueDay != null && !dueDay.isAfter(today)
        }

        val recurringByStatus = appContainer.recurringTaskRepository.getAllTasks()
            .groupBy { RecurrenceUtils.statusOf(it, now, horizonDays) }
        val dueRecurring = recurringByStatus[RecurringStatus.OVERDUE].orEmpty() +
                recurringByStatus[RecurringStatus.DUE_TODAY].orEmpty()
        val upcomingRecurring = recurringByStatus[RecurringStatus.UPCOMING].orEmpty()

        // this.id: the enclosing provideGlance's GlanceId parameter shadows the entity property.
        fun TaskEntity.toRow(): TaskWidgetRow {
            val isOverdue = due?.isOverdue(now, zone) == true
            return TaskWidgetRow(
                id = this.id,
                isRecurring = false,
                title = title,
                priorityColor = priorityColor(priority),
                statusColor = when {
                    isOverdue -> ErrorColor
                    due?.date(zone) == today -> WarningColor
                    else -> TaskColor
                },
                dueLabel = dueLabel(due, today, zone, use24Hour),
                isOverdue = isOverdue,
            )
        }

        fun RecurringTaskEntity.toRow(): TaskWidgetRow {
            val status = RecurrenceUtils.statusOf(this, now, horizonDays)
            return TaskWidgetRow(
                id = this.id,
                isRecurring = true,
                title = title,
                priorityColor = TaskColor,
                statusColor = recurringStatusColor(status),
                dueLabel = dueLabel(due, today, zone, use24Hour),
                isOverdue = status == RecurringStatus.OVERDUE,
            )
        }

        // Each section sorts by the actual due instant, like the Home screen's lists.
        val dueSorted = (duePlain.map { it.dueDate to it.toRow() } +
                dueRecurring.map { it.dueDate as Instant? to it.toRow() })
            .sortedBy { it.first }
            .map { it.second }
        val upcomingSorted = (upcomingPlain.map { it.dueDate to it.toRow() } +
                upcomingRecurring.map { it.dueDate as Instant? to it.toRow() })
            .sortedBy { it.first }
            .map { it.second }

        provideContent {
            GlanceTheme(colors = TimetyWidgetColors) { WidgetContent(context, dueSorted, upcomingSorted) }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, due: List<TaskWidgetRow>, upcoming: List<TaskWidgetRow>) {
        val openTasksTab = actionStartActivity(
            widgetNavIntent(context, BottomNavItem.Tasks.route)
        )
        OutlinedBox(
            border = GlanceTheme.colors.outline,
            fill = GlanceTheme.colors.background,
            modifier = GlanceModifier.fillMaxSize(),
            corners = WidgetCorners.CARD,
            innerModifier = GlanceModifier.fillMaxSize(),
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable(openTasksTab),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clickable(openTasksTab)
                ) {
                    Text(
                        text = context.getString(R.string.widgetTasksDue, due.size),
                        style = TextStyle(
                            color = widgetTextColor(WarningColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                    )
                }

                if (due.isEmpty() && upcoming.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.widgetNoUrgentTasks),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        itemsIndexed(due) { _, row -> TaskRowItem(context, row) }
                        if (upcoming.isNotEmpty()) {
                            item {
                                Text(
                                    text = "${context.getString(R.string.commonTimeUpcoming)} (${upcoming.size})",
                                    style = TextStyle(
                                        color = widgetTextColor(TaskColor),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = GlanceModifier.padding(
                                        top = 4.dp, bottom = 6.dp, start = 2.dp
                                    )
                                )
                            }
                            itemsIndexed(upcoming) { _, row -> TaskRowItem(context, row) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskRowItem(context: Context, row: TaskWidgetRow) {
        val detailRoute = if (row.isRecurring) {
            AppRoute.recurringTaskDetail(row.id)
        } else {
            AppRoute.taskDetail(row.id)
        }
        val completeAction = if (row.isRecurring) {
            actionRunCallback<CompleteRecurringTaskAction>(
                actionParametersOf(RecurringTaskIdKey to row.id)
            )
        } else {
            actionRunCallback<CompleteTaskAction>(actionParametersOf(TaskIdKey to row.id))
        }

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            OutlinedBox(
                border = ColorProvider(row.statusColor),
                fill = GlanceTheme.colors.surface,
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    CheckBox(
                        checked = false,
                        onCheckedChange = completeAction,
                        colors = CheckboxDefaults.colors(
                            checkedColor = ColorProvider(TaskColor),
                            uncheckedColor = ColorProvider(TaskColor),
                        ),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(vertical = 6.dp)
                            .clickable(actionStartActivity(widgetNavIntent(context, detailRoute))),
                    ) {
                        Text(
                            text = if (row.isRecurring) "↻" else "●",
                            style = TextStyle(
                                color = ColorProvider(row.priorityColor),
                                fontSize = if (row.isRecurring) 12.sp else 10.sp
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = row.title,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (row.dueLabel.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = row.dueLabel,
                                style = TextStyle(
                                    color = if (row.isOverdue) {
                                        widgetTextColor(ErrorColor)
                                    } else {
                                        GlanceTheme.colors.onSurfaceVariant
                                    },
                                    fontSize = 12.sp
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
        }
    }

    /**
     * Time of day for tasks due today, a short date otherwise; empty when there is no due date or
     * it is due all day today.
     */
    private fun dueLabel(
        due: Due?,
        today: LocalDate,
        zone: ZoneId,
        use24Hour: Boolean
    ): String {
        if (due == null) return ""
        val dueDay = due.date(zone)
        return when {
            dueDay != today -> AppDateFormatUtils.formatShortDate(dueDay)
            due.hasTime -> AppDateFormatUtils.formatTime(due.at, use24Hour)
            else -> ""
        }
    }

    private fun priorityColor(priority: Priority) = when (priority) {
        Priority.LOW -> TaskColor
        Priority.MEDIUM -> WarningColor
        Priority.HIGH, Priority.VERY_HIGH -> ErrorColor
    }
}
