package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stored GPS fix of a walk (ADR 0004). Route points are personal data: they never leave the device and are
 * never logged. Deleting the walk removes its points through the foreign key cascade.
 */
@Entity(
    tableName = RoutePointEntity.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = WalkEntity::class,
            parentColumns = ["id"],
            childColumns = ["walk_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["walk_id", "seq"])],
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "walk_id") val walkId: Long,
    val seq: Int,
    val lat: Double,
    val lon: Double,
    @ColumnInfo(name = "accuracy_m") val accuracyM: Double,
    @ColumnInfo(name = "time_utc_ms") val timeUtcMs: Long,
) {
    companion object {
        const val TABLE_NAME = "route_point"
    }
}
