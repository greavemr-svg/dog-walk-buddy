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
