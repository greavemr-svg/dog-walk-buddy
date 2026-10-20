package io.github.greavemrsvg.dogwalkbuddy.data.weather

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.Duration

/** Constants for the MET Norway Locationforecast 2.0 client (ADR 0002). */
object WeatherConfig {
    /** Production base URL. Tests inject a stub server URL instead. */
    val productionBaseUrl: HttpUrl = "https://api.met.no/weatherapi/locationforecast/2.0/".toHttpUrl()

    const val ENDPOINT_COMPLETE = "complete"
    const val QUERY_LAT = "lat"
    const val QUERY_LON = "lon"

    /** Contact URL for the identifying User-Agent that MET Norway requires. No personal email. */
    const val CONTACT_URL = "https://github.com/greavemr-svg/dog-walk-buddy"
    const val USER_AGENT_PRODUCT = "DogWalkBuddy"

    /** Coordinates are rounded to this many decimals (about 1 km) before they leave the device. */
    const val COORDINATE_DECIMALS = 2

    /** Responses larger than this are rejected (security standards, input validation). */
    const val MAX_RESPONSE_BYTES: Long = 2L * 1024 * 1024

    const val HTTP_CACHE_DIRECTORY = "http-weather"
    const val HTTP_CACHE_BYTES: Long = 2L * 1024 * 1024

    val callTimeout: Duration = Duration.ofSeconds(10)

    const val HTTP_OK = 200

    /** MET answers 203 for a deprecated product. The data is still valid. */
    const val HTTP_NON_AUTHORITATIVE = 203

    /** Hours kept from a response. The check uses 12, the rest gives headroom for a stale-hour shift. */
    const val MAX_PERIODS_KEPT = 48

    fun userAgent(versionName: String): String = "$USER_AGENT_PRODUCT/$versionName (+$CONTACT_URL)"
}
