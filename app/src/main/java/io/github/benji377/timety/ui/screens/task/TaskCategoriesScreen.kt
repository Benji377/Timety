package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.NamedColorEditDialog
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.components.common.TimetyButton as Button


/**
 * Lists task categories with per-category task counts and lets the user add, edit, or delete them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoriesScreen(
    onNavigateBack: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val categories by taskViewModel.allCategories.collectAsState()

    var categoryDialogTarget by remember { mutableStateOf<TaskCategoryEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryPendingDelete by remember { mutableStateOf<TaskCategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.categoryTitle),
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = AppTheme.space3XLarge),
        ) {
            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spaceLarge),
                    colors = ButtonDefaults.buttonColors(containerColor = TaskColor),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                    Text(stringResource(R.string.categoryAddLabel))
                }
            }
            if (categories.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.categoryEmpty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spaceLarge),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(categories, key = { it.id }) { category ->
                    val taskCount = tasks.count { it.task.category == category.name }
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
                                    .background(Color(category.colorValue)),
                            )
                            Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category.name)
                                Text(
                                    quantityString(
                                        R.plurals.categoryTaskCount,
                                        taskCount,
                                        zeroRes = R.string.categoryTaskCountZero,
                                        taskCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { categoryDialogTarget = category }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.categoryEditTitle)
                                )
                            }
                            IconButton(onClick = { categoryPendingDelete = category }) {
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

    if (showAddDialog) {
        NamedColorEditDialog(
            title = stringResource(R.string.categoryAddLabel),
            nameLabel = stringResource(R.string.categoryNameLabel),
            colorLabel = stringResource(R.string.categoryColorLabel),
            confirmLabel = stringResource(R.string.categoryAddLabel),
            initialName = "",
            initialColor = TaskColor,
            colors = CATEGORY_COLORS,
            onConfirm = { name, colorValue ->
                taskViewModel.createCategory(name, colorValue)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
    categoryDialogTarget?.let { category ->
        NamedColorEditDialog(
            title = stringResource(R.string.categoryEditTitle),
            nameLabel = stringResource(R.string.categoryNameLabel),
            colorLabel = stringResource(R.string.categoryColorLabel),
            confirmLabel = stringResource(R.string.commonLabelSave),
            initialName = category.name,
            initialColor = Color(category.colorValue),
            colors = CATEGORY_COLORS,
            onConfirm = { name, colorValue ->
                taskViewModel.updateCategory(category, name, colorValue)
                categoryDialogTarget = null
            },
            onDismiss = { categoryDialogTarget = null },
        )
    }

    val deleteTarget = categoryPendingDelete
    ConfirmationDialog(
        visible = deleteTarget != null,
        title = stringResource(R.string.categoryDeleteTitle),
        content = stringResource(R.string.categoryDeleteContent, deleteTarget?.name ?: ""),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            deleteTarget?.let { taskViewModel.deleteCategory(it) }
            categoryPendingDelete = null
        },
        onDismiss = { categoryPendingDelete = null }
    )
}

private val CATEGORY_COLORS = listOf(TaskColor) + PickerPalette
