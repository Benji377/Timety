package io.github.benji377.timety.ui.screens.location

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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    settingsViewModel: io.github.benji377.timety.ui.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = io.github.benji377.timety.ui.viewmodel.AppViewModelProvider.Factory
    ),
    onLocationSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val locationApiEndpoint by settingsViewModel.locationApiEndpoint.collectAsState()

    val performSearch: (String) -> Unit = { searchQuery ->
        searchJob?.cancel()
        if (searchQuery.trim().length >= 3) {
            searchJob = coroutineScope.launch {
                delay(600) // Debounce
                isLoading = true
                errorMessage = null

                try {
                    val endpoint = locationApiEndpoint
                    val baseUrl = if (endpoint.endsWith("/")) endpoint else "$endpoint/"

                    val results = withContext(Dispatchers.IO) {
                        val url = URL("${baseUrl}?q=${searchQuery.trim()}&limit=10")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty(
                            "User-Agent",
                            "timety/1.0 (io.github.benji377.timety)"
                        )
                        connection.setRequestProperty("Accept", "application/json")
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000

                        if (connection.responseCode == 200) {
                            val responseStr =
                                connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(responseStr)
                            val featuresArray = json.optJSONArray("features")
                            val list = mutableListOf<JSONObject>()
                            if (featuresArray != null) {
                                for (i in 0 until featuresArray.length()) {
                                    list.add(featuresArray.getJSONObject(i))
                                }
                            }
                            list
                        } else {
                            throw ServerException(connection.responseCode)
                        }
                    }
                    searchResults = results
                } catch (e: ServerException) {
                    errorMessage = context.getString(R.string.locationPickerServerError, e.code)
                    searchResults = emptyList()
                } catch (e: Exception) {
                    errorMessage = context.getString(R.string.locationPickerNetworkError)
                    searchResults = emptyList()
                } finally {
                    isLoading = false
                }
            }
        } else {
            searchResults = emptyList()
            isLoading = false
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(stringResource(R.string.locationPickerTitle)) },
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

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
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
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.locationPickerStartTyping),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.locationPickerMinChars),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else if (!isLoading && searchResults.isEmpty() && errorMessage == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.locationPickerNoResults, query.trim()),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { feature ->
                            val properties = feature.optJSONObject("properties") ?: JSONObject()
                            val title = getPrimaryName(properties)
                                .ifEmpty { stringResource(R.string.locationPickerUnknown) }
                            val subtitle = buildDetailsString(properties)

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
                                        tint = Color.Gray
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

private fun getPrimaryName(p: JSONObject): String {
    val name = p.optString("name", "")
    if (name.isNotEmpty()) return name

    val street = p.optString("street", "")
    val number = p.optString("housenumber", "")
    if (street.isNotEmpty()) {
        return if (number.isNotEmpty()) "$street $number" else street
    }

    val city = p.optString("city", "")
    val state = p.optString("state", "")
    if (city.isNotEmpty()) return city
    if (state.isNotEmpty()) return state

    return ""
}

private fun buildDetailsString(p: JSONObject): String {
    val parts = mutableListOf<String>()

    val type = p.optString("osm_value", "")
    if (type.isNotEmpty() && type != "yes") {
        parts.add(type.replaceFirstChar { it.uppercase() })
    }

    val street = p.optString("street", "")
    val number = p.optString("housenumber", "")
    var streetInfo = ""
    if (street.isNotEmpty()) streetInfo += street
    if (street.isNotEmpty() && number.isNotEmpty()) streetInfo += " $number"
    if (streetInfo.isNotEmpty()) parts.add(streetInfo)

    var cityStr = p.optString("city", "")
    if (cityStr.isEmpty()) cityStr = p.optString("town", "")
    if (cityStr.isEmpty()) cityStr = p.optString("village", "")

    val state = p.optString("state", "")
    val postcode = p.optString("postcode", "")

    val locationParts = mutableListOf<String>()
    if (cityStr.isNotEmpty()) locationParts.add(cityStr)
    if (state.isNotEmpty()) locationParts.add(state)
    if (postcode.isNotEmpty()) locationParts.add(postcode)

    if (locationParts.isNotEmpty()) {
        parts.add(locationParts.joinToString(", "))
    }

    return parts.joinToString(" • ")
}

private class ServerException(val code: Int) : Exception("Server error: $code")
