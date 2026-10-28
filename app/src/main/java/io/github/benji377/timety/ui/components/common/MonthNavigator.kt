package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.LocalDate

/** Month title between previous/next arrows; [nextEnabled] false stops at the shown month. */
@Composable
fun MonthNavigator(
    focusedMonth: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeoIconButton(
            onClick = onPrevious,
            icon = Icons.Filled.ChevronLeft,
            contentDescription = stringResource(R.string.commonLabelPrevious),
        )
        Text(
            text = AppDateFormatUtils.formatMonthYear(focusedMonth),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        NeoIconButton(
            onClick = onNext,
            icon = Icons.Filled.ChevronRight,
            contentDescription = stringResource(R.string.commonLabelNext),
            enabled = nextEnabled,
        )
    }
}
