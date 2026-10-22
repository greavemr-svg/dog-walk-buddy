package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.PavementFactor
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason

/** The four cells of the Today factor grid, in grid order (AC17). */
enum class Factor { AIR, RAIN, WIND, PAVEMENT }

/** What one factor cell shows. The screen turns it into the exact D3 text. */
sealed interface FactorValue {
    /** Air in °C, Rain in mm/h or Wind in mph, already rounded to a whole number. */
    data class Whole(
        val value: Long,
    ) : FactorValue

    /** The pavement factor's own rating: "Cool", "Warm" or "Hot". */
    data class Pavement(
        val rating: Rating,
    ) : FactorValue

    /** The pavement rule does not apply. */
    data object NotRated : FactorValue

    /** The cell's input value is missing. */
    data object NoData : FactorValue
}

/** One factor cell. [highlighted] marks the factor the reason names. */
data class FactorCell(
    val factor: Factor,
    val value: FactorValue,
    val highlighted: Boolean,
)

/** Builds the factor grid for the current hour (AC17). */
object FactorGrid {
    /** The factor the reason names, or null for "Good conditions" and "Some weather data isn't available". */
    fun driverOf(reason: Reason): Factor? =
        when (reason) {
            Reason.WARM, Reason.TOO_HOT, Reason.FREEZING -> Factor.AIR
            Reason.RAIN, Reason.HEAVY_RAIN -> Factor.RAIN
            Reason.WINDY, Reason.DANGEROUS_WINDS -> Factor.WIND
            Reason.PAVEMENT_WARM, Reason.PAVEMENT_TOO_HOT -> Factor.PAVEMENT
            Reason.GOOD, Reason.DATA_MISSING -> null
        }

    /** The four cells, in grid order, with the cell the reason names highlighted. */
    fun cells(
        conditions: HourlyConditions,
        pavement: PavementFactor,
        reason: Reason,
    ): List<FactorCell> {
        val driver = driverOf(reason)
        return Factor.entries.map { factor ->
            val value =
                when (factor) {
                    Factor.AIR -> whole(conditions.temperatureC)
                    Factor.RAIN -> whole(conditions.precipitationMmPerHour)
                    Factor.WIND -> whole(conditions.windMeanMph)
                    Factor.PAVEMENT -> pavementValue(pavement)
                }
            FactorCell(factor, value, highlighted = factor == driver)
        }
    }

    private fun pavementValue(pavement: PavementFactor): FactorValue =
        when (pavement) {
            PavementFactor.NotRated -> FactorValue.NotRated
            PavementFactor.NoData -> FactorValue.NoData
            is PavementFactor.Rated -> FactorValue.Pavement(pavement.rating)
        }

    private fun whole(value: Double?): FactorValue =
        if (value == null) FactorValue.NoData else FactorValue.Whole(FactorRounding.wholeNumber(value))
}
