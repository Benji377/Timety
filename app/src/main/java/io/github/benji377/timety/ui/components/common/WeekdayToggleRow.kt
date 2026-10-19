package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.task.weekdayShortName
import io.github.benji377.timety.ui.theme.AppTheme


/**
 * Multi-select weekday picker (Monday first) styled as a [NeoSegmentedSelector] track: seven
 * equal single-letter segments, so it never wraps regardless of locale. Each selected day gets
 * the selector's inset fill; [selected] holds ISO weekday numbers (1 = Monday). Square-cornered
 * and slimmer than the selector, since single letters need less room than labels. Like the
 * selector, a disabled row keeps its selected fills and only mutes the track.
 */
@Composable
fun WeekdayToggleRow(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.neoIconButtonSize)
            .clip(AppTheme.brMedium)
            .background(
                if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(AppTheme.borderCard, borderColor, AppTheme.brMedium)
    ) {
        (1..7).forEach { day ->
            val isSelected = day in selected
            // Same rule as NeoSegmentedSelector: a divider only between two unselected days, since
            // a selected segment already has its own border.
            val drawsDivider = day < 7 && !isSelected && (day + 1) !in selected
            // The one-letter label is ambiguous (T/T, S/S), so screen readers get the longer name.
            val description = weekdayShortName(day)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let { m ->
                        if (drawsDivider) {
                            m.drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = AppTheme.borderHairline.toPx()
                                )
                            }
                        } else m
                    }
                    .toggleable(
                        value = isSelected,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(day) },
                    )
                    .semantics { contentDescription = description }
                    .padding(AppTheme.segmentedSelectorInset)
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(AppTheme.brMedium)
                                .background(selectedColor, AppTheme.brMedium)
                                .border(AppTheme.borderHairline, borderColor, AppTheme.brMedium)
                        } else Modifier
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekdayLetter(day),
                    fontWeight = if (isSelected) AppTheme.fwBold else AppTheme.fwMedium,
                    color = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = AppTheme.fsBodyMedium,
                )
            }
        }
    }
}


@Composable
private fun weekdayLetter(isoWeekday: Int): String = when (isoWeekday) {
    1 -> stringResource(R.string.calendarHeaderMon)
    2 -> stringResource(R.string.calendarHeaderTue)
    3 -> stringResource(R.string.calendarHeaderWed)
    4 -> stringResource(R.string.calendarHeaderThu)
    5 -> stringResource(R.string.calendarHeaderFri)
    6 -> stringResource(R.string.calendarHeaderSat)
    else -> stringResource(R.string.calendarHeaderSun)
}
