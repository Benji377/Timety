package io.github.benji377.timety.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.TimetyApplication
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as TimetyApplication).container
        val taskRepository = appContainer.taskRepository

        val allTasksWithSubtasks = taskRepository.allTasks.first()
        val now = Instant.now()
        val endOfToday = now.atZone(ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.DAYS)
            .plusDays(1)
            .toInstant()

        val urgentTasks = allTasksWithSubtasks.map { it.task }.filter { task ->
            !task.isCompleted && task.dueDate != null && task.dueDate.isBefore(endOfToday)
        }.sortedBy { it.dueDate }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(GlanceTheme.colors.surface)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = context.getString(R.string.widgetTasksDue, urgentTasks.size),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    if (urgentTasks.isEmpty()) {
                        Text(
                            text = "No urgent tasks",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        urgentTasks.take(3).forEach { task ->
                            Text(
                                text = "• ${task.title}",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                                maxLines = 1
                            )
                        }
                        if (urgentTasks.size > 3) {
                            Text(
                                text = "+${urgentTasks.size - 3} more",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}
