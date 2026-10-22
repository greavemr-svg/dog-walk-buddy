package io.github.greavemrsvg.dogwalkbuddy.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckData
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.service.WalkRecordingController
import io.github.greavemrsvg.dogwalkbuddy.service.WalkServiceLauncher
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakeMapCacheClearer
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticTrack
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.time.Duration

/** "Delete all my data" (AC47, ADR 0004) against a real in-memory database and a real photo directory. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeleteAllDataUseCaseTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private val container get() = test.container
    private val photoDirectory get() = File(container.filesDir, PhotoStore.DIRECTORY_NAME)

    private fun useCase(
        evictHttpCache: () -> Unit = {},
        mapCache: FakeMapCacheClearer = test.mapCache,
        timeout: Duration = Duration.ofMillis(300),
    ) = DeleteAllDataUseCase(
        recordingController = container.walkRecordingController,
        recordingState = container.recordingStateHolder,
        database = test.database,
        photoStore = container.photoStore,
        evictHttpCache = evictHttpCache,
        mapCache = mapCache,
        stopTimeout = timeout,
    )

    /** A finished walk with synthetic route points. */
    private fun addWalkWithRoute(): Long =
        runBlocking {
            val repository = container.walkRepository
            val walk = repository.startWalk()
            val fixes = SyntheticTrack.north(fixes = 5, stepM = 20.0, accuracyM = 5.0, start = walk.startUtc)
            fixes.forEachIndexed { index, fix -> repository.recordFix(walk.id, index, fix, 20.0 * (index + 1)) }
            repository.finishWalk(walk.id)
            walk.id
        }

    private fun addProfileWithPhoto() =
        runBlocking {
            val ready = container.photoStore.prepare { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) } as PhotoPreparation.Ready
            container.profileRepository.save(ProfileInput("Rex", "Collie"), HeatSensitivity.SENSITIVE, PhotoChange.Replace(ready.photo))
        }

    private fun addCachedCheck() =
        runBlocking {
            test.stub.enqueueFixture()
            container.checkRepository.check(LocationFix(51.507351, -0.127758, accuracyM = 1000.0, timeUtc = test.clock.instant()))
        }

    @Test
    fun AC47_everything_is_removed_walks_routes_profile_photo_and_cached_check() =
        runBlocking {
            val walkId = addWalkWithRoute()
            addProfileWithPhoto()
            addCachedCheck()
            File(photoDirectory, "photo_00000000-0000-0000-0000-000000000009.jpg.partial").writeBytes(byteArrayOf(1))
            assertEquals(5, container.walkRepository.routePoints(walkId).size)
            assertNotNull(test.database.dogProfileDao().get())
            assertNotNull(test.database.cachedCheckDao().get())
            assertTrue(photoDirectory.listFiles()!!.size >= 2)

            useCase()()

            assertTrue(
                container.walkRepository
                    .observeSummaries()
                    .first()
                    .isEmpty(),
            )
            assertNull(container.walkRepository.walk(walkId))
            assertEquals(0, container.walkRepository.routePoints(walkId).size)
            assertNull(test.database.dogProfileDao().get())
            assertNull(test.database.cachedCheckDao().get())
            assertFalse(photoDirectory.exists())
        }

    @Test
    fun AC47_the_check_has_no_cached_result_afterwards() =
        runBlocking {
            addCachedCheck()

            useCase()()

            val offline = container.checkRepository.loadCached()
            assertEquals(CheckData.Unavailable, offline)
        }

    @Test
    fun AC47_the_weather_http_cache_and_the_map_tile_cache_are_cleared() =
        runBlocking {
            var evicted = 0

            useCase(evictHttpCache = { evicted++ })()

            assertEquals(1, evicted)
            assertEquals(1, test.mapCache.clearCount)
        }

    @Test
    fun AC47_the_container_use_case_clears_the_real_weather_http_cache() =
        runBlocking {
            container.deleteAllDataUseCase()

            assertEquals(0L, container.weatherHttpClient.cache!!.size())
            assertEquals(1, test.mapCache.clearCount)
        }

    @Test
    fun AC47_an_active_recording_is_stopped_first_then_its_walk_is_deleted() =
        runBlocking {
            val holder = container.recordingStateHolder
            var walksWhenStopped = -1
            var recordingWhenStopped = false
            val launcher =
                object : WalkServiceLauncher {
                    override fun start(walkId: Long) = Unit

                    // The real service saves the walk and clears the session. By then no data may have been deleted.
                    override fun stop() {
                        runBlocking {
                            walksWhenStopped =
                                test.database
                                    .walkDao()
                                    .recording()
                                    .size
                            recordingWhenStopped = holder.isRecording
                            container.walkRepository.recordingWalks().forEach { container.walkRepository.finishWalk(it.id) }
                        }
                        holder.clear()
                    }
                }
            val controller =
                WalkRecordingController(
                    repository = container.walkRepository,
                    holder = holder,
                    launcher = launcher,
                    recovery = RecoverInterruptedWalkUseCase(container.walkRepository) { holder.state.value?.walkId },
                )
            assertTrue(controller.start())

            DeleteAllDataUseCase(
                recordingController = controller,
                recordingState = holder,
                database = test.database,
                photoStore = container.photoStore,
                evictHttpCache = {},
                mapCache = test.mapCache,
            )()

            assertEquals(1, walksWhenStopped)
            assertTrue(recordingWhenStopped)
            assertFalse(holder.isRecording)
            assertTrue(
                test.database
                    .walkDao()
                    .recording()
                    .isEmpty(),
            )
            assertTrue(
                container.walkRepository
                    .observeSummaries()
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun AC47_nothing_is_deleted_if_a_recording_does_not_stop_in_time() =
        runBlocking {
            addProfileWithPhoto()
            assertTrue(container.walkRecordingController.start())

            val failure = assertThrows(DeleteAllDataException::class.java) { runBlocking { useCase(timeout = Duration.ofMillis(50))() } }

            assertTrue(failure.cause is IllegalStateException)
            assertEquals(1, test.launcher.stopCount)
            assertNotNull(test.database.dogProfileDao().get())
            assertEquals(
                1,
                test.database
                    .walkDao()
                    .recording()
                    .size,
            )
            assertTrue(photoDirectory.exists())
            assertEquals(0, test.mapCache.clearCount)
        }

    @Test
    fun AC47_with_no_recording_the_service_is_not_asked_to_stop() =
        runBlocking {
            useCase()()

            assertEquals(0, test.launcher.stopCount)
        }

    @Test
    fun AC47_a_failing_step_does_not_stop_the_others_and_the_failure_is_reported() =
        runBlocking {
            addWalkWithRoute()
            addProfileWithPhoto()
            test.mapCache.failure = IOException("map")
            var evicted = 0

            val failure =
                assertThrows(DeleteAllDataException::class.java) {
                    runBlocking {
                        useCase(evictHttpCache = {
                            evicted++
                            throw IOException("http")
                        })()
                    }
                }

            // The database and the photos were still deleted, and both failures are kept.
            assertEquals("http", failure.cause!!.message)
            assertEquals(
                "map",
                failure.cause!!
                    .suppressed
                    .single()
                    .message,
            )
            assertEquals(1, evicted)
            assertTrue(
                container.walkRepository
                    .observeSummaries()
                    .first()
                    .isEmpty(),
            )
            assertNull(test.database.dogProfileDao().get())
            assertFalse(photoDirectory.exists())
        }

    @Test
    fun AC47_deleting_can_be_repeated_after_a_failure() =
        runBlocking {
            addWalkWithRoute()
            test.mapCache.failure = IOException("map")
            assertThrows(DeleteAllDataException::class.java) { runBlocking { useCase()() } }
            test.mapCache.failure = null

            useCase()()

            assertEquals(1, test.mapCache.clearCount)
            assertTrue(
                container.walkRepository
                    .observeSummaries()
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun AC47_with_no_data_at_all_it_succeeds() =
        runBlocking {
            useCase()()

            assertTrue(
                container.walkRepository
                    .observeSummaries()
                    .first()
                    .isEmpty(),
            )
            assertEquals(1, test.mapCache.clearCount)
        }

    @Test
    fun AC47_it_announces_that_the_database_was_emptied_so_screens_can_forget_what_they_hold() =
        runBlocking {
            val useCase = useCase()
            val announced = async(start = CoroutineStart.UNDISPATCHED) { useCase.deletions.first() }

            useCase()

            assertEquals(Unit, announced.await())
        }
}
