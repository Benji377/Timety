package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Flat, hairline-bordered modal surface: the app's replacement for Material3's
 * [androidx.compose.material3.AlertDialog], whose default shape/elevation don't have a border
 * param to override. The dialog is separated from the scrim behind it by its own surface fill, so
 * it needs no elevation of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        Surface(
            shape = AppTheme.brNeo,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(AppTheme.borderHairline, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(AppTheme.spaceXLarge), content = content)
        }
    }
}

/**
 * Convenience overload of [NeoDialog] mirroring the common title/text/confirm/dismiss shape of
 * Material3's `AlertDialog`, for callers migrating away from it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    NeoDialog(onDismissRequest = onDismissRequest) {
        Text(title, fontSize = AppTheme.fsHeadingSmall, fontWeight = FontWeight.Bold)
        if (text != null) {
            Spacer(modifier = Modifier.height(AppTheme.spaceMedium))
            Text(
                text,
                fontSize = AppTheme.fsBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            dismissButton?.invoke()
            if (dismissButton != null) {
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
            }
            confirmButton()
        }
    }
}

/**
 * Composable-slot overload of [NeoAlertDialog], shaped to match Material3's `AlertDialog` almost
 * exactly (`title`/`text` as optional composable slots rather than plain strings) so a raw
 * `AlertDialog(...)` call site can migrate to the neo-styled dialog container by swapping the
 * function name alone. [title] and [text], when present, inherit this dialog's standard heading
 * and body text styles via [ProvideTextStyle] so callers don't have to restate
 * font size/weight/color on every `Text(...)` inside them - only an explicit style on a caller's
 * own `Text` overrides it.
 */
@Composable
fun NeoAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    NeoDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        if (title != null) {
            ProvideTextStyle(TextStyle(fontSize = AppTheme.fsHeadingSmall, fontWeight = FontWeight.Bold)) {
                title()
            }
        }
        if (title != null && text != null) {
            Spacer(modifier = Modifier.height(AppTheme.spaceMedium))
        }
        if (text != null) {
            ProvideTextStyle(
                TextStyle(
                    fontSize = AppTheme.fsBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                text()
            }
        }
        Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            dismissButton?.invoke()
            if (dismissButton != null) {
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
            }
            confirmButton()
        }
    }
}
