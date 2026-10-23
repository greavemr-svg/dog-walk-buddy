package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.res.Resources
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The words shown for the best window. [label] is non-null only for "Best available: Caution". */
data class BestWindowText(
    val label: String?,
    val text: String,
)

/**
 * Turns a [BestWindow] into display text, using the device's 12 or 24 hour format. The zone and
 * the format flag are injected so the output is deterministic in tests.
 */
class BestWindowFormatter(
    private val resources: Resources,
    private val zone: ZoneId,
    private val is24HourFormat: () -> Boolean,
) {
    fun format(window: BestWindow): BestWindowText =
        when (window) {
            BestWindow.NoGoodTime -> BestWindowText(null, resources.getString(R.string.best_window_none))
            is BestWindow.Window -> {
                val end = time(window.endUtc)
                val text =
                    if (window.startsNow) {
                        resources.getString(R.string.best_window_now_until, end)
                    } else {
                        resources.getString(R.string.best_window_range, time(window.startUtc), end)
                    }
                val label =
                    if (window.rating == Rating.CAUTION) {
                        resources.getString(R.string.best_window_best_available_caution)
                    } else {
                        null
                    }
                BestWindowText(label, text)
            }
        }

    /** The strip's hour labels, in the same zone and the same device time format as the window text (AC23). */
    fun hourLabelFormat(): HourLabelFormat = HourLabelFormat(zone, is24HourFormat())

    private fun time(instant: Instant): String {
        val pattern = if (is24HourFormat()) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, Locale.UK).withZone(zone).format(instant)
    }
}
