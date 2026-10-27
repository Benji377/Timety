package io.github.benji377.timety.ui.components.mood

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.theme.AppTheme

/** Asks for today's mood with one-tap faces. */
@Composable
fun RateTodayCard(
    onPick: (DayRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.moodRateTodayTitle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = AppTheme.spaceLarge, end = AppTheme.spaceLarge, top = AppTheme.spaceLarge),
        )
        Text(
            text = stringResource(R.string.moodRateTodayBody),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppTheme.spaceLarge),
        )
        Spacer(Modifier.height(AppTheme.spaceMedium))
        MoodChoiceRow(
            selected = null,
            onPick = onPick,
            modifier = Modifier.padding(start = AppTheme.spaceLarge, end = AppTheme.spaceLarge, bottom = AppTheme.spaceLarge),
        )
    }
}
