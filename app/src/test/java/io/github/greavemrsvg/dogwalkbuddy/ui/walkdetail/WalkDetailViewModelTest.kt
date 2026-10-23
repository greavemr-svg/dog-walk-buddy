package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.RoutePointEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.data.net.ConnectivityStatus
import io.github.greavemrsvg.dogwalkbuddy.data.repo.WalkRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Instant

/** The walk detail's state: the walk, its route and whether the map can be shown (AC38, AC39). */
@RunWith(AndroidJUnit4::class)
class WalkDetailViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private val start = Instant.parse("2026-10-19T09:15:00Z")

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun addWalk(points: Int = 3): Long =
        runBlocking {
            val dao = test.database.walkDao()
            val id =
                dao.insertWalk(
                    WalkEntity(
                        startUtcMs = start.toEpochMilli(),
                        endUtcMs = start.plusSeconds(4500).toEpochMilli(),
                        status = WalkStatus.COMPLETE,
                        distanceM = 3218.688,
                    ),
                )
            repeat(points) { seq ->
                dao.insertPoint(
                    RoutePointEntity(
                        walkId = id,
                        seq = seq,
                        lat = 51.5 + seq * 0.001,
                        lon = -0.12,
                        accuracyM = 5.0,
                        timeUtcMs = start.toEpochMilli() + seq * 3000L,
                    ),
                )
            }
            id
        }

    private fun viewModel(
        walkId: Long,
        connectivity: ConnectivityStatus = test.connectivity,
        repository: WalkRepository = test.container.walkRepository,
    ) = WalkDetailViewModel(walkId, repository, connectivity)

    private fun loaded(viewModel: WalkDetailViewModel): WalkDetailUiState.Loaded {
        awaitCondition("walk detail loaded") { viewModel.state.value !is WalkDetailUiState.Loading }
        return viewModel.state.value as WalkDetailUiState.Loaded
    }

    @Test
    fun AC38_the_state_holds_the_walk_with_its_duration_distance_and_route_in_order() {
        val state = loaded(viewModel(addWalk()))

        assertEquals(start, state.walk.startUtc)
        assertEquals(4500, state.walk.durationSeconds)
        assertEquals(3218.688, state.walk.distanceM, 0.0001)
        assertEquals(listOf(LatLon(51.5, -0.12), LatLon(51.501, -0.12), LatLon(51.502, -0.12)), state.route)
    }

    @Test
    fun AC38_with_a_connection_the_map_is_available() {
        test.connectivity.online = true

        assertTrue(loaded(viewModel(addWalk())).mapAvailable)
    }

    @Test
    fun AC39_without_a_connection_the_map_is_not_available_but_the_walk_facts_are_still_loaded() {
        test.connectivity.online = false

        val state = loaded(viewModel(addWalk()))

        assertFalse(state.mapAvailable)
        assertEquals(4500, state.walk.durationSeconds)
        assertEquals(3, state.route.size)
    }

    @Test
    fun AC39_the_connection_is_asked_once_as_the_detail_opens() {
        var asked = 0
        val vm = viewModel(addWalk(), connectivity = { ++asked > 0 })

        loaded(vm)
        vm.onMapLoadFailed()

        assertEquals(1, asked)
    }

    @Test
    fun AC39_when_the_map_reports_a_load_failure_the_map_is_no_longer_available() {
        val vm = viewModel(addWalk())
        assertTrue(loaded(vm).mapAvailable)

        vm.onMapLoadFailed()

        val state = vm.state.value as WalkDetailUiState.Loaded
        assertFalse(state.mapAvailable)
        assertEquals(4500, state.walk.durationSeconds)
    }

    @Test
    fun a_load_failure_report_before_the_walk_loads_changes_nothing() {
        val vm = viewModel(walkId = 999)
        awaitCondition("not found") { vm.state.value == WalkDetailUiState.NotFound }

        vm.onMapLoadFailed()

        assertEquals(WalkDetailUiState.NotFound, vm.state.value)
    }

    @Test
    fun a_walk_with_no_route_points_loads_with_an_empty_route() {
        assertEquals(emptyList<LatLon>(), loaded(viewModel(addWalk(points = 0))).route)
    }

    @Test
    fun a_walk_that_does_not_exist_is_not_found() {
        val vm = viewModel(walkId = 12345)

        awaitCondition("not found") { vm.state.value == WalkDetailUiState.NotFound }
    }

    @Test
    fun a_walk_still_marked_recording_uses_the_last_fix_time_as_its_end() {
        val id =
            runBlocking {
                test.database.walkDao().insertWalk(
                    WalkEntity(
                        startUtcMs = start.toEpochMilli(),
                        status = WalkStatus.RECORDING,
                        distanceM = 100.0,
                        lastFixUtcMs = start.plusSeconds(600).toEpochMilli(),
                    ),
                )
            }

        assertEquals(600, loaded(viewModel(id)).walk.durationSeconds)
    }

    @Test
    fun a_walk_with_an_end_before_its_start_shows_zero_duration() {
        val id =
            runBlocking {
                test.database.walkDao().insertWalk(
                    WalkEntity(
                        startUtcMs = start.toEpochMilli(),
                        endUtcMs = start.minusSeconds(60).toEpochMilli(),
                        status = WalkStatus.COMPLETE,
                    ),
                )
            }

        assertEquals(0, loaded(viewModel(id)).walk.durationSeconds)
    }

    @Test
    fun a_database_failure_is_reported_as_load_failed_without_a_crash() {
        // A broken table makes the read fail, as a damaged database would.
        test.database.openHelper.writableDatabase
            .execSQL("DROP TABLE route_point")
        test.database.openHelper.writableDatabase
            .execSQL("DROP TABLE walk")

        val vm = viewModel(walkId = 1)

        awaitCondition("load failed") { vm.state.value == WalkDetailUiState.LoadFailed }
    }

    @Test
    fun the_factory_builds_a_view_model_for_the_requested_walk() {
        val id = addWalk()
        val store = ViewModelStore()
        val vm = ViewModelProvider.create(store, WalkDetailViewModel.factory(test.container, id))[WalkDetailViewModel::class]

        assertEquals(4500, loaded(vm).walk.durationSeconds)
        store.clear()
    }

    @Test
    fun UX_AC48_asking_to_delete_opens_the_confirmation_and_deletes_nothing() {
        val id = addWalk()
        val vm = viewModel(id)
        loaded(vm)

        vm.requestDelete()

        assertTrue(vm.deletion.value.confirming)
        assertFalse(vm.deletion.value.deleted)
        assertEquals(id, runBlocking { test.database.walkDao().get(id) }?.id)
    }

    @Test
    fun UX_AC48_cancelling_closes_the_confirmation_and_keeps_the_walk() {
        val id = addWalk()
        val vm = viewModel(id)
        loaded(vm)

        vm.requestDelete()
        vm.cancelDelete()
        // Confirming after a cancel does nothing, because nothing is being confirmed.
        vm.confirmDelete()

        assertEquals(WalkDeletionState(), vm.deletion.value)
        assertEquals(id, runBlocking { test.database.walkDao().get(id) }?.id)
        assertEquals(3, runBlocking { test.database.walkDao().points(id) }.size)
    }

    @Test
    fun UX_AC48_confirming_removes_the_walk_and_its_route_and_reports_it_deleted() {
        val id = addWalk()
        val vm = viewModel(id)
        loaded(vm)

        vm.requestDelete()
        vm.confirmDelete()

        awaitCondition("walk deleted") { vm.deletion.value.deleted }
        assertFalse(vm.deletion.value.confirming)
        assertEquals(null, runBlocking { test.database.walkDao().get(id) })
        assertTrue(runBlocking { test.database.walkDao().points(id) }.isEmpty())
    }

    @Test
    fun UX_AC48_a_delete_that_fails_is_reported_and_can_be_dismissed() {
        val id = addWalk()
        val vm = viewModel(id)
        loaded(vm)
        // A broken table makes the delete fail, as a damaged database would.
        test.database.openHelper.writableDatabase
            .execSQL("DROP TABLE route_point")
        test.database.openHelper.writableDatabase
            .execSQL("DROP TABLE walk")

        vm.requestDelete()
        vm.confirmDelete()

        awaitCondition("delete failed") { vm.deletion.value.failed }
        assertFalse(vm.deletion.value.deleted)
        vm.dismissDeleteFailure()
        assertEquals(WalkDeletionState(), vm.deletion.value)
    }
}
