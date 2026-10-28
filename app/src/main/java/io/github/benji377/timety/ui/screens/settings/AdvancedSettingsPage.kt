package io.github.benji377.timety.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import kotlinx.coroutines.launch

/** Advanced settings: expert-only options, such as the location search API endpoint. */
@Composable
internal fun AdvancedSettings(
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
) {
    val locationApiEndpoint by settingsViewModel.locationApiEndpoint.collectAsState()
    val locationInvalidStr = stringResource(R.string.settingsLocationApiInvalid)
    val scope = rememberCoroutineScope()

    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var pendingLocationUrl by rememberSaveable { mutableStateOf(locationApiEndpoint) }
    var locationError by rememberSaveable { mutableStateOf<String?>(null) }
    // Not saveable: the validation coroutine dies with a configuration change, so a restored
    // "checking" flag would lock the dialog's buttons for good.
    var isCheckingLocation by remember { mutableStateOf(false) }

    fun closeLocationDialog() {
        showLocationDialog = false
        locationError = null
        pendingLocationUrl = locationApiEndpoint
    }

    if (showLocationDialog) {
        NeoAlertDialog(
            onDismissRequest = { closeLocationDialog() },
            title = { Text(stringResource(R.string.settingsLabelLocationApi)) },
            text = {
                Column {
                    NeoOutlinedTextField(
                        value = pendingLocationUrl,
                        onValueChange = {
                            pendingLocationUrl = it
                            locationError = null
                        },
                        label = { Text(stringResource(R.string.settingsLabelLocationApi)) },
                        singleLine = true,
                        isError = locationError != null,
                        supportingText = locationError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                    if (isCheckingLocation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settingsLabelValidating),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { closeLocationDialog() },
                    enabled = !isCheckingLocation
                ) { Text(stringResource(R.string.commonLabelCancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = pendingLocationUrl.trim()
                        if (trimmed.isEmpty()) {
                            settingsViewModel.setLocationApiEndpoint(trimmed)
                            showLocationDialog = false
                        } else {
                            scope.launch {
                                isCheckingLocation = true
                                val isValid = settingsViewModel.validateLocationApiEndpoint(trimmed)
                                isCheckingLocation = false
                                if (isValid) {
                                    settingsViewModel.setLocationApiEndpoint(trimmed)
                                    showLocationDialog = false
                                } else {
                                    locationError = locationInvalidStr
                                }
                            }
                        }
                    },
                    enabled = !isCheckingLocation
                ) { Text(stringResource(R.string.commonLabelSave)) }
            }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelLocationApi),
        subtitle = if (locationApiEndpoint.length > 40) {
            locationApiEndpoint.take(40) + "..."
        } else {
            locationApiEndpoint
        },
        icon = Icons.Filled.Cloud,
        iconTint = TaskColor,
        trailing = SettingsTrailing.EDIT,
        onClick = {
            pendingLocationUrl = locationApiEndpoint
            locationError = null
            showLocationDialog = true
        }
    )
}
