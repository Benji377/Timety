package io.github.benji377.timety.ui.components.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.theme.AppTheme

/** The three moods as large tappable faces, best first; [selected] is outlined in its color. */
@Composable
fun MoodChoiceRow(
    selected: DayRating?,
    onPick: (DayRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
    ) {
        MoodsBestFirst.forEach { mood ->
            val isSelected = mood == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppTheme.brMedium)
                    .background(if (isSelected) mood.color.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = AppTheme.borderHairline,
                        color = if (isSelected) mood.color else MaterialTheme.colorScheme.outline,
                        shape = AppTheme.brMedium,
                    )
                    .clickable(role = Role.Button) { onPick(mood) }
                    .padding(vertical = AppTheme.spaceMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = mood.icon,
                    contentDescription = null,
                    tint = mood.color,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = stringResource(mood.labelRes),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = AppTheme.spaceXSmall),
                )
            }
        }
    }
}
