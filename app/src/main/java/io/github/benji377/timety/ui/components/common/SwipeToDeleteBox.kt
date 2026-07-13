package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor


/**
 * Wraps a list tile in end-to-start swipe-to-delete: the swipe reveals a red delete
 * background and opens a [ConfirmationDialog] instead of dismissing directly, so the tile
 * always springs back and only [onDelete] removes it.
 */
@Composable
fun SwipeToDeleteBox(
    dialogTitle: String,
    dialogContent: String,
    confirmLabel: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    margin: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            false
        },
    )

    ConfirmationDialog(
        visible = showDeleteDialog,
        title = dialogTitle,
        content = dialogContent,
        confirmLabel = confirmLabel,
        confirmColor = ErrorColor,
        onConfirm = {
            showDeleteDialog = false
            onDelete()
        },
        onDismiss = { showDeleteDialog = false },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            // Only visible mid-swipe: at rest the red would bleed through rounded corners
            // and through tiles without an opaque background (e.g. inside stack cards).
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(margin)
                        .clip(AppTheme.brMedium)
                        .background(ErrorColor),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.commonLabelDelete),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = AppTheme.spaceLarge)
                            .size(AppTheme.listTileSwipeIconSize),
                    )
                }
            }
        },
    ) {
        content()
    }
}
