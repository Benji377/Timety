package io.github.benji377.timety.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.RestoreOutcome
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import kotlinx.coroutines.launch

/** Data & backup settings: export to a file or share sheet, and restore from a backup file. */
@Composable
internal fun DataBackupSettings(
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
) {
    val context = LocalContext.current
    val backupExportSuccessStr = stringResource(R.string.backupExportSuccess)
    val backupExportFailureRaw = stringResource(R.string.backupExportFailure)
    val backupExportSubjectStr = stringResource(R.string.backupExportSubject)
    val backupShareFailureRaw = stringResource(R.string.backupShareFailure)
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val backupService = remember {
        (context.applicationContext as TimetyApplication).container.backupService
    }
    val restoreOutcome by settingsViewModel.restoreOutcome.collectAsState()

    // Import flow: pick a file, confirm the overwrite, restore the backup, then prompt a restart.
    // Saveable so a rotation between picking the file and confirming doesn't drop the import.
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showExportOptions by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = backupService.exportToUri(uri)
            val msg = if (result.isSuccess) {
                backupExportSuccessStr
            } else {
                backupExportFailureRaw.format(result.exceptionOrNull()?.message ?: "")
            }
            snackbarHostState.showSnackbar(msg)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    Spacer(modifier = Modifier.height(8.dp))
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelExportData),
        subtitle = stringResource(R.string.settingsLabelExportDataSubtitle),
        icon = Icons.Outlined.UploadFile,
        iconTint = TaskColor,
        trailing = SettingsTrailing.NONE,
        onClick = { showExportOptions = true }
    )
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelImportData),
        subtitle = stringResource(R.string.settingsLabelImportDataSubtitle),
        icon = Icons.Outlined.Download,
        iconTint = FocusColor,
        trailing = SettingsTrailing.NONE,
        onClick = { importLauncher.launch(arrayOf("application/json")) }
    )

    // Import confirmation dialog warning that restoring a backup overwrites existing data.
    ConfirmationDialog(
        visible = pendingImportUri != null,
        title = stringResource(R.string.backupImportConfirmTitle),
        content = stringResource(R.string.backupImportConfirmBody),
        onConfirm = {
            pendingImportUri?.let { settingsViewModel.restoreBackup(it) }
            pendingImportUri = null
        },
        onDismiss = { pendingImportUri = null }
    )

    // Export options dialog: save the backup to device storage or share it elsewhere.
    if (showExportOptions) {
        NeoAlertDialog(
            onDismissRequest = { showExportOptions = false },
            title = { Text(stringResource(R.string.backupExportTitle)) },
            text = {
                Column {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.backupActionSaveDevice)) },
                        supportingContent = { Text(stringResource(R.string.backupActionSaveDeviceSubtitle)) },
                        leadingContent = { Icon(Icons.Outlined.Download, null, tint = TaskColor) },
                        modifier = Modifier.clickable {
                            showExportOptions = false
                            exportLauncher.launch(backupService.suggestedFileName())
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.backupActionShareCloud)) },
                        supportingContent = { Text(stringResource(R.string.backupActionShareCloudSubtitle)) },
                        leadingContent = { Icon(Icons.Outlined.UploadFile, null, tint = FocusColor) },
                        modifier = Modifier.clickable {
                            showExportOptions = false
                            scope.launch {
                                backupService.exportToShareUri().fold(
                                    onSuccess = { uri ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, backupExportSubjectStr)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, backupExportSubjectStr)
                                        )
                                    },
                                    onFailure = { e ->
                                        snackbarHostState.showSnackbar(
                                            backupShareFailureRaw.format(e.message ?: "")
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportOptions = false }) {
                    Text(stringResource(R.string.commonLabelCancel))
                }
            }
        )
    }

    // Shows why a restore failed; nothing was changed. Dismissing consumes the outcome.
    (restoreOutcome as? RestoreOutcome.Failure)?.let { failure ->
        NeoAlertDialog(
            onDismissRequest = { settingsViewModel.consumeRestoreOutcome() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backupImportFailedTitle))
                }
            },
            text = { Text(failure.message) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.consumeRestoreOutcome() }) {
                    Text(stringResource(R.string.commonButtonGotIt))
                }
            }
        )
    }

    // Prompts the user to restart the app after a successful backup restore.
    if (restoreOutcome == RestoreOutcome.Success) {
        NeoAlertDialog(
            onDismissRequest = { settingsViewModel.consumeRestoreOutcome() },
            title = { Text(stringResource(R.string.backupRestoreSuccessTitle)) },
            text = { Text(stringResource(R.string.backupRestoreSuccessBody)) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.consumeRestoreOutcome() }) {
                    Text(stringResource(R.string.commonButtonGotIt))
                }
            }
        )
    }
}
