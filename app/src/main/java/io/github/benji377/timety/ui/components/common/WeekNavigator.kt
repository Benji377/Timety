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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate


/**
 * Header for stepping through Monday-to-Sunday weeks around [focusedDate]. The forward arrow is
 * disabled once the current week is reached, so navigation cannot go into the future.
 */
@Composable
fun WeekNavigator(
    focusedDate: LocalDate,
    onShiftWeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val startOfWeek = AppDateUtils.startOfWeekMonday(focusedDate)
    val endOfWeek = startOfWeek.plusDays(6)

    val today = LocalDate.now()
    val isCurrentWeek = AppDateUtils.isWithinInclusive(today, startOfWeek, endOfWeek)

    val dfs = LocalDateFormatSettings.current
    // Both ends in the user's date format; mixing styles ("Jul 13 - 7/19/26") reads as a typo.
    val weekRangeLabel =
        "${AppDateFormatUtils.formatDate(startOfWeek, dfs.dateFormatCode)} \u2013 " +
            AppDateFormatUtils.formatDate(endOfWeek, dfs.dateFormatCode)

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
