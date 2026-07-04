package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.TextInputDialog
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel

/**
 * Category management screen. Mirrors `widgets/task/task_categories_widget.dart`
 * (Flutter's `CategoriesWidget`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoriesScreen(
    onNavigateBack: () -> Unit,
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val categories = taskViewModel.getAllCategories()

    var editingCategory by remember { mutableStateOf<String?>(null) }
    var deletingCategory by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background),
                title = { Text(stringResource(R.string.categoryTitle), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.categoryEmpty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                items(categories) { category ->
                    val taskCount = tasks.count { it.task.category == category }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ListItem(
                                modifier = Modifier.weight(1f),
                                leadingContent = {
                                    Icon(Icons.Filled.Label, contentDescription = null)
                                },
                                headlineContent = { Text(category) },
                                supportingContent = {
                                    Text(quantityString(R.plurals.categoryTaskCount, taskCount, zeroRes = R.string.categoryTaskCountZero, taskCount))
                                },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                            )
                            IconButton(onClick = { editingCategory = category }) {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                            }
                            IconButton(onClick = { deletingCategory = category }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorColor)
                            }
                        }
                    }
                }
            }
        }
    }

    TextInputDialog(
        visible = editingCategory != null,
        title = stringResource(R.string.categoryEditTitle),
        labelText = stringResource(R.string.categoryNameLabel),
        initialValue = editingCategory ?: "",
        onConfirm = { newName ->
            val oldName = editingCategory
            if (oldName != null && newName != oldName) {
                taskViewModel.renameCategory(oldName, newName)
            }
            editingCategory = null
        },
        onDismiss = { editingCategory = null }
    )

    ConfirmationDialog(
        visible = deletingCategory != null,
        title = stringResource(R.string.categoryDeleteTitle),
        content = stringResource(R.string.categoryDeleteContent, deletingCategory ?: ""),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            deletingCategory?.let { taskViewModel.deleteCategory(it) }
            deletingCategory = null
        },
        onDismiss = { deletingCategory = null }
    )
}
