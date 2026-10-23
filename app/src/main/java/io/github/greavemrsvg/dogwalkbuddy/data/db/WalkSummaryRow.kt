package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.ColumnInfo

/** The columns of a finished walk that the history needs. It is a query result, not a table. */
data class WalkSummaryRow(
    val id: Long,
    @ColumnInfo(name = "start_utc_ms") val startUtcMs: Long,
    @ColumnInfo(name = "end_utc_ms") val endUtcMs: Long,
    @ColumnInfo(name = "distance_m") val distanceM: Double,
)
