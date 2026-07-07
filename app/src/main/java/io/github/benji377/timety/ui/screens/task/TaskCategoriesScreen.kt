package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import androidx.compose.foundation.lazy.grid.items as gridItems
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
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
        CategoryEditDialog(
            initialCategory = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, colorValue ->
                taskViewModel.createCategory(name, colorValue)
                showAddDialog = false
            },
        )
    }
    categoryDialogTarget?.let { category ->
        CategoryEditDialog(
            initialCategory = category,
            onDismiss = { categoryDialogTarget = null },
            onConfirm = { name, colorValue ->
                taskViewModel.updateCategory(category, name, colorValue)
                categoryDialogTarget = null
            },
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

@Composable
private fun CategoryEditDialog(
    initialCategory: TaskCategoryEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorValue: Int) -> Unit,
) {
    val isEditing = initialCategory != null
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedColor by remember {
        mutableStateOf(initialCategory?.let { Color(it.colorValue) } ?: TaskColor)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) stringResource(R.string.categoryEditTitle)
                else stringResource(R.string.categoryAddLabel)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.categoryNameLabel)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                Text(stringResource(R.string.categoryColorLabel))
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                ) {
                    gridItems(CATEGORY_COLORS) { optionColor ->
                        val isSelected = optionColor == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(optionColor)
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                                    else Modifier,
                                )
                                .clickable { selectedColor = optionColor },
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
        confirmButton = {
            Button(onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    onConfirm(trimmed, selectedColor.toArgb())
                }
            }) {
                Text(
                    if (isEditing) stringResource(R.string.commonLabelSave)
                    else stringResource(R.string.categoryAddLabel)
                )
            }
        },
    )
}

private val CATEGORY_COLORS = listOf(TaskColor) + PickerPalette
