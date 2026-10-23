package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** The walks of one Monday-to-Sunday week, newest first, with their totals. Totals are raw sums, formatted later. */
data class WeekGroup(
    val weekStart: LocalDate,
    val walks: List<WalkSummary>,
    val count: Int,
    val totalSeconds: Long,
    val totalM: Double,
) {
    /** The Sunday that ends the week. */
    val weekEnd: LocalDate get() = weekStart.plusDays(6)
}

/** How a day of the current week is drawn in the this-week card (AC41). */
enum class DayDotState {
    /** At least one walk started that day. */
    WALKED,

    /** Today or an earlier day of the week with no walk. */
    PAST_NO_WALK,

    /** A later day of the week. */
    FUTURE,
}

/** One day of the current Monday-to-Sunday week, for the this-week card's dot row (AC41). */
data class DayDot(
    val date: LocalDate,
    val state: DayDotState,
    val isToday: Boolean,
)

/** Groups walks by the week they started in (spec Assumption 8). Pure Kotlin. */
object WeeklyTotals {
    /** The Monday of the week that contains [instant] in [zoneId]. */
    fun weekStartOf(
        instant: Instant,
        zoneId: ZoneId,
    ): LocalDate = instant.atZone(zoneId).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** The weeks that have walks, newest week first. A walk counts in the week it started. */
    fun group(
        walks: List<WalkSummary>,
        zoneId: ZoneId,
    ): List<WeekGroup> =
        walks
            .sortedByDescending { it.startUtc }
            .groupBy { weekStartOf(it.startUtc, zoneId) }
            .map { (weekStart, inWeek) -> weekGroup(weekStart, inWeek) }
            .sortedByDescending { it.weekStart }

    /** The week that contains [now]. It is always present: with no walks it shows 0 walks, 0:00 and 0.0 mi (AC37). */
    fun thisWeek(
        walks: List<WalkSummary>,
        zoneId: ZoneId,
        now: Instant,
    ): WeekGroup {
        val weekStart = weekStartOf(now, zoneId)
        val inWeek = walks.filter { weekStartOf(it.startUtc, zoneId) == weekStart }.sortedByDescending { it.startUtc }
        return weekGroup(weekStart, inWeek)
    }

    /**
     * The seven days, Monday to Sunday, of the week that contains [now] (AC41). A day is walked when a walk started on
     * it in [zoneId]. Only walks in the current week are looked at.
     */
    fun dayDots(
        walks: List<WalkSummary>,
        zoneId: ZoneId,
        now: Instant,
    ): List<DayDot> {
        val today = now.atZone(zoneId).toLocalDate()
        val weekStart = weekStartOf(now, zoneId)
        val walkedDays =
            walks
                .map { it.startUtc.atZone(zoneId).toLocalDate() }
                .filter { !it.isBefore(weekStart) && it.isBefore(weekStart.plusDays(DAYS_IN_WEEK)) }
                .toSet()
        return (0 until DAYS_IN_WEEK).map { offset ->
            val date = weekStart.plusDays(offset)
            val state =
                when {
                    date in walkedDays -> DayDotState.WALKED
                    date.isAfter(today) -> DayDotState.FUTURE
                    else -> DayDotState.PAST_NO_WALK
                }
            DayDot(date = date, state = state, isToday = date == today)
        }
    }

    private const val DAYS_IN_WEEK = 7L

    private fun weekGroup(
        weekStart: LocalDate,
        walks: List<WalkSummary>,
    ) = WeekGroup(
        weekStart = weekStart,
        walks = walks,
        count = walks.size,
        totalSeconds = walks.sumOf { it.durationSeconds },
        totalM = walks.sumOf { it.distanceM },
    )
}
