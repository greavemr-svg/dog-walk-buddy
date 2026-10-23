package io.github.greavemrsvg.dogwalkbuddy.ui.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** The debug-only seeding of synthetic walks used for the history performance check. */
@RunWith(AndroidJUnit4::class)
class DebugToolsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seeding_adds_500_finished_synthetic_walks_that_all_start_in_the_past() {
        val now = Instant.parse("2026-10-21T10:00:00Z")

        runBlocking { DebugTools.seed(database.walkDao(), now) }

        val walks = runBlocking { database.walkDao().observeCompleted().first() }
        assertEquals(DebugTools.SAMPLE_WALK_COUNT, walks.size)
        assertTrue(walks.all { it.startUtcMs < now.toEpochMilli() && it.endUtcMs > it.startUtcMs && it.distanceM > 0 })
    }
}
