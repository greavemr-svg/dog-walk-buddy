package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/** AC34: a walk cut short by a process kill is saved on the next launch, ending at its last recorded fix. */
@RunWith(AndroidJUnit4::class)
class RecoverInterruptedWalkUseCaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = Instant.parse("2026-10-19T09:00:00Z")
    private val clock = MutableClock(start)
    private val databaseName = "recovery_test.db"
    private var database: AppDatabase? = null

    private fun open(): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            // Robolectric's native SQLite cannot reopen a write-ahead-log file on Windows. The rollback journal
            // behaves the same for what is tested here: rows written before the close are there after the reopen.
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
            .also { database = it }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    private fun fix(
        seconds: Long,
        lat: Double,
    ) = LocationFix(lat, -0.12, 5.0, start.plusSeconds(seconds))

    @Test
    fun AC34_a_killed_process_leaves_a_walk_that_is_saved_at_its_last_fix_with_its_distance() =
        runBlocking {
            val first = open()
            val writer = WalkRepository(first.walkDao(), clock)
            val walk = writer.startWalk()
            writer.recordFix(walk.id, 0, fix(3, 51.5), 0.0)
            writer.recordFix(walk.id, 1, fix(60, 51.501), 111.2)
            clock.advance(Duration.ofMinutes(20))
            // The process is killed: the database is closed with the walk still RECORDING, and the next launch opens it.
            first.close()

            val repository = WalkRepository(open().walkDao(), clock)
            val recovered = RecoverInterruptedWalkUseCase(repository) { false }()

            assertEquals(1, recovered)
            val row = repository.walk(walk.id)!!
            assertEquals(WalkStatus.COMPLETE, row.status)
            assertEquals(start.plusSeconds(60).toEpochMilli(), row.endUtcMs)
            assertEquals(111.2, row.distanceM, 0.0)
            assertEquals(2, repository.routePoints(walk.id).size)
        }

    @Test
    fun AC34_a_walk_with_no_fix_ends_at_its_start() =
        runBlocking {
            val repository = WalkRepository(open().walkDao(), clock)
            val walk = repository.startWalk()

            RecoverInterruptedWalkUseCase(repository) { false }()

            val row = repository.walk(walk.id)!!
            assertEquals(WalkStatus.COMPLETE, row.status)
            assertEquals(start.toEpochMilli(), row.endUtcMs)
        }

    @Test
    fun a_live_recording_is_left_alone() =
        runBlocking {
            val repository = WalkRepository(open().walkDao(), clock)
            val walk = repository.startWalk()

            val recovered = RecoverInterruptedWalkUseCase(repository) { true }()

            assertEquals(0, recovered)
            assertEquals(WalkStatus.RECORDING, repository.walk(walk.id)!!.status)
        }

    @Test
    fun walks_that_are_already_complete_are_not_touched() =
        runBlocking {
            val repository = WalkRepository(open().walkDao(), clock)
            val walk = repository.startWalk()
            clock.advance(Duration.ofMinutes(5))
            repository.finishWalk(walk.id)
            val end = repository.walk(walk.id)!!.endUtcMs

            assertEquals(0, RecoverInterruptedWalkUseCase(repository) { false }())

            assertEquals(end, repository.walk(walk.id)!!.endUtcMs)
        }
}
