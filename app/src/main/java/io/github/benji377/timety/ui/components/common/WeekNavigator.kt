package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Week navigation header. Mirrors `widgets/common/week_navigator.dart`: shows the
 * current week range and lets the caller shift by [onShiftWeek] days (-7/+7).
 */
@Composable
fun WeekNavigator(
    focusedDate: LocalDate,
    onShiftWeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val startOfWeek = AppDateUtils.startOfWeekMonday(focusedDate)
    val endOfWeek = startOfWeek.plusDays(6)

    val today = LocalDate.now()
    val isCurrentWeek = AppDateUtils.isWithinInclusive(today, startOfWeek, endOfWeek)

    val dfs = io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current
    val weekRangeLabel = "${io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatShortDate(startOfWeek)} - ${io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatDate(endOfWeek, dfs.dateFormatCode)}"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onShiftWeek(-7) }) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isCurrentWeek) stringResource(R.string.weekNavThisWeek) else stringResource(
                    R.string.weekNavPastWeek
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = weekRangeLabel,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        IconButton(onClick = { onShiftWeek(7) }, enabled = !isCurrentWeek) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (isCurrentWeek) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
