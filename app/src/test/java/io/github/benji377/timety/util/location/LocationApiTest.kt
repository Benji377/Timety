package io.github.benji377.timety.util.location

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationApiTest {

    @Test
    fun primaryName_fallsBackFromNameToStreetToCityToState() {
        assertEquals("Café", LocationApi.primaryName(JSONObject("""{"name":"Café","street":"Main St"}""")))
        assertEquals("Main St 4", LocationApi.primaryName(JSONObject("""{"street":"Main St","housenumber":"4"}""")))
        assertEquals("Main St", LocationApi.primaryName(JSONObject("""{"street":"Main St"}""")))
        assertEquals("Bolzano", LocationApi.primaryName(JSONObject("""{"city":"Bolzano","state":"South Tyrol"}""")))
        assertEquals("South Tyrol", LocationApi.primaryName(JSONObject("""{"state":"South Tyrol"}""")))
        assertEquals("", LocationApi.primaryName(JSONObject("{}")))
    }

    @Test
    fun detailsString_joinsTypeStreetAndArea() {
        val p = JSONObject(
            """{"osm_value":"cafe","street":"Main St","housenumber":"4","town":"Merano","postcode":"39012"}"""
        )
        assertEquals("Cafe • Main St 4 • Merano, 39012", LocationApi.detailsString(p))
    }

    @Test
    fun detailsString_skipsGenericTypeAndLoneHouseNumber() {
        val p = JSONObject("""{"osm_value":"yes","housenumber":"4","state":"South Tyrol"}""")
        assertEquals("South Tyrol", LocationApi.detailsString(p))
        assertEquals("", LocationApi.detailsString(JSONObject("{}")))
    }

    @Test
    fun toPlace_readsGeoJsonCoordinatesAndDropsNonFiniteOnes() {
        val feature = JSONObject(
            """{"properties":{"country":"Italy","city":"Bolzano"},"geometry":{"coordinates":[11.35,46.5]}}"""
        )
        val place = LocationApi.toPlace(feature, "Home")
        assertEquals("Home", place.name)
        assertEquals("Bolzano", place.details)
        assertEquals("Italy", place.country)
        assertEquals(46.5, place.latitude!!, 0.0)
        assertEquals(11.35, place.longitude!!, 0.0)

        val broken = LocationApi.toPlace(JSONObject("""{"geometry":{"coordinates":[null,"1e999"]}}"""), "X")
        assertNull(broken.latitude)
        assertNull(broken.longitude)
    }
}
