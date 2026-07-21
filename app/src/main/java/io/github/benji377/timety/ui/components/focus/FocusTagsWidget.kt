package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.NamedColorEditDialog
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.ui.components.common.NeoButton as Button


/** Screen listing focus tags, with actions to create, edit, and delete them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTagsWidget(
    onNavigateBack: () -> Unit,
    focusViewModel: FocusViewModel = activityScopedViewModel(),
) {
    val tags by focusViewModel.allTags.collectAsState()
    var tagDialogTarget by remember { mutableStateOf<FocusTagEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var tagPendingDelete by remember { mutableStateOf<FocusTagEntity?>(null) }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.focusTagsTitle),
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = AppTheme.space3XLarge),
        ) {
            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spaceLarge),
                    colors = ButtonDefaults.buttonColors(containerColor = FocusColor),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                    Text(stringResource(R.string.focusTagsLabelAdd))
                }
            }
            if (tags.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.focusTagsLabelEmpty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spaceLarge),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(tags, key = { it.id }) { tag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppTheme.spaceSmall,
                                vertical = AppTheme.spaceXSmall
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppTheme.spaceMedium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(tag.colorValue)),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                            Text(tag.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { tagDialogTarget = tag }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.focusTagsLabelEdit)
                                )
                            }
                            if (!tag.id.startsWith("default_tag")) {
                                IconButton(onClick = { tagPendingDelete = tag }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.commonLabelDelete),
                                        tint = ErrorColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NamedColorEditDialog(
            title = stringResource(R.string.focusTagsLabelAdd),
            nameLabel = stringResource(R.string.focusTagsLabelName),
            colorLabel = stringResource(R.string.focusTagsLabelColor),
            confirmLabel = stringResource(R.string.focusTagsLabelAdd),
            initialName = "",
            initialColor = FocusColor,
            colors = FOCUS_TAG_COLORS,
            onConfirm = { name, colorValue ->
                focusViewModel.createTag(name, colorValue)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
    tagDialogTarget?.let { tag ->
        NamedColorEditDialog(
            title = stringResource(R.string.focusTagsLabelEdit),
            nameLabel = stringResource(R.string.focusTagsLabelName),
            colorLabel = stringResource(R.string.focusTagsLabelColor),
            confirmLabel = stringResource(R.string.commonLabelSave),
            initialName = tag.name,
            initialColor = Color(tag.colorValue),
            colors = FOCUS_TAG_COLORS,
            onConfirm = { name, colorValue ->
                focusViewModel.updateTag(tag.id, name, colorValue)
                tagDialogTarget = null
            },
            onDismiss = { tagDialogTarget = null },
        )
    }

    val deleteTarget = tagPendingDelete
    ConfirmationDialog(
        visible = deleteTarget != null,
        title = stringResource(R.string.focusTagsDialogTitleDelete),
        content = deleteTarget?.let {
            stringResource(
                R.string.focusTagsDialogContentDelete,
                it.name
            )
        } ?: "",
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            deleteTarget?.let { focusViewModel.deleteTag(it) }
            tagPendingDelete = null
        },
        onDismiss = { tagPendingDelete = null },
    )
}

private val FOCUS_TAG_COLORS = listOf(FocusColor) + PickerPalette
