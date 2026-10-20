package io.github.greavemrsvg.dogwalkbuddy.data.weather

import java.math.BigDecimal
import java.math.RoundingMode

/** Rounds coordinates to [WeatherConfig.COORDINATE_DECIMALS] decimals, half away from zero (ADR 0002). */
object CoordinateRounding {
    fun round(degrees: Double): Double = toDecimal(degrees).toDouble()

    /** Plain decimal text with a dot separator (BigDecimal ignores the device locale), for example "-0.13". */
    fun format(degrees: Double): String = toDecimal(degrees).toPlainString()

    private fun toDecimal(degrees: Double): BigDecimal {
        require(degrees.isFinite()) { "Coordinate must be finite" }
        // BigDecimal.valueOf uses the shortest decimal text of the double, so 51.505 is not seen as 51.50499999.
        return BigDecimal.valueOf(degrees).setScale(WeatherConfig.COORDINATE_DECIMALS, RoundingMode.HALF_UP)
    }
}
