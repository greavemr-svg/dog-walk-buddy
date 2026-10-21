package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WalkHistoryRepositoryTest {
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

    private fun recordWalk(
        minutes: Long,
        distanceM: Double,
    ): Long =
        runBlocking {
            val walk = repository.startWalk()
            repository.recordFix(walk.id, 0, LocationFix(51.5, -0.12, 5.0, clock.instant()), distanceM)
            clock.advance(Duration.ofMinutes(minutes))
            repository.finishWalk(walk.id)
            walk.id
        }

    private fun summaries() = runBlocking { repository.observeSummaries().first() }

    @Test
    fun AC30_a_stopped_walk_appears_at_the_top_of_the_history_with_its_duration_and_distance() {
        val older = recordWalk(20, 500.0)
        clock.advance(Duration.ofHours(2))
        val newest = recordWalk(45, 1609.344)

        val list = summaries()

        assertEquals(listOf(newest, older), list.map { it.id })
        assertEquals(45 * 60L, list[0].durationSeconds)
        assertEquals(1609.344, list[0].distanceM, 0.0)
        assertEquals(20 * 60L, list[1].durationSeconds)
    }

    @Test
    fun AC35_a_walk_still_being_recorded_is_not_listed() {
        runBlocking { repository.startWalk() }

        assertTrue(summaries().isEmpty())
    }

    @Test
    fun AC40_deleting_a_walk_removes_it_and_its_route_points_and_keeps_the_others() {
        val first = recordWalk(10, 100.0)
        clock.advance(Duration.ofHours(1))
        val second = recordWalk(10, 200.0)

        runBlocking { repository.deleteWalk(first) }

        assertEquals(listOf(second), summaries().map { it.id })
        runBlocking {
            assertTrue(repository.routePoints(first).isEmpty())
            assertEquals(1, repository.routePoints(second).size)
            assertNull(repository.walk(first))
        }
    }

    @Test
    fun AC40_deleting_a_walk_that_does_not_exist_changes_nothing() {
        val id = recordWalk(10, 100.0)

        runBlocking { repository.deleteWalk(id + 99) }

        assertEquals(listOf(id), summaries().map { it.id })
    }
}
