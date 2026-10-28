package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.ui.components.common.NeoFilterChip
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.LocalDate

@Composable
internal fun TagFilterRow(
    sessions: List<FocusSessionEntity>,
    tags: List<FocusTagEntity>,
    selectedTagId: String?,
    onSelect: (String?) -> Unit,
) {
    val tagById = remember(tags) { tags.associateBy { it.id } }
    val counts = remember(sessions, tags) {
        val map = LinkedHashMap<String, Int>()
        sessions.forEach { s ->
            val id = s.tagId; if (id != null && tagById.containsKey(id)) map[id] =
            (map[id] ?: 0) + 1
        }
        map
    }
    if (counts.isEmpty()) return

    val usedTags = remember(counts, tags) {
        counts.keys.mapNotNull { tagById[it] }.sortedBy { it.name }
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)
    ) {
        NeoFilterChip(
            selected = selectedTagId == null,
            onClick = { onSelect(null) },
            label = stringResource(R.string.focusTagsLabelAll),
            selectedColor = FocusColor,
        )
        usedTags.forEach { tag ->
            NeoFilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelect(if (selectedTagId == tag.id) null else tag.id) },
                label = "${tag.name} (${counts[tag.id] ?: 0})",
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(tag.colorValue))
                    )
                },
                selectedColor = Color(tag.colorValue),
            )
        }
    }
}

@Composable
internal fun DayPillSelector(
    startOfWeek: LocalDate,
    selectedDay: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)
    ) {
        for (i in 0 until 7) {
            val day = startOfWeek.plusDays(i.toLong())
            val isSelected = day == selectedDay
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) FocusColor else Color.Transparent)
                    .then(
                        Modifier.background(
                            color = Color.Transparent,
                        ),
                    )
                    .padding(horizontal = AppTheme.spaceMedium, vertical = AppTheme.spaceSmall)
                    .then(Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        Modifier.background(if (isSelected) FocusColor else Color.Transparent)
                    )
                    .then(
                        Modifier.clickable(
                            onClick = { onSelect(day) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() })
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = AppDateFormatUtils.formatWeekdayDay(
                        day
                    ),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
