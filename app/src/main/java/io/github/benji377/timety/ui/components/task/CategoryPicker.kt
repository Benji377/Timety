package io.github.benji377.timety.ui.components.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.InfoColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField

/**
 * Outlined-text-field colors for values that are shown but not typed into: readable "disabled"
 * styling while editing, and plain transparent fields in view mode.
 */
@Composable
fun readOnlyFieldColors(isEditing: Boolean) = if (isEditing) {
    OutlinedTextFieldDefaults.colors(
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
} else {
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Category selector shared by the task and recurring-task detail screens: a dropdown over
 * [existingCategories] with an inline "add new" text-field flow, or a read-only field when
 * not editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    category: String,
    onCategoryChange: (String) -> Unit,
    isEditing: Boolean,
    isAddingNewCategory: Boolean,
    onIsAddingNewCategoryChange: (Boolean) -> Unit,
    newCategoryText: String,
    onNewCategoryTextChange: (String) -> Unit,
    existingCategories: List<TaskCategoryEntity>,
    onCreateCategory: (String) -> Unit,
) {
    val selectedColorValue = existingCategories.firstOrNull { it.name == category }?.colorValue
    val categoryLeadingIcon: @Composable () -> Unit = {
        if (selectedColorValue != null) AppUtils.CategoryDot(selectedColorValue)
        else Icon(Icons.AutoMirrored.Filled.Label, null)
    }

    if (!isEditing) {
        OutlinedTextField(
            value = category.ifEmpty { stringResource(R.string.taskDetailsLabelCategoryEmpty) },
            onValueChange = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.taskDetailsLabelCategory)) },
            leadingIcon = categoryLeadingIcon,
            colors = readOnlyFieldColors(isEditing = false)
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isAddingNewCategory) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(category.ifEmpty { stringResource(R.string.taskDetailsLabelCategorySelect) }) },
                    label = { Text(stringResource(R.string.taskDetailsLabelCategory)) },
                    leadingIcon = categoryLeadingIcon,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.taskDetailsLabelCategoryEmpty)) },
                        onClick = {
                            onCategoryChange("")
                            expanded = false
                        }
                    )
                    existingCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            leadingIcon = { AppUtils.CategoryDot(cat.colorValue) },
                            onClick = {
                                onCategoryChange(cat.name)
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = InfoColor,
                                    modifier = Modifier.size(AppTheme.iconSizeSmall)
                                )
                                Spacer(Modifier.width(AppTheme.spaceSmall))
                                Text(
                                    stringResource(R.string.taskDetailsLabelCategoryAddNew),
                                    color = InfoColor
                                )
                            }
                        },
                        onClick = {
                            onIsAddingNewCategoryChange(true)
                            expanded = false
                        }
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = newCategoryText,
                onValueChange = onNewCategoryTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.taskDetailsLabelCategoryNewName)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.LabelImportant, null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val trimmed = newCategoryText.trim()
                            if (trimmed.isNotEmpty()) {
                                onCreateCategory(trimmed)
                                onCategoryChange(trimmed)
                                onIsAddingNewCategoryChange(false)
                                onNewCategoryTextChange("")
                            }
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.commonLabelConfirm),
                                tint = SuccessColor
                            )
                        }
                        IconButton(onClick = { onIsAddingNewCategoryChange(false) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.commonLabelCancel),
                                tint = ErrorColor
                            )
                        }
                    }
                }
            )
        }
    }
}
