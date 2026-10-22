package io.github.greavemrsvg.dogwalkbuddy.domain.sun

import java.time.Instant
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise and sunset from the NOAA solar position equations (accurate to about a minute at UK
 * latitudes). Pure Kotlin, no clock. The events are computed for the UTC calendar day of the
 * given instant, which is right for the UK where both events fall on the same UTC day.
 *
 * Daylight is evaluated at the instant given. The rating uses the start of each hourly period,
 * so an hour that straddles sunrise or sunset takes the state at its start.
 */
object SunCalculator {
    private const val UNIX_EPOCH_JULIAN_DAY = 2440587.5
    private const val J2000_JULIAN_DAY = 2451545.0
    private const val DAYS_PER_CENTURY = 36525.0
    private const val SOLAR_NOON_UTC_MINUTES = 720.0
    private const val MINUTES_PER_DEGREE = 4.0
    private const val SUNRISE_ZENITH_DEGREES = 90.833
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val MILLIS_PER_DAY = 86_400_000L

    /** Sunrise and sunset. Both are null when the sun never rises or never sets that day. */
    data class SunTimes(
        val sunrise: Instant?,
        val sunset: Instant?,
        val sunAlwaysUp: Boolean,
    )

    /** True when [instant] is between sunrise and sunset at the given location. */
    fun isDaylight(
        instant: Instant,
        latitude: Double,
        longitude: Double,
    ): Boolean {
        val times = sunTimes(instant, latitude, longitude)
        val sunrise = times.sunrise
        val sunset = times.sunset
        if (sunrise == null || sunset == null) return times.sunAlwaysUp
        return !instant.isBefore(sunrise) && instant.isBefore(sunset)
    }

    /** Sunrise and sunset for the UTC day containing [instant]. */
    @Suppress("LongMethod")
    fun sunTimes(
        instant: Instant,
        latitude: Double,
        longitude: Double,
    ): SunTimes {
        val epochDay = Math.floorDiv(instant.toEpochMilli(), MILLIS_PER_DAY)
        val dayStartMillis = epochDay * MILLIS_PER_DAY
        // Julian day at local solar noon, where the equations are evaluated.
        val julianDay = epochDay + UNIX_EPOCH_JULIAN_DAY + 0.5 - longitude / 360.0
        val t = (julianDay - J2000_JULIAN_DAY) / DAYS_PER_CENTURY

        val meanLongitude = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0).toRad()
        val meanAnomaly = (357.52911 + t * (35999.05029 - 0.0001537 * t)).toRad()
        val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val equationOfCentre =
            sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) +
                sin(3 * meanAnomaly) * 0.000289
        val trueLongitude = meanLongitude.toDeg() + equationOfCentre
        val omega = (125.04 - 1934.136 * t).toRad()
        val apparentLongitude = (trueLongitude - 0.00569 - 0.00478 * sin(omega)).toRad()
        val meanObliquity =
            23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val obliquity = (meanObliquity + 0.00256 * cos(omega)).toRad()
        val declination = asin(sin(obliquity) * sin(apparentLongitude))

        val halfTan = tan(obliquity / 2)
        val y = halfTan * halfTan
        val equationOfTimeMinutes =
            MINUTES_PER_DEGREE *
                (
                    y * sin(2 * meanLongitude) -
                        2 * eccentricity * sin(meanAnomaly) +
                        4 * eccentricity * y * sin(meanAnomaly) * cos(2 * meanLongitude) -
                        0.5 * y * y * sin(4 * meanLongitude) -
                        1.25 * eccentricity * eccentricity * sin(2 * meanAnomaly)
                ).toDeg()

        val lat = latitude.toRad()
        val cosHourAngle =
            cos(SUNRISE_ZENITH_DEGREES.toRad()) / (cos(lat) * cos(declination)) -
                tan(lat) * tan(declination)
        if (cosHourAngle > 1.0) return SunTimes(null, null, sunAlwaysUp = false)
        if (cosHourAngle < -1.0) return SunTimes(null, null, sunAlwaysUp = true)

        val hourAngleDegrees = acos(cosHourAngle).toDeg()
        val noonMinutes = SOLAR_NOON_UTC_MINUTES - MINUTES_PER_DEGREE * longitude - equationOfTimeMinutes
        val sunriseMinutes = noonMinutes - MINUTES_PER_DEGREE * hourAngleDegrees
        val sunsetMinutes = noonMinutes + MINUTES_PER_DEGREE * hourAngleDegrees
        return SunTimes(
            sunrise = Instant.ofEpochMilli(dayStartMillis + (sunriseMinutes * MILLIS_PER_MINUTE).toLong()),
            sunset = Instant.ofEpochMilli(dayStartMillis + (sunsetMinutes * MILLIS_PER_MINUTE).toLong()),
            sunAlwaysUp = false,
        )
    }

    private fun Double.toRad(): Double = this * PI / 180.0

    private fun Double.toDeg(): Double = this * 180.0 / PI
}
