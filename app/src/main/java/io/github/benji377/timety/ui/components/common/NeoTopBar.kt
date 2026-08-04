package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor

/**
 * Standard top bar: app background color (no surface tint) and a bold title,
 * so every screen carries the same header treatment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    NeoTopBar(
        titleContent = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

/** Variant for screens whose title area is richer than a single line of text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoTopBar(
    titleContent: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actions,
        )
        // Bold bottom edge so every header reads as a distinct neobrutalist band.
        HorizontalDivider(
            thickness = AppTheme.neoBorderWidth,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * The standard back arrow for [NeoTopBar]'s navigationIcon slot, with a localized
 * content description so TalkBack announces it.
 */
@Composable
fun BackNavigationIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.commonBack),
        )
    }
}

/**
 * Top bar actions for the detail screens' view/edit modes: delete and edit buttons while
 * viewing an existing item, a save button while editing or creating.
 */
@Composable
fun DetailTopBarActions(
    isViewing: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
) {
    if (isViewing) {
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.commonLabelDelete),
                tint = ErrorColor
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = null)
        }
    } else {
        IconButton(onClick = onSave) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.commonLabelSave)
            )
        }
    }
}
