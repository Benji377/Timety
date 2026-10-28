package io.github.benji377.timety.util.location

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.benji377.timety.data.model.task.TaskPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LocationServerException(val code: Int) : Exception("Server error: $code")

/**
 * Photon-compatible forward-geocoding client used by the location picker.
 */
object LocationApi {
    /**
     * Searches the configured endpoint and returns the raw GeoJSON features.
     * Throws [LocationServerException] on a non-200 response and the usual
     * IO exceptions when offline or unreachable.
     */
    suspend fun search(endpoint: String, query: String, limit: Int = 10): List<JSONObject> =
        withContext(Dispatchers.IO) {
            val baseUrl = if (endpoint.endsWith("/")) endpoint else "$endpoint/"
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("${baseUrl}?q=$encodedQuery&limit=$limit")
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
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val featuresArray = JSONObject(responseStr).optJSONArray("features")
                buildList {
                    if (featuresArray != null) {
                        for (i in 0 until featuresArray.length()) {
                            add(featuresArray.getJSONObject(i))
                        }
                    }
                }
            } else {
                throw LocationServerException(connection.responseCode)
            }
        }

    /** Best single-line label for a search result: its name, else street, else city/state. */
    fun primaryName(p: JSONObject): String =
        p.text("name") ?: streetLine(p) ?: p.text("city") ?: p.text("state") ?: ""

    /** Secondary detail line for a search result: type, street/number, and city/state/postcode. */
    fun detailsString(p: JSONObject): String {
        val type = p.text("osm_value")?.takeIf { it != "yes" }?.replaceFirstChar { it.uppercase() }
        val city = p.text("city") ?: p.text("town") ?: p.text("village")
        val area = listOfNotNull(city, p.text("state"), p.text("postcode")).joinToString(", ").ifEmpty { null }
        return listOfNotNull(type, streetLine(p), area).joinToString(" • ")
    }

    private fun streetLine(p: JSONObject): String? =
        p.text("street")?.let { listOfNotNull(it, p.text("housenumber")).joinToString(" ") }

    private fun JSONObject.text(key: String): String? = optString(key, "").ifEmpty { null }

    /**
     * Snapshots a search result for storing on a task. [name] is the label shown in the picker,
     * which may be a localized fallback when the result has no usable name.
     */
    fun toPlace(feature: JSONObject, name: String): TaskPlace {
        val properties = feature.optJSONObject("properties") ?: JSONObject()
        // GeoJSON stores [longitude, latitude].
        val coordinates = feature.optJSONObject("geometry")?.optJSONArray("coordinates")
            ?.takeIf { it.length() >= 2 }
        return TaskPlace(
            name = name,
            details = detailsString(properties),
            country = properties.optString("country", ""),
            // A broken endpoint can send null or "1e999"; JSON can't store NaN or Infinity later.
            latitude = coordinates?.optDouble(1)?.takeIf { it.isFinite() },
            longitude = coordinates?.optDouble(0)?.takeIf { it.isFinite() },
        )
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
