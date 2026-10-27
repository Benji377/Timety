package io.github.benji377.timety.ui.components.mood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.LocalDate

/** Rates [day], or removes its rating when it has one. Picking a mood also closes the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodPickerSheet(
    day: LocalDate,
    current: DayRating?,
    onPick: (DayRating) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dfs = LocalDateFormatSettings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTheme.spaceLarge,
                    end = AppTheme.spaceLarge,
                    bottom = AppTheme.spaceXLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (day == LocalDate.now()) {
                    stringResource(R.string.moodRateTodayTitle)
                } else {
                    stringResource(R.string.moodPickerTitle, AppDateFormatUtils.formatDate(day, dfs.dateFormatCode))
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            MoodChoiceRow(selected = current, onPick = onPick)
            if (current != null) {
                NeoTextButton(
                    onClick = onClear,
                    text = stringResource(R.string.commonLabelRemove),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
