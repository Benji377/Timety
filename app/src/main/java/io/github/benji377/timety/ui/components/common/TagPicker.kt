package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel

/** The colors offered for a tag. */
private val TagColors = (listOf(TaskColor, FocusColor) + PickerPalette).distinct()

/** Small color swatch identifying a tag. */
@Composable
fun TagDot(colorValue: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color(colorValue))
    )
}

/**
 * Dialog for creating a tag, or editing [tag] when it isn't null. Names already used by another of
 * [allTags] are rejected, ignoring case.
 */
@Composable
fun TagEditDialog(
    tag: TagEntity?,
    allTags: List<TagEntity>,
    onConfirm: (name: String, colorValue: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(if (tag == null) R.string.focusTagsLabelAdd else R.string.focusTagsLabelEdit)
    NamedColorEditDialog(
        title = title,
        nameLabel = stringResource(R.string.focusTagsLabelName),
        colorLabel = stringResource(R.string.focusTagsLabelColor),
        confirmLabel = if (tag == null) title else stringResource(R.string.commonLabelSave),
        initialName = tag?.name.orEmpty(),
        initialColor = tag?.let { Color(it.colorValue) } ?: TaskColor,
        colors = TagColors,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isNameTaken = { name -> allTags.any { it.id != tag?.id && TagEntity.isSameName(it.name, name) } },
        nameTakenMessage = stringResource(R.string.tagNameTaken),
    )
}

/**
 * The tag field shared by every form: the selected tags as chips, read-only unless [isEditing],
 * where they open a sheet for picking from all tags or creating a new one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPicker(
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val allTags by tagViewModel.allTags.collectAsState()
    val selectedTags = allTags.filter { it.id in selectedIds }
    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.globalLabelTags),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppTheme.spaceSmall))
        if (!isEditing && selectedTags.isEmpty()) {
            Text(stringResource(R.string.focusTargetUntagged))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
        ) {
            selectedTags.forEach { tag ->
                NeoFilterChip(
                    selected = true,
                    enabled = isEditing,
                    onClick = { showSheet = true },
                    label = tag.name,
                    selectedColor = Color(tag.colorValue),
                )
            }
            if (isEditing) {
                NeoFilterChip(
                    selected = false,
                    onClick = { showSheet = true },
                    label = stringResource(R.string.tagPickerChoose),
                )
            }
        }
    }

    if (showSheet) {
        TagSelectorSheet(
            allTags = allTags,
            selectedIds = selectedIds,
            onSelectionChange = onSelectionChange,
            onCreateTag = tagViewModel::createTag,
            onDismiss = { showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSelectorSheet(
    allTags: List<TagEntity>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onCreateTag: (String, Int, (TagEntity) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    // A new tag arrives after the database write, by when the selection may have changed.
    val currentSelection by rememberUpdatedState(selectedIds)
    val currentOnChange by rememberUpdatedState(onSelectionChange)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.globalLabelTags),
            fontSize = AppTheme.fsHeadingSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = AppTheme.spaceLarge,
                end = AppTheme.spaceLarge,
                bottom = AppTheme.spaceMedium
            ),
        )
        if (allTags.isEmpty()) {
            Text(
                text = stringResource(R.string.focusTagsLabelEmpty),
                modifier = Modifier.padding(AppTheme.spaceLarge),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(allTags, key = { it.id }) { tag ->
                val isSelected = tag.id in selectedIds
                ListItem(
                    leadingContent = { TagDot(tag.colorValue, Modifier.size(24.dp)) },
                    headlineContent = {
                        Text(
                            tag.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = SuccessColor)
                        }
                    },
                    modifier = Modifier.clickable {
                        onSelectionChange(if (isSelected) selectedIds - tag.id else selectedIds + tag.id)
                    },
                )
                HorizontalDivider()
            }
        }
        ListItem(
            leadingContent = { Icon(Icons.Filled.AddCircleOutline, contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.focusTagsLabelAdd)) },
            modifier = Modifier.clickable { showCreateDialog = true },
        )
    }

    if (showCreateDialog) {
        TagEditDialog(
            tag = null,
            allTags = allTags,
            onConfirm = { name, colorValue ->
                onCreateTag(name, colorValue) { created -> currentOnChange(currentSelection + created.id) }
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }
}
