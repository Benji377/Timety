package io.github.benji377.timety.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.components.common.NeoDurationPickerDialog
import io.github.benji377.timety.ui.components.common.NeoTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import kotlin.math.roundToInt


/** Top bar with a back button over a scrolling column; the frame shared by every settings page. */
@Composable
internal fun SettingsPageScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            NeoTopBar(
                title = title,
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
            content = content
        )
    }
}

/** Uppercase accent label above a group of settings. */
@Composable
internal fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodySmall.copy(
            color = TaskColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
    )
}

/** Separates two groups on the same settings page. */
@Composable
internal fun SettingsGroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}

/** Icon at the end of a [NavSettingsItem], hinting at what tapping it does. */
internal enum class SettingsTrailing {
    /** Opens another page or a picker dialog. */
    CHEVRON,

    /** Opens a dialog that edits a free-form value. */
    EDIT,

    /** Leaves the app (browser, system settings). */
    EXTERNAL,

    /** Runs an action straight away. */
    NONE,
}

/** A tappable settings row; [subtitle] may span several lines. */
@Composable
internal fun NavSettingsItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    iconTint: Color = LocalContentColor.current,
    trailing: SettingsTrailing = SettingsTrailing.CHEVRON,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = iconTint) },
        trailingContent = when (trailing) {
            SettingsTrailing.CHEVRON -> {
                { Icon(Icons.Filled.ChevronRight, null) }
            }

            SettingsTrailing.EDIT -> {
                { Icon(Icons.Filled.Edit, null) }
            }

            SettingsTrailing.EXTERNAL -> {
                {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsTrailing.NONE -> null
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** A settings row with an on/off switch; the whole row toggles, like Android's own settings. */
@Composable
internal fun SwitchSettingsItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    iconTint: Color = LocalContentColor.current,
    enabled: Boolean = true,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = iconTint) },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = SuccessColor)
            )
        },
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    )
}

/** A settings row showing the current choice; tapping it opens a menu of [options]. */
@Composable
internal fun DropdownSettingsItem(
    headlineText: String,
    leadingIcon: @Composable () -> Unit,
    currentLabel: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    optionIcon: (@Composable (String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(headlineText) },
        leadingContent = leadingIcon,
        trailingContent = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        currentLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            AppTheme.brNeo
                        )
                ) {
                    options.forEach { (label, value) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = optionIcon?.let { { it(value) } },
                            onClick = {
                                onSelect(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { expanded = !expanded }
    )
}

internal data class NumberDialogSpec(
    val title: String,
    val current: Int,
    val min: Int,
    val max: Int,
    val unit: String,
    val onSave: (Int) -> Unit
)

internal data class DurationDialogSpec(
    val title: String,
    val current: Int,
    val min: Int,
    val max: Int,
    val onSave: (Int) -> Unit
)

internal data class TimeDialogSpec(
    val title: String,
    val current: String,
    val onSave: (String) -> Unit
)

@Composable
internal fun NumberPickerDialog(spec: NumberDialogSpec, onDismiss: () -> Unit) {
    var value by remember(spec) { mutableFloatStateOf(spec.current.toFloat()) }
    NeoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${value.toInt()} ${spec.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                // Continuous slider rounded to whole units: precise selection without
                // the coarse 5-unit jumps (or dozens of tick marks) a stepped slider gives.
                Slider(
                    value = value,
                    onValueChange = { value = it.roundToInt().toFloat() },
                    valueRange = spec.min.toFloat()..spec.max.toFloat()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                spec.onSave(value.toInt())
                onDismiss()
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        }
    )
}

@Composable
internal fun DurationPickerDialog(spec: DurationDialogSpec, onDismiss: () -> Unit) {
    NeoDurationPickerDialog(
        initialMinutes = spec.current,
        minMinutes = spec.min,
        maxMinutes = spec.max,
        title = { Text(spec.title) },
        confirmLabel = stringResource(R.string.commonLabelSave),
        onConfirm = {
            spec.onSave(it)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
internal fun TimePickerDialogRow(spec: TimeDialogSpec, onDismiss: () -> Unit) {
    val (initialHour, initialMinute) = AppDateFormatUtils.parseHHmm(spec.current)
    NeoTimePickerDialog(
        initialHour = initialHour,
        initialMinute = initialMinute,
        title = { Text(spec.title) },
        confirmLabel = stringResource(R.string.commonLabelSave),
        onConfirm = { hour, minute ->
            spec.onSave("%02d:%02d".format(hour, minute))
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
