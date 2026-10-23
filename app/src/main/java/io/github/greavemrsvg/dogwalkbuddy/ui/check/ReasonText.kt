package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.annotation.StringRes
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason

/** The string resource holding the exact spec text for each [Reason]. */
@StringRes
fun Reason.textRes(): Int =
    when (this) {
        Reason.GOOD -> R.string.reason_good
        Reason.WARM -> R.string.reason_warm
        Reason.TOO_HOT -> R.string.reason_too_hot
        Reason.PAVEMENT_WARM -> R.string.reason_pavement_warm
        Reason.PAVEMENT_TOO_HOT -> R.string.reason_pavement_too_hot
        Reason.RAIN -> R.string.reason_rain
        Reason.HEAVY_RAIN -> R.string.reason_heavy_rain
        Reason.WINDY -> R.string.reason_windy
        Reason.DANGEROUS_WINDS -> R.string.reason_dangerous_winds
        Reason.FREEZING -> R.string.reason_freezing
        Reason.DATA_MISSING -> R.string.reason_data_missing
    }
