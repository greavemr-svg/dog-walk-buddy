package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a walk is in its life. Stored as the enum name (ADR 0004). */
enum class WalkStatus { RECORDING, COMPLETE }

/**
 * One walk (ADR 0004). Times are UTC epoch milliseconds and the distance is in metres. [endUtcMs] is null while
 * the walk is being recorded. [lastFixUtcMs] is the time of the last stored fix, which recovery uses (AC34).
 */
@Entity(
    tableName = WalkEntity.TABLE_NAME,
    indices = [Index(value = ["status", "start_utc_ms"])],
)
data class WalkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_utc_ms") val startUtcMs: Long,
    @ColumnInfo(name = "end_utc_ms") val endUtcMs: Long? = null,
    val status: WalkStatus,
    @ColumnInfo(name = "distance_m", defaultValue = "0") val distanceM: Double = 0.0,
    @ColumnInfo(name = "last_fix_utc_ms") val lastFixUtcMs: Long? = null,
) {
    companion object {
        const val TABLE_NAME = "walk"
    }
}
