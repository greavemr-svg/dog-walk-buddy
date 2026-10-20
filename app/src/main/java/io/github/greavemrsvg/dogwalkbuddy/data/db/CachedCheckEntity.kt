package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The last successful check (ADR 0004). A single row. It holds the validated raw conditions, not the rating,
 * so a heat sensitivity change can re-rate without a network request.
 */
@Entity(tableName = CachedCheckEntity.TABLE_NAME)
data class CachedCheckEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "fetched_utc_ms") val fetchedUtcMs: Long,
    /** Rounded coordinates, the only location ever stored for the check. */
    val lat: Double,
    val lon: Double,
    @ColumnInfo(name = "hours_json") val hoursJson: String,
) {
    companion object {
        const val TABLE_NAME = "cached_check"
        const val SINGLE_ROW_ID = 1
    }
}
