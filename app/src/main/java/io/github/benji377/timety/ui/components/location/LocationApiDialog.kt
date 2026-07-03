package io.github.benji377.timety.ui.components.location

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val endpointFlow = settingsRepository.locationApiEndpointFlow.collectAsState(initial = "https://photon.komoot.io/api/")
    var endpointText by remember(endpointFlow.value) { mutableStateOf(endpointFlow.value) }
    
    var isTestingConnection by remember { mutableStateOf(false) }
    var hasValidatedConnection by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location API Endpoint") },
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
                        testError = null
                    },
                    label = { Text("API Endpoint URL") },
                    placeholder = { Text("https://photon.komoot.io/api/") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                InfoItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Supported APIs",
                    subtitle = "This app uses the Photon geocoder API format."
                )
                
                Row(
                    modifier = Modifier
                        .padding(start = 32.dp, top = 0.dp, bottom = 0.dp)
                        .clickable { uriHandler.openUri("https://github.com/komoot/photon") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Learn more about Photon", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
                }
                
                InfoItem(
                    icon = Icons.Default.Storage,
                    title = "Best Practice",
                    subtitle = "Consider self-hosting the API to improve privacy and avoid rate limits."
                )
                
                if (hasValidatedConnection && testError == null) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connection successful!", color = Color(0xFF4CAF50), fontSize = 12.sp)
                        }
                    }
                }
                
                if (testError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Error: $testError", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (endpointText != "https://photon.komoot.io/api/") {
                    TextButton(onClick = { 
                        endpointText = "https://photon.komoot.io/api/" 
                        hasValidatedConnection = false
                        testError = null
                    }) {
                        Text("Reset")
                    }
                }
                TextButton(
                    onClick = {
                        isTestingConnection = true
                        testError = null
                        coroutineScope.launch {
                            val success = validateLocationApiEndpoint(endpointText.trim())
                            isTestingConnection = false
                            if (success) {
                                hasValidatedConnection = true
                            } else {
                                testError = "Failed to connect"
                            }
                        }
                    },
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private suspend fun validateLocationApiEndpoint(endpoint: String): Boolean = withContext(Dispatchers.IO) {
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
