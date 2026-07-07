package io.github.benji377.timety.util.location

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LocationServerException(val code: Int) : Exception("Server error: $code")

/**
 * Photon-compatible forward-geocoding client, shared by the location picker and the
 * task detail place-info view.
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

    fun primaryName(p: JSONObject): String {
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

    fun detailsString(p: JSONObject): String {
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

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
