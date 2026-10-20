package io.github.greavemrsvg.dogwalkbuddy.testutil

import java.time.Duration
import java.time.Instant

/** Builds small MET Locationforecast bodies with chosen values, so a test controls exactly what is rated. */
object MetBodies {
    /**
     * [hours] hourly periods from [start]. Every hour has temperature [temperatureC], [windMetresPerSecond] mean wind,
     * [cloudPercent] cloud cover and [precipitationMmPerHour] rain. A null leaves that value out of the body.
     */
    fun hourly(
        start: Instant,
        temperatureC: Double? = 15.0,
        windMetresPerSecond: Double? = 2.0,
        cloudPercent: Double? = 100.0,
        precipitationMmPerHour: Double? = 0.0,
        hours: Int = 12,
    ): String {
        val periods =
            (0 until hours).joinToString(",") { index ->
                val instant =
                    listOfNotNull(
                        temperatureC?.let { "\"air_temperature\":$it" },
                        windMetresPerSecond?.let { "\"wind_speed\":$it" },
                        cloudPercent?.let { "\"cloud_area_fraction\":$it" },
                    ).joinToString(",")
                val next = precipitationMmPerHour?.let { "\"next_1_hours\":{\"details\":{\"precipitation_amount\":$it}}" }
                val data = listOfNotNull("\"instant\":{\"details\":{$instant}}", next).joinToString(",")
                "{\"time\":\"${start.plus(Duration.ofHours(index.toLong()))}\",\"data\":{$data}}"
            }
        return "{\"properties\":{\"timeseries\":[$periods]}}"
    }
}
