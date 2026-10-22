package io.github.greavemrsvg.dogwalkbuddy.ui.check

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The hour labels under the 12-hour strip (AC23, owner decisions "Follow phone setting" and `"2pm"`): the start of
 * each hour in the device's time zone, as a 2-digit hour ("14", "00") in 24-hour mode, or as the hour with no
 * leading zero and no minutes followed directly by "am" or "pm" ("2pm", "12am", "12pm") in 12-hour mode. These
 * deliberately differ from the best-window text, which keeps its MVP form ("3:00 pm").
 */
class HourLabelFormat(
    zone: ZoneId,
    is24Hour: Boolean,
) {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (is24Hour) PATTERN_24_HOUR else PATTERN_12_HOUR, Locale.UK).withZone(zone)

    fun format(hourStart: Instant): String = formatter.format(hourStart)

    private companion object {
        const val PATTERN_24_HOUR = "HH"

        // Locale.UK gives lowercase "am" and "pm".
        const val PATTERN_12_HOUR = "ha"
    }
}
