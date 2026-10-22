package io.github.greavemrsvg.dogwalkbuddy.domain.check

import kotlin.math.abs
import kotlin.math.floor

/**
 * Whole-number display of the factor values (AC17, owner decisions "Whole numbers" and "Away from zero"). The shown
 * number is informational only: ratings always use the unrounded values.
 */
object FactorRounding {
    private const val HALF = 0.5

    /**
     * Rounds half away from zero: 14.5 gives 15, -2.5 gives -3 and -2.4 gives -2. A value that rounds to zero is 0,
     * never -0, so -0.4 shows "0°C". `kotlin.math.round` (half to even) and `Math.round` (-2.5 to -2) are not used.
     */
    fun wholeNumber(value: Double): Long {
        val size = abs(value)
        val whole = floor(size)
        // The fractional part is compared, not `floor(size + 0.5)`: adding 0.5 to 0.49999999999999994 rounds up to 1.0
        // in floating point. `size - whole` is exact, so only a true half or more rounds up.
        val magnitude = (if (size - whole >= HALF) whole + 1 else whole).toLong()
        return if (value < 0) -magnitude else magnitude
    }
}
