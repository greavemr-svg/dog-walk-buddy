package io.github.greavemrsvg.dogwalkbuddy.domain.rating

import java.time.Instant

/** Overall rating for an hour. Declared in increasing order of severity, so the ordinal gives the worst. */
enum class Rating { GO, CAUTION, AVOID }

/** Dog heat sensitivity. [offsetC] is "s" in the spec's rating rules. */
enum class HeatSensitivity(
    val offsetC: Double,
) {
    NORMAL(0.0),
    SENSITIVE(4.0),
}

/** The main reason shown with a rating. The exact texts live in `strings.xml`. */
enum class Reason {
    GOOD,
    WARM,
    TOO_HOT,
    PAVEMENT_WARM,
    PAVEMENT_TOO_HOT,
    RAIN,
    HEAVY_RAIN,
    WINDY,
    DANGEROUS_WINDS,
    FREEZING,
    DATA_MISSING,
}

/** Forecast values for one hour. A null value means the data is missing or was rejected as invalid. */
data class HourlyConditions(
    val startUtc: Instant,
    val temperatureC: Double?,
    val precipitationMmPerHour: Double?,
    val windMeanMph: Double?,
    val cloudCoverPercent: Double?,
)

data class HourRating(
    val startUtc: Instant,
    val rating: Rating,
    val reason: Reason,
)
