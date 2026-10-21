package io.github.greavemrsvg.dogwalkbuddy.testutil

import io.github.greavemrsvg.dogwalkbuddy.data.location.WalkLocationSource
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.service.WalkServiceLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant
import kotlin.math.cos

/** A walk location source the test feeds by hand. */
class FakeWalkLocationSource : WalkLocationSource {
    private val flow = MutableSharedFlow<LocationFix>(extraBufferCapacity = 1024)

    /** How many collectors are listening, so a test can tell that updates are on or removed. */
    val subscriptions get() = flow.subscriptionCount.value

    override fun fixes(): Flow<LocationFix> = flow

    fun emit(fix: LocationFix) = check(flow.tryEmit(fix)) { "Fix was not delivered" }
}

/** Records the start and stop requests instead of starting a real service. */
class FakeWalkServiceLauncher : WalkServiceLauncher {
    val started = mutableListOf<Long>()
    var stopCount = 0
    var failStart = false

    override fun start(walkId: Long) {
        if (failStart) throw IllegalStateException("start refused")
        started += walkId
    }

    override fun stop() {
        stopCount++
    }
}

/** Synthetic walks for tests: every point is generated, none is a real place. */
object SyntheticTrack {
    private const val METRES_PER_DEGREE_LAT = 111_194.9266

    /**
     * A straight track heading north from a fictional start, [fixes] points [stepM] metres apart, one every 3 s.
     * A small deterministic wobble (at most [noiseM]) is added across the track.
     */
    fun north(
        fixes: Int,
        stepM: Double,
        accuracyM: Double,
        start: Instant,
        noiseM: Double = 0.0,
    ): List<LocationFix> {
        val lat0 = 51.5
        val lon0 = -0.12
        val metresPerDegreeLon = METRES_PER_DEGREE_LAT * cos(Math.toRadians(lat0))
        return (0 until fixes).map { i ->
            // A fixed pattern of offsets, so runs are repeatable.
            val wobble = noiseM * WOBBLE[i % WOBBLE.size]
            LocationFix(
                latitude = lat0 + i * stepM / METRES_PER_DEGREE_LAT,
                longitude = lon0 + wobble / metresPerDegreeLon,
                accuracyM = accuracyM,
                timeUtc = start.plusSeconds(3L * (i + 1)),
            )
        }
    }

    private val WOBBLE = doubleArrayOf(0.0, 0.8, -0.5, 0.3, -0.9, 0.6, -0.2, 0.9, -0.7, 0.1)
}
