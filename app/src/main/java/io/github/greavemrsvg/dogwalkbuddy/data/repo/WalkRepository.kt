package io.github.greavemrsvg.dogwalkbuddy.data.repo

import io.github.greavemrsvg.dogwalkbuddy.data.db.RoutePointEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import java.time.Clock
import java.time.Instant

/** Walk storage used by recording (ADR 0004, ADR 0005). Task 7 adds history reads and delete. */
class WalkRepository(
    private val dao: WalkDao,
    private val clock: Clock,
) {
    /** Inserts the `RECORDING` row with the start time from the clock and returns its id. */
    suspend fun startWalk(): Walk {
        val start = clock.instant()
        val id = dao.insertWalk(WalkEntity(startUtcMs = start.toEpochMilli(), status = WalkStatus.RECORDING))
        return Walk(id, start)
    }

    /** Stores one accepted fix and the new total distance in one transaction. False when the walk is not recording. */
    suspend fun recordFix(
        walkId: Long,
        seq: Int,
        fix: LocationFix,
        distanceM: Double,
    ): Boolean {
        val point =
            RoutePointEntity(
                walkId = walkId,
                seq = seq,
                lat = fix.latitude,
                lon = fix.longitude,
                accuracyM = fix.accuracyM ?: 0.0,
                timeUtcMs = fix.timeUtc.toEpochMilli(),
            )
        return dao.appendFix(point, distanceM)
    }

    /** Ends the walk now: `end_utc_ms` is the clock time and the status is `COMPLETE` (AC30). */
    suspend fun finishWalk(walkId: Long) {
        dao.complete(walkId, clock.instant().toEpochMilli())
    }

    /** Removes a walk that never started recording, together with its points. */
    suspend fun discard(walkId: Long) {
        dao.delete(walkId)
    }

    suspend fun nextSeq(walkId: Long): Int = dao.nextSeq(walkId)

    suspend fun walk(walkId: Long): WalkEntity? = dao.get(walkId)

    suspend fun routePoints(walkId: Long): List<RoutePointEntity> = dao.points(walkId)

    /** Walks still marked `RECORDING`: more than one only if an earlier recovery failed. */
    suspend fun recordingWalks(): List<WalkEntity> = dao.recording()

    /**
     * Completes an interrupted walk at its last recorded fix, or at its start when it has none (AC34). The
     * distance already holds the total up to the last fix.
     */
    suspend fun completeAtLastFix(walk: WalkEntity) {
        dao.complete(walk.id, walk.lastFixUtcMs ?: walk.startUtcMs)
    }

    /** A walk that has just been inserted. */
    data class Walk(
        val id: Long,
        val startUtc: Instant,
    )
}
