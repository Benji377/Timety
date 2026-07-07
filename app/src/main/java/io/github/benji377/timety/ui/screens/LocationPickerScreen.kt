package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.R
import androidx.compose.runtime.LaunchedEffect
import io.github.benji377.timety.util.location.LocationApi
import io.github.benji377.timety.util.location.LocationServerException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AppViewModelProvider.Factory
    ),
    initialQuery: String = "",
    onLocationSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var errorState by remember { mutableStateOf<LocationError?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val locationApiEndpoint by settingsViewModel.locationApiEndpoint.collectAsState()

    val performSearch: (String) -> Unit = { searchQuery ->
        searchJob?.cancel()
        if (searchQuery.trim().length >= 3) {
            searchJob = coroutineScope.launch {
                delay(600.milliseconds) // Debounce
                isLoading = true
                errorState = null

                try {
                    searchResults = LocationApi.search(locationApiEndpoint, searchQuery)
                } catch (e: LocationServerException) {
                    errorState = LocationError.Server(e.code)
                    searchResults = emptyList()
                } catch (e: Exception) {
                    errorState = LocationError.Network
                    searchResults = emptyList()
                } finally {
                    isLoading = false
                }
            }
        } else {
            searchResults = emptyList()
            isLoading = false
            errorState = null
        }
    }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) performSearch(initialQuery)
    }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.locationPickerTitle),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.commonBack)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    performSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.locationPickerHint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            performSearch("")
                        }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.commonClear)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val currentError = errorState
            if (currentError != null) {
                val errorMsg = when (currentError) {
                    is LocationError.Server -> stringResource(R.string.locationPickerServerError, currentError.code)
                    is LocationError.Network -> stringResource(R.string.locationPickerNetworkError)
                }
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (query.trim().length < 3) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.locationPickerStartTyping),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.locationPickerMinChars),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!isLoading && searchResults.isEmpty() && errorState == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.locationPickerNoResults, query.trim()),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { feature ->
                            val properties = feature.optJSONObject("properties") ?: JSONObject()
                            val title = LocationApi.primaryName(properties)
                                .ifEmpty { stringResource(R.string.locationPickerUnknown) }
                            val subtitle = LocationApi.detailsString(properties)

                            ListItem(
                                headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
                                supportingContent = if (subtitle.isNotEmpty()) {
                                    {
                                        Text(
                                            subtitle,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else null,
                                leadingContent = {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onLocationSelected(title)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private sealed class LocationError {
    class Server(val code: Int) : LocationError()
    object Network : LocationError()
}
