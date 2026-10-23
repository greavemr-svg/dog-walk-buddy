package io.github.greavemrsvg.dogwalkbuddy.ui.history

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The dates and times of the Walks tab and Walk detail (D7, D8, AC39, AC46), in the device zone and in British
 * English. A date or week range in the current year has no year; one in another year shows it, and a week that spans
 * two years shows both. The current year is the year of [clock]'s local date in [zoneId].
 *
 * [rangeTemplate] joins the two ends of a range, for example "%1$s – %2$s" (the `history_week_range_span` string).
 * The formatters are built once, so build one of these per screen, not per row.
 */
class WalkDateText(
    private val zoneId: ZoneId,
    private val clock: Clock,
    is24Hour: Boolean,
    private val rangeTemplate: String,
) {
    private val dayOfMonth = DateTimeFormatter.ofPattern("EEE d", Locale.UK)
    private val dayMonth = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    private val dayMonthYear = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
    private val fullDayMonth = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)
    private val fullDayMonthYear = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.UK)
    private val timeFormat = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)

    private fun currentYear(): Int = LocalDate.now(clock.withZone(zoneId)).year

    private fun localDate(instant: Instant): LocalDate = instant.atZone(zoneId).toLocalDate()

    /** A row date, "Mon 19 Oct", or "Mon 19 Oct 2026" outside the current year. */
    fun rowDate(instant: Instant): String {
        val date = localDate(instant)
        return (if (date.year == currentYear()) dayMonth else dayMonthYear).format(date)
    }

    /** A heading date, "Monday 19 October", or "Monday 19 October 2026" outside the current year. */
    fun fullDate(instant: Instant): String {
        val date = localDate(instant)
        return (if (date.year == currentYear()) fullDayMonth else fullDayMonthYear).format(date)
    }

    /**
     * A week range: "Mon 19 – Sun 25 Oct" within a month, "Mon 26 Oct – Sun 1 Nov" across months, with the year added
     * when the week is not in the current year, and "Mon 28 Dec 2026 – Sun 3 Jan 2027" across years.
     */
    fun weekRange(
        start: LocalDate,
        end: LocalDate,
    ): String {
        val (from, to) =
            when {
                start.year != end.year -> dayMonthYear.format(start) to dayMonthYear.format(end)
                start.month == end.month -> dayOfMonth.format(start) to endOfRange(end)
                else -> dayMonth.format(start) to endOfRange(end)
            }
        return rangeTemplate.format(from, to)
    }

    private fun endOfRange(end: LocalDate): String = (if (end.year == currentYear()) dayMonth else dayMonthYear).format(end)

    /** A start time in the device's 12-hour or 24-hour format, "09:15" or "9:15 am". */
    fun time(instant: Instant): String = timeFormat.format(instant.atZone(zoneId))

    /** A time range, "09:15 – 10:30", joined by [rangeTemplate] (C14). */
    fun timeRange(
        start: Instant,
        end: Instant,
    ): String = rangeTemplate.format(time(start), time(end))
}
