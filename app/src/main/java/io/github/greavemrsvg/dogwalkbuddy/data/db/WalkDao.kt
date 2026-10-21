package io.github.greavemrsvg.dogwalkbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WalkDao {
    @Insert
    abstract suspend fun insertWalk(walk: WalkEntity): Long

    @Insert
    abstract suspend fun insertPoint(point: RoutePointEntity)

    @Query("UPDATE walk SET distance_m = :distanceM, last_fix_utc_ms = :lastFixUtcMs WHERE id = :walkId AND status = 'RECORDING'")
    abstract suspend fun updateProgress(
        walkId: Long,
        distanceM: Double,
        lastFixUtcMs: Long,
    ): Int

    /**
     * Stores one accepted fix and the walk's new distance and last fix time in one transaction, so a process kill
     * loses at most the fix in flight (ADR 0004, AC34). A walk that is no longer recording takes no more fixes.
     */
    @Transaction
    open suspend fun appendFix(
        point: RoutePointEntity,
        distanceM: Double,
    ): Boolean {
        if (updateProgress(point.walkId, distanceM, point.timeUtcMs) == 0) return false
        insertPoint(point)
        return true
    }

    @Query("UPDATE walk SET status = 'COMPLETE', end_utc_ms = :endUtcMs WHERE id = :walkId AND status = 'RECORDING'")
    abstract suspend fun complete(
        walkId: Long,
        endUtcMs: Long,
    ): Int

    @Query("SELECT * FROM walk WHERE id = :walkId")
    abstract suspend fun get(walkId: Long): WalkEntity?

    @Query("SELECT * FROM walk WHERE status = 'RECORDING' ORDER BY start_utc_ms")
    abstract suspend fun recording(): List<WalkEntity>

    @Query("SELECT * FROM route_point WHERE walk_id = :walkId ORDER BY seq")
    abstract suspend fun points(walkId: Long): List<RoutePointEntity>

    @Query("SELECT COALESCE(MAX(seq) + 1, 0) FROM route_point WHERE walk_id = :walkId")
    abstract suspend fun nextSeq(walkId: Long): Int

    /** Finished walks, newest first. Emits again whenever a walk is added or removed. */
    @Query(
        "SELECT id, start_utc_ms, end_utc_ms, distance_m FROM walk " +
            "WHERE status = 'COMPLETE' AND end_utc_ms IS NOT NULL ORDER BY start_utc_ms DESC, id DESC",
    )
    abstract fun observeCompleted(): Flow<List<WalkSummaryRow>>

    @Query("DELETE FROM walk WHERE id = :walkId")
    abstract suspend fun delete(walkId: Long): Int
}
