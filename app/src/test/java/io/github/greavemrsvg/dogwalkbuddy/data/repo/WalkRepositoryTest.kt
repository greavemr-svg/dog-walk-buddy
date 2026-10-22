package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WalkRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = Instant.parse("2026-10-19T09:00:00Z")
    private val clock = MutableClock(start)
    private lateinit var database: AppDatabase
    private lateinit var repository: WalkRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = WalkRepository(database.walkDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun fix(seconds: Long) = LocationFix(51.5, -0.12, 5.0, start.plusSeconds(seconds))

    @Test
    fun AC28_starting_a_walk_stores_a_recording_row_with_the_clock_time() =
        runBlocking {
            val walk = repository.startWalk()

            val row = repository.walk(walk.id)!!
            assertEquals(WalkStatus.RECORDING, row.status)
            assertEquals(start.toEpochMilli(), row.startUtcMs)
            assertEquals(start, walk.startUtc)
            assertNull(row.endUtcMs)
            assertEquals(0.0, row.distanceM, 0.0)
            assertNull(row.lastFixUtcMs)
        }

    @Test
    fun AC34_a_stored_fix_updates_the_points_distance_and_last_fix_together() =
        runBlocking {
            val walk = repository.startWalk()

            assertTrue(repository.recordFix(walk.id, seq = 0, fix = fix(3), distanceM = 12.5))

            val row = repository.walk(walk.id)!!
            assertEquals(12.5, row.distanceM, 0.0)
            assertEquals(fix(3).timeUtc.toEpochMilli(), row.lastFixUtcMs)
            val points = repository.routePoints(walk.id)
            assertEquals(1, points.size)
            assertEquals(51.5, points[0].lat, 0.0)
            assertEquals(5.0, points[0].accuracyM, 0.0)
            assertEquals(1, repository.nextSeq(walk.id))
        }

    @Test
    fun a_fix_with_no_accuracy_is_stored_with_zero_accuracy() =
        runBlocking {
            val walk = repository.startWalk()

            repository.recordFix(walk.id, 0, LocationFix(51.5, -0.12, null, start.plusSeconds(1)), 0.0)

            assertEquals(0.0, repository.routePoints(walk.id).single().accuracyM, 0.0)
        }

    @Test
    fun AC30_finishing_a_walk_sets_the_end_time_and_the_complete_status() =
        runBlocking {
            val walk = repository.startWalk()
            clock.advance(Duration.ofMinutes(30))

            repository.finishWalk(walk.id)

            val row = repository.walk(walk.id)!!
            assertEquals(WalkStatus.COMPLETE, row.status)
            assertEquals(start.plus(Duration.ofMinutes(30)).toEpochMilli(), row.endUtcMs)
        }

    @Test
    fun a_walk_that_is_complete_takes_no_more_fixes_and_keeps_its_end_time() =
        runBlocking {
            val walk = repository.startWalk()
            repository.recordFix(walk.id, 0, fix(3), 5.0)
            clock.advance(Duration.ofMinutes(1))
            repository.finishWalk(walk.id)
            val end = repository.walk(walk.id)!!.endUtcMs

            assertFalse(repository.recordFix(walk.id, 1, fix(6), 99.0))
            clock.advance(Duration.ofMinutes(1))
            repository.finishWalk(walk.id)

            assertEquals(1, repository.routePoints(walk.id).size)
            assertEquals(5.0, repository.walk(walk.id)!!.distanceM, 0.0)
            assertEquals(end, repository.walk(walk.id)!!.endUtcMs)
        }

    @Test
    fun discarding_a_walk_removes_it_and_its_points() =
        runBlocking {
            val walk = repository.startWalk()
            repository.recordFix(walk.id, 0, fix(3), 5.0)

            repository.discard(walk.id)

            assertNull(repository.walk(walk.id))
            assertTrue(repository.routePoints(walk.id).isEmpty())
        }

    @Test
    fun the_recording_walks_are_listed_and_a_missing_walk_is_null() =
        runBlocking {
            val walk = repository.startWalk()

            assertEquals(listOf(walk.id), repository.recordingWalks().map { it.id })
            assertNotNull(repository.walk(walk.id))
            assertNull(repository.walk(walk.id + 100))
        }
}
