package io.github.benji377.timety.ui.components.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.task.RecurringStatus

/** Border color that mirrors [TaskListTile]'s overdue/today/default scheme for a recurring status. */
fun recurringStatusColor(status: RecurringStatus): Color = when (status) {
    RecurringStatus.OVERDUE -> ErrorColor
    RecurringStatus.DUE_TODAY -> WarningColor
    RecurringStatus.UPCOMING, RecurringStatus.SCHEDULED -> TaskColor
}

/**
 * [TaskListTile]-style card for a recurring task appearing among normal tasks: checkbox that logs
 * an occurrence, title, cadence, and due date, with a trailing repeat icon marking it as recurring.
 * The border color reflects [status].
 */
@Composable
fun RecurringTaskListTile(
    task: RecurringTaskEntity,
    status: RecurringStatus,
    onComplete: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    margin: PaddingValues = PaddingValues(
        horizontal = AppTheme.spaceLarge,
        vertical = AppTheme.spaceXSmall
    ),
) {
    val dateFmt = LocalDateFormatSettings.current
    val borderColor = recurringStatusColor(status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(margin)
            .clickable { onTap() },
        shape = AppTheme.brMedium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppTheme.listTileBorderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SuccessColor,
                    uncheckedColor = TaskColor,
                    checkmarkColor = Color.White,
                ),
            )
            Spacer(Modifier.width(AppTheme.spaceSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = AppTheme.fwBold),
                )
                Text(
                    text = recurrenceCadenceLabel(task),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = AppTheme.fsLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = borderColor,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = AppDateFormatUtils.formatDateTime(
                            task.dueDate, dateFmt.dateFormatCode, dateFmt.use24HourFormat
                        ),
                        fontSize = AppTheme.fsLabel,
                        color = borderColor,
                        fontWeight = AppTheme.fwMedium,
                    )
                }
            }
            Spacer(Modifier.width(AppTheme.spaceLarge))
            Icon(
                imageVector = Icons.Filled.Repeat,
                contentDescription = null,
                tint = TaskColor,
            )
        }
    }
}
