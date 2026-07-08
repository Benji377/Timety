package io.github.benji377.timety.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit


/** Home-screen widget listing open tasks due today or earlier, sorted by priority then due date. */
class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as TimetyApplication).container
        val taskRepository = appContainer.taskRepository

        // Midnight at the start of tomorrow, i.e. the exclusive cutoff for "due today or earlier".
        val endOfToday = Instant.now().atZone(ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.DAYS)
            .plusDays(1)
            .toInstant()

        val urgentTasks = taskRepository.getOpenTasksDueBefore(endOfToday)
            .sortedWith(compareByDescending<TaskEntity> { it.priority.ordinal }.thenBy { it.dueDate })
        val use24Hour = DateFormat.is24HourFormat(context)

        provideContent {
            GlanceTheme {
                val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.widget_background))
                        .padding(12.dp)
                        .clickable(openApp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.widgetTasksDue, urgentTasks.size),
                            style = TextStyle(
                                color = ColorProvider(TaskColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                        )
                    }

                    if (urgentTasks.isEmpty()) {
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
                            itemsIndexed(urgentTasks) { _, task ->
                                Column(modifier = GlanceModifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .background(ImageProvider(R.drawable.widget_task_item_bg))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .clickable(openApp),
                                    ) {
                                        Text(
                                            text = "●",
                                            style = TextStyle(
                                                color = ColorProvider(priorityColor(task.priority)),
                                                fontSize = 10.sp
                                            ),
                                        )
                                        Spacer(modifier = GlanceModifier.width(8.dp))
                                        Text(
                                            text = task.title,
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )
                                        val due = task.dueDate
                                        if (due != null) {
                                            Spacer(modifier = GlanceModifier.width(8.dp))
                                            Text(
                                                text = AppDateFormatUtils.formatTime(
                                                    due,
                                                    use24Hour
                                                ),
                                                style = TextStyle(
                                                    color = GlanceTheme.colors.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                ),
                                            )
                                        }
                                    }
                                    Spacer(modifier = GlanceModifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun priorityColor(priority: Priority) = when (priority) {
        Priority.LOW -> TaskColor
        Priority.MEDIUM -> WarningColor
        Priority.HIGH, Priority.VERY_HIGH -> ErrorColor
    }
}
