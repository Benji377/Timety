package io.github.benji377.timety.ui.components.location

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationApiDialog(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val endpointFlow =
        settingsRepository.locationApiEndpointFlow.collectAsState(initial = SettingsRepository.DEFAULT_LOCATION_API_ENDPOINT)
    var endpointText by remember(endpointFlow.value) { mutableStateOf(endpointFlow.value) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var hasValidatedConnection by remember { mutableStateOf(false) }
    var hasTestFailed by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.locationApiDialogTitle)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = endpointText,
                    onValueChange = {
                        endpointText = it
                        hasValidatedConnection = false
                        hasTestFailed = false
                    },
                    label = { Text(stringResource(R.string.locationApiDialogLabel)) },
                    placeholder = { Text(SettingsRepository.DEFAULT_LOCATION_API_ENDPOINT) },
                    modifier = Modifier.fillMaxWidth()
                )

                InfoItem(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.locationApiDialogSupported),
                    subtitle = stringResource(R.string.locationApiDialogSupportedDesc)
                )

                Row(
                    modifier = Modifier
                        .padding(start = 32.dp, top = 0.dp, bottom = 0.dp)
                        .clickable { uriHandler.openUri("https://github.com/komoot/photon") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.locationApiDialogLearnMore),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                }

                InfoItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.locationApiDialogBestPractice),
                    subtitle = stringResource(R.string.locationApiDialogBestPracticeDesc)
                )

                if (hasValidatedConnection && !hasTestFailed) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(
                            2.dp,
                            Color(0xFF4CAF50),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.locationApiDialogSuccess),
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (hasTestFailed) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.locationApiDialogError),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        settingsRepository.saveLocationApiEndpoint(endpointText.trim())
                        onDismiss()
                    }
                },
                enabled = hasValidatedConnection || endpointText.trim().isEmpty()
            ) {
                Text(stringResource(R.string.commonLabelSave))
            }
        },
        dismissButton = {
            Row {
                if (endpointText != SettingsRepository.DEFAULT_LOCATION_API_ENDPOINT) {
                    TextButton(onClick = {
                        endpointText = SettingsRepository.DEFAULT_LOCATION_API_ENDPOINT
                        hasValidatedConnection = false
                        hasTestFailed = false
                    }) {
                        Text(stringResource(R.string.locationApiDialogReset))
                    }
                }
                TextButton(
                    onClick = {
                        isTestingConnection = true
                        hasTestFailed = false
                        coroutineScope.launch {
                            val success = validateLocationApiEndpoint(endpointText.trim())
                            isTestingConnection = false
                            hasValidatedConnection = success
                            hasTestFailed = !success
                        }
                    },
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.locationApiDialogTest))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.commonLabelCancel))
                }
            }
        }
    )
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private suspend fun validateLocationApiEndpoint(endpoint: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val baseUrl = if (endpoint.endsWith("/")) endpoint else "$endpoint/"
            val url = URL("${baseUrl}?q=berlin&limit=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "timety/1.0 (io.github.benji377.timety)")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val code = connection.responseCode
            connection.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }
    }
