package io.github.greavemrsvg.dogwalkbuddy.ui.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.RoutePointEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkSummaryRow
import io.github.greavemrsvg.dogwalkbuddy.data.repo.WalkRepository
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HistoryViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = MutableClock(Instant.parse("2026-10-21T10:00:00Z"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var database: AppDatabase
    private lateinit var repository: WalkRepository
    private lateinit var viewModel: HistoryViewModel

    @Volatile private var latest: HistoryUiState? = null

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = WalkRepository(database.walkDao(), clock)
        viewModel = HistoryViewModel(repository, clock, ZoneOffset.UTC)
        scope.launch { viewModel.state.collect { latest = it } }
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    private fun addWalk(start: String): Long =
        runBlocking {
            val startAt = Instant.parse(start)
            database.walkDao().insertWalk(
                WalkEntity(
                    startUtcMs = startAt.toEpochMilli(),
                    endUtcMs = startAt.plusSeconds(1800).toEpochMilli(),
                    status = WalkStatus.COMPLETE,
                    distanceM = 1609.344,
                ),
            )
        }

    private fun await(
        what: String,
        condition: (HistoryUiState) -> Boolean,
    ) = awaitCondition(what) { latest?.let(condition) == true }

    @Test
    fun AC37_before_anything_loads_the_state_is_empty_with_a_zero_this_week() {
        val initial = viewModel.state.value

        assertFalse(initial.isLoaded)
        assertEquals(0, initial.thisWeek.count)
    }

    @Test
    fun AC35_the_state_lists_the_saved_walks_in_weeks_and_is_not_empty() {
        addWalk("2026-10-20T08:00:00Z")

        await("one week with one walk") { it.isLoaded && it.weeks.size == 1 }

        assertFalse(latest!!.isEmpty)
        assertEquals(1, latest!!.thisWeek.count)
    }

    @Test
    fun AC40_requesting_then_cancelling_a_delete_keeps_the_walk() {
        val id = addWalk("2026-10-20T08:00:00Z")
        await("loaded") { it.weeks.isNotEmpty() }

        viewModel.requestDelete(latest!!.weeks[0].walks[0])
        await("confirmation pending") { it.pendingDelete?.id == id }
        viewModel.cancelDelete()
        await("confirmation gone") { it.pendingDelete == null }

        assertEquals(1, latest!!.weeks.size)
    }

    @Test
    fun AC40_confirming_a_delete_removes_the_walk() {
        addWalk("2026-10-20T08:00:00Z")
        await("loaded") { it.weeks.isNotEmpty() }

        viewModel.requestDelete(latest!!.weeks[0].walks[0])
        await("confirmation pending") { it.pendingDelete != null }
        viewModel.confirmDelete()

        await("walk removed") { it.isEmpty && it.pendingDelete == null }
        assertEquals(0, latest!!.thisWeek.count)
    }

    @Test
    fun confirming_with_nothing_pending_does_nothing() {
        addWalk("2026-10-20T08:00:00Z")
        await("loaded") { it.weeks.isNotEmpty() }

        viewModel.confirmDelete()

        assertEquals(1, latest!!.weeks.size)
        assertFalse(latest!!.deleteFailed)
    }

    @Test
    fun AC40_a_delete_that_fails_is_reported_and_can_be_dismissed() {
        val row = WalkSummaryRow(id = 1, startUtcMs = 1_000, endUtcMs = 61_000, distanceM = 100.0)
        val failing = HistoryViewModel(WalkRepository(FailingDao(listOf(row), failRead = false), clock), clock, ZoneOffset.UTC)
        var seen: HistoryUiState? = null
        scope.launch { failing.state.collect { seen = it } }
        awaitCondition("loaded") { seen?.weeks?.isNotEmpty() == true }

        failing.requestDelete(seen!!.weeks[0].walks[0])
        failing.confirmDelete()

        awaitCondition("failure reported and confirmation closed") { seen?.deleteFailed == true && seen?.pendingDelete == null }
        assertNull(seen!!.pendingDelete)
        failing.dismissDeleteFailure()
        awaitCondition("failure dismissed") { seen?.deleteFailed == false }
    }

    @Test
    fun AC37_refreshing_recomputes_this_week_against_the_current_time() {
        addWalk("2026-10-20T08:00:00Z")
        await("this week has the walk") { it.thisWeek.count == 1 }

        clock.advance(Duration.ofDays(7))
        viewModel.refresh()

        await("this week has rolled over") { it.thisWeek.count == 0 && it.weeks.size == 1 }
    }

    @Test
    fun a_history_that_cannot_be_read_is_reported_instead_of_crashing() {
        val broken = HistoryViewModel(WalkRepository(FailingDao(emptyList(), failRead = true), clock), clock, ZoneOffset.UTC)
        var seen: HistoryUiState? = null
        scope.launch { broken.state.collect { seen = it } }

        awaitCondition("load failure reported") { seen?.loadFailed == true }

        assertTrue(seen!!.isLoaded)
        assertTrue(seen!!.isEmpty)
    }

    /** A DAO whose reads or deletes fail, to check that the screen reports the failure. */
    private class FailingDao(
        private val rows: List<WalkSummaryRow>,
        private val failRead: Boolean,
    ) : WalkDao() {
        override fun observeCompleted(): Flow<List<WalkSummaryRow>> = if (failRead) flow { error("read failed") } else flowOf(rows)

        override suspend fun delete(walkId: Long): Int = error("delete failed")

        override suspend fun insertWalk(walk: WalkEntity): Long = unused()

        override suspend fun insertPoint(point: RoutePointEntity) = unused()

        override suspend fun updateProgress(
            walkId: Long,
            distanceM: Double,
            lastFixUtcMs: Long,
        ): Int = unused()

        override suspend fun complete(
            walkId: Long,
            endUtcMs: Long,
        ): Int = unused()

        override suspend fun get(walkId: Long): WalkEntity? = unused()

        override suspend fun recording(): List<WalkEntity> = unused()

        override suspend fun points(walkId: Long): List<RoutePointEntity> = unused()

        override suspend fun nextSeq(walkId: Long): Int = unused()

        private fun unused(): Nothing = error("not used by the history")
    }
}
