package io.github.benji377.timety.data.model.task

import org.json.JSONObject

/**
 * Snapshot of the place picked from a location search, stored on the task so its details can be
 * shown offline and always refer to the exact result the user chose, not a later lookup by name.
 */
data class TaskPlace(
    val name: String,
    val details: String = "",
    val country: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("details", details)
        put("country", country)
        put("latitude", latitude ?: JSONObject.NULL)
        put("longitude", longitude ?: JSONObject.NULL)
    }

    companion object {
        /** Parses [toJson] output; returns null when the object has no usable name. */
        fun fromJson(json: JSONObject): TaskPlace? {
            val name = json.optString("name", "")
            if (name.isEmpty()) return null
            return TaskPlace(
                name = name,
                details = json.optString("details", ""),
                country = json.optString("country", ""),
                latitude = json.optDoubleOrNull("latitude"),
                longitude = json.optDoubleOrNull("longitude"),
            )
        }

        private fun JSONObject.optDoubleOrNull(key: String): Double? =
            if (isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }
    }
}
