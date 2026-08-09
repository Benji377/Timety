package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        // Bottom edge so every header reads as a distinct band above the content.
        HorizontalDivider(
            thickness = AppTheme.borderHairline,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * The standard back arrow for [NeoTopBar]'s navigationIcon slot, with a localized content
 * description so TalkBack announces it. Bordered via [NeoIconButton], matching the action icons on
 * the opposite side of the bar.
 *
 * The trailing gap is a full [AppTheme.spaceLarge]: M3 places the title immediately after the
 * navigation slot, so a bordered button needs real separation from the first letter of the title -
 * bare glyphs could sit closer because they had no visible edge to collide with.
 */
@Composable
fun BackNavigationIcon(onClick: () -> Unit) {
    NeoIconButton(
        onClick = onClick,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(R.string.commonBack),
        modifier = Modifier.padding(start = AppTheme.spaceSmall, end = AppTheme.spaceLarge),
    )
}

/**
 * Top bar actions for the detail screens' view/edit modes: delete and edit buttons while
 * viewing an existing item, a save button while editing or creating. Bordered via [NeoIconButton]
 * instead of bare glyphs, so every icon in a header carries a container of its own.
 */
@Composable
fun DetailTopBarActions(
    isViewing: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
) {
    if (isViewing) {
        NeoIconButton(
            onClick = onDelete,
            icon = Icons.Filled.DeleteOutline,
            contentDescription = stringResource(R.string.commonLabelDelete),
            contentColor = ErrorColor,
            modifier = Modifier.padding(end = AppTheme.spaceSmall),
        )
        NeoIconButton(
            onClick = onEdit,
            icon = Icons.Filled.Edit,
            contentDescription = null,
            modifier = Modifier.padding(end = AppTheme.spaceSmall),
        )
    } else {
        NeoIconButton(
            onClick = onSave,
            icon = Icons.Filled.Check,
            contentDescription = stringResource(R.string.commonLabelSave),
            modifier = Modifier.padding(end = AppTheme.spaceSmall),
        )
    }
}
