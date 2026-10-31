package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.MonthNavigator
import io.github.benji377.timety.ui.components.common.NeoButton
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.CalendarUtils
import io.github.benji377.timety.util.stats.FocusHistoryDatePreset
import java.time.LocalDate

private val WeekdayHeaders = listOf(
    R.string.calendarHeaderMon,
    R.string.calendarHeaderTue,
    R.string.calendarHeaderWed,
    R.string.calendarHeaderThu,
    R.string.calendarHeaderFri,
    R.string.calendarHeaderSat,
    R.string.calendarHeaderSun,
)

/** One choice in a [HistoryOptionSheet]. */
internal data class HistoryOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** A list of [options]; [multiSelect] shows checkboxes instead of radio buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryOptionSheet(
    title: String,
    options: List<HistoryOption>,
    onDismiss: () -> Unit,
    multiSelect: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppTheme.spaceLarge,
                    end = AppTheme.spaceLarge,
                    bottom = AppTheme.spaceXLarge,
                ),
        ) {
            SheetTitle(title)
            options.forEach { option ->
                OptionRow(option, multiSelect)
            }
        }
    }
}

/**
 * Picks the history's dates: a quick range applies right away, while the calendar selects one day
 * or, with a second tap, a range. Underlined days have sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryDateSheet(
    current: ClosedRange<LocalDate>?,
    today: LocalDate,
    daysWithSessions: Set<LocalDate>,
    onSelect: (ClosedRange<LocalDate>?) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentPreset = FocusHistoryDatePreset.entries.find { it.range(today) == current }
    val customRange = current.takeIf { currentPreset == null }
    var month by remember { mutableStateOf((customRange?.start ?: today).withDayOfMonth(1)) }
    var rangeStart by remember { mutableStateOf(customRange?.start) }
    var rangeEnd by remember { mutableStateOf(customRange?.endInclusive) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppTheme.spaceLarge,
                    end = AppTheme.spaceLarge,
                    bottom = AppTheme.spaceXLarge,
                ),
        ) {
            SheetTitle(stringResource(R.string.focusHistoryChipDate))
            FocusHistoryDatePreset.entries.forEach { preset ->
                OptionRow(
                    HistoryOption(
                        label = stringResource(preset.labelRes),
                        selected = preset == currentPreset && rangeStart == null,
                        onClick = { onSelect(preset.range(today)) },
                    ),
                    multiSelect = false,
                )
            }
            Spacer(Modifier.height(AppTheme.spaceMedium))
            MonthNavigator(
                focusedMonth = month,
                onPrevious = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                nextEnabled = month.isBefore(today.withDayOfMonth(1)),
            )
            Spacer(Modifier.height(AppTheme.spaceSmall))
            HistoryMonthGrid(
                month = month,
                today = today,
                daysWithSessions = daysWithSessions,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                onDayClick = { day ->
                    val start = rangeStart
                    when {
                        start == null || rangeEnd != null -> {
                            rangeStart = day
                            rangeEnd = null
                        }

                        day.isBefore(start) -> {
                            rangeStart = day
                            rangeEnd = start
                        }

                        else -> rangeEnd = day
                    }
                },
            )
            Spacer(Modifier.height(AppTheme.spaceSmall))
            Text(
                text = stringResource(R.string.focusHistoryDateHint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppTheme.spaceMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeoTextButton(
                    onClick = { onSelect(null) },
                    text = stringResource(R.string.focusHistoryDateClear),
                )
                Spacer(Modifier.padding(horizontal = AppTheme.spaceXSmall))
                NeoButton(
                    onClick = {
                        rangeStart?.let { start -> onSelect(start..(rangeEnd ?: start)) }
                    },
                    enabled = rangeStart != null,
                ) {
                    Text(stringResource(R.string.focusHistoryDateApply))
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = AppTheme.spaceSmall),
    )
}

@Composable
private fun OptionRow(option: HistoryOption, multiSelect: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = option.selected,
                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
                onClick = option.onClick,
            )
            .padding(vertical = AppTheme.spaceXSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiSelect) {
            Checkbox(checked = option.selected, onCheckedChange = null, modifier = Modifier.padding(AppTheme.spaceMedium))
        } else {
            RadioButton(selected = option.selected, onClick = null, modifier = Modifier.padding(AppTheme.spaceMedium))
        }
        Text(option.label)
    }
}

@Composable
private fun HistoryMonthGrid(
    month: LocalDate,
    today: LocalDate,
    daysWithSessions: Set<LocalDate>,
    rangeStart: LocalDate?,
    rangeEnd: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
) {
    val weeks = remember(month) { CalendarUtils.generateWeeks(month) }
    Row(Modifier.fillMaxWidth()) {
        WeekdayHeaders.forEach { header ->
            Text(
                text = stringResource(header),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    weeks.forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { day ->
                val cell = Modifier.weight(1f)
                if (day.month != month.month) {
                    Spacer(cell)
                } else {
                    val isEdge = day == rangeStart || day == rangeEnd
                    val inRange = rangeStart != null && rangeEnd != null && day in rangeStart..rangeEnd
                    HistoryDayCell(
                        day = day,
                        hasSessions = day in daysWithSessions,
                        isEdge = isEdge,
                        inRange = inRange,
                        isFuture = day.isAfter(today),
                        onClick = { onDayClick(day) },
                        modifier = cell,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCell(
    day: LocalDate,
    hasSessions: Boolean,
    isEdge: Boolean,
    inRange: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dfs = LocalDateFormatSettings.current
    val background = when {
        isEdge -> FocusColor
        inRange -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val textColor = when {
        isEdge -> Color.White
        isFuture -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = AppDateFormatUtils.formatDate(day, dfs.dateFormatCode)
    Box(
        modifier = modifier
            .height(44.dp)
            .padding(1.dp)
            .clip(AppTheme.brMedium)
            .background(background)
            .clickable(
                enabled = !isFuture,
                onClickLabel = description,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (hasSessions) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (hasSessions) TextDecoration.Underline else null,
        )
    }
}
