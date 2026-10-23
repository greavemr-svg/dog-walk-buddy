package io.github.greavemrsvg.dogwalkbuddy.domain.rating

/**
 * Rates one forecast hour as Go, Caution or Avoid, following the binding rules in the spec
 * ("Rating rules"). Pure Kotlin: no Android types, no clock.
 */
object RatingEngine {
    private const val HEAT_CAUTION_FROM_C = 20.0
    private const val HEAT_AVOID_FROM_C = 25.0
    private const val SUNNY_CLOUD_BELOW_PERCENT = 50.0
    private const val RAIN_CAUTION_FROM_MM_H = 0.5
    private const val RAIN_AVOID_ABOVE_MM_H = 4.0
    private const val WIND_CAUTION_FROM_MPH = 25.0
    private const val WIND_AVOID_FROM_MPH = 39.0
    private const val COLD_CAUTION_AT_OR_BELOW_C = 0.0

    /** A single factor's result. A null reason means the factor is Go. */
    private data class FactorResult(
        val rating: Rating,
        val reason: Reason?,
    )

    private val goFactor = FactorResult(Rating.GO, null)

    /**
     * True when temperature, precipitation and mean wind are all missing, so no honest result
     * exists and the check must show "Can't check right now" (AC15).
     */
    fun hasNoCoreData(hour: HourlyConditions): Boolean =
        hour.temperatureC == null && hour.precipitationMmPerHour == null && hour.windMeanMph == null

    /**
     * Rates [hour]. [isDaylight] is true between local sunrise and sunset. An hour with any of
     * temperature, precipitation or mean wind missing is never Go (AC13).
     */
    fun rate(
        hour: HourlyConditions,
        sensitivity: HeatSensitivity,
        isDaylight: Boolean,
    ): HourRating {
        val s = sensitivity.offsetC
        val temperature = hour.temperatureC
        val cloud = hour.cloudCoverPercent
        val pavementApplies = isDaylight && cloud != null && cloud < SUNNY_CLOUD_BELOW_PERCENT

        // Order is the tie-break precedence: pavement, heat, wind, rain, cold.
        val factors =
            listOf(
                if (pavementApplies) {
                    heat(temperature, s, Reason.PAVEMENT_WARM, Reason.PAVEMENT_TOO_HOT)
                } else {
                    goFactor
                },
                heat(temperature, s, Reason.WARM, Reason.TOO_HOT),
                wind(hour.windMeanMph),
                rain(hour.precipitationMmPerHour),
                cold(temperature),
            )

        // maxByOrNull returns the first of equally severe factors, which is the precedence order.
        val worst = factors.maxByOrNull { it.rating.ordinal } ?: goFactor
        val reason = worst.reason
        if (reason != null) return HourRating(hour.startUtc, worst.rating, reason)

        val anyMissing =
            temperature == null || hour.precipitationMmPerHour == null || hour.windMeanMph == null
        return if (anyMissing) {
            HourRating(hour.startUtc, Rating.CAUTION, Reason.DATA_MISSING)
        } else {
            HourRating(hour.startUtc, Rating.GO, Reason.GOOD)
        }
    }

    /**
     * The pavement factor on its own, for the Today factor grid (AC17). It uses the same rule and thresholds as the
     * pavement part of [rate], which it does not change: [PavementFactor.NotRated] at night, or when cloud cover is
     * 50 % or more or missing; [PavementFactor.NoData] when the rule applies but the temperature is missing.
     */
    fun pavementFactor(
        hour: HourlyConditions,
        sensitivity: HeatSensitivity,
        isDaylight: Boolean,
    ): PavementFactor {
        val cloud = hour.cloudCoverPercent
        if (!isDaylight || cloud == null || cloud >= SUNNY_CLOUD_BELOW_PERCENT) return PavementFactor.NotRated
        val temperature = hour.temperatureC ?: return PavementFactor.NoData
        val factor = heat(temperature, sensitivity.offsetC, Reason.PAVEMENT_WARM, Reason.PAVEMENT_TOO_HOT)
        return PavementFactor.Rated(factor.rating)
    }

    private fun heat(
        temperature: Double?,
        s: Double,
        cautionReason: Reason,
        avoidReason: Reason,
    ): FactorResult =
        when {
            temperature == null -> goFactor
            temperature >= HEAT_AVOID_FROM_C - s -> FactorResult(Rating.AVOID, avoidReason)
            temperature >= HEAT_CAUTION_FROM_C - s -> FactorResult(Rating.CAUTION, cautionReason)
            else -> goFactor
        }

    private fun wind(meanMph: Double?): FactorResult =
        when {
            meanMph == null -> goFactor
            meanMph >= WIND_AVOID_FROM_MPH -> FactorResult(Rating.AVOID, Reason.DANGEROUS_WINDS)
            meanMph >= WIND_CAUTION_FROM_MPH -> FactorResult(Rating.CAUTION, Reason.WINDY)
            else -> goFactor
        }

    private fun rain(mmPerHour: Double?): FactorResult =
        when {
            mmPerHour == null -> goFactor
            mmPerHour > RAIN_AVOID_ABOVE_MM_H -> FactorResult(Rating.AVOID, Reason.HEAVY_RAIN)
            mmPerHour >= RAIN_CAUTION_FROM_MM_H -> FactorResult(Rating.CAUTION, Reason.RAIN)
            else -> goFactor
        }

    private fun cold(temperature: Double?): FactorResult =
        if (temperature != null && temperature <= COLD_CAUTION_AT_OR_BELOW_C) {
            FactorResult(Rating.CAUTION, Reason.FREEZING)
        } else {
            goFactor
        }
}

/** The pavement factor of one hour, as the Today factor grid shows it (AC17). */
sealed interface PavementFactor {
    /** The pavement rule does not apply: night, cloud cover 50 % or more, or cloud cover missing. */
    data object NotRated : PavementFactor

    /** The rule applies but the temperature is missing. */
    data object NoData : PavementFactor

    /** The pavement factor's own rating, by the same thresholds as heat. */
    data class Rated(
        val rating: Rating,
    ) : PavementFactor
}
