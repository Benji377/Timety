package io.github.benji377.timety.ui.components.task

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.util.habit.HabitUtils

/** The cadence name shown on the recurrence unit selector. */
@Composable
fun recurrenceUnitName(unit: RecurrenceUnit): String = when (unit) {
    RecurrenceUnit.WEEK -> stringResource(R.string.recurrenceWeekly)
    RecurrenceUnit.MONTH -> stringResource(R.string.recurrenceMonthly)
    RecurrenceUnit.YEAR -> stringResource(R.string.recurrenceYearly)
}

/** Short localized weekday name ("Mon".."Sun") for an ISO weekday number (1 = Monday). */
@Composable
fun weekdayShortName(isoWeekday: Int): String = when (isoWeekday) {
    1 -> stringResource(R.string.commonWeekdayMon)
    2 -> stringResource(R.string.commonWeekdayTue)
    3 -> stringResource(R.string.commonWeekdayWed)
    4 -> stringResource(R.string.commonWeekdayThu)
    5 -> stringResource(R.string.commonWeekdayFri)
    6 -> stringResource(R.string.commonWeekdaySat)
    else -> stringResource(R.string.commonWeekdaySun)
}

/** Ordinal word for a monthly nth-weekday rule: "first".."fourth", or "last". */
@Composable
fun recurrenceOrdinalName(ordinal: Int): String = when (ordinal) {
    1 -> stringResource(R.string.recurrenceOrdinalFirst)
    2 -> stringResource(R.string.recurrenceOrdinalSecond)
    3 -> stringResource(R.string.recurrenceOrdinalThird)
    4 -> stringResource(R.string.recurrenceOrdinalFourth)
    else -> stringResource(R.string.recurrenceOrdinalLast)
}

/**
 * A human cadence label for cards and the recurring list, e.g. "Every 2 weeks on Mon, Fri",
 * "Every month on day 3", or "Every 3 months on the second Fri".
 */
@Composable
fun recurrenceCadenceLabel(task: RecurringTaskEntity): String {
    val n = task.interval.coerceAtLeast(1)
    val plural = when (task.unit) {
        RecurrenceUnit.WEEK -> R.plurals.recurrenceEveryWeeks
        RecurrenceUnit.MONTH -> R.plurals.recurrenceEveryMonths
        RecurrenceUnit.YEAR -> R.plurals.recurrenceEveryYears
    }
    val base = quantityString(plural, n, 0, n)
    return when (task.unit) {
        RecurrenceUnit.WEEK -> {
            val days = HabitUtils.parseWeekdays(task.daysOfWeek).sorted()
            if (days.isEmpty()) base
            else stringResource(
                R.string.recurrenceOnDays,
                base,
                // map is inline, so the composable string lookup is allowed; joinToString isn't.
                days.map { weekdayShortName(it) }.joinToString()
            )
        }

        RecurrenceUnit.MONTH -> when (task.monthlyMode) {
            MonthlyMode.DAY_OF_MONTH ->
                task.monthlyDay?.let { stringResource(R.string.recurrenceOnDayOfMonth, base, it) }
                    ?: base

            MonthlyMode.NTH_WEEKDAY -> {
                val ordinal = task.monthlyOrdinal
                val weekday = task.monthlyWeekday
                if (ordinal == null || weekday == null) base
                else stringResource(
                    R.string.recurrenceOnNthWeekday,
                    base,
                    recurrenceOrdinalName(ordinal),
                    weekdayShortName(weekday)
                )
            }
        }

        RecurrenceUnit.YEAR -> base
    }
}
