package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.DogWalkBuddyApp
import io.github.greavemrsvg.dogwalkbuddy.data.db.RoutePointEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoChange
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoPreparation
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoStore
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.FAKE_MAP_TAG
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakePermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.WithPermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.START_WALK_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.STOP_WALK_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant

/**
 * Cross-feature flows through the real navigation, ViewModels, database and photo store, with a stub MET server, a
 * fake map and a fake connectivity (AC24, AC38, AC39, AC46, AC47). Coordinates are synthetic.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class AppFlowIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private lateinit var registry: FakePermissionRegistry

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        shadowOf(ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        registry =
            FakePermissionRegistry { asked ->
                if (Manifest.permission.ACCESS_COARSE_LOCATION in asked) test.permissionGranted = true
                asked.toSet()
            }
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun showApp() {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithPermissionRegistry(registry) { DogWalkBuddyNavHost(container = test.container) }
            }
        }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Polls the semantics tree, which is what lets the composition catch up, then checks the map was asked to draw.
    private fun awaitMap() {
        composeRule.waitUntil(WAIT_MS) { composeRule.onAllNodesWithTag(FAKE_MAP_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(test.routeMap.drawCount > 0)
    }

    private fun tap(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    private fun back() = composeRule.onNodeWithText("Back").performClick()

    private fun addWalkWithRoute(): Long =
        runBlocking {
            val start = Instant.parse("2026-10-19T09:15:00Z")
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
            repeat(3) { seq ->
                dao.insertPoint(
                    RoutePointEntity(walkId = id, seq = seq, lat = 51.5 + seq * 0.01, lon = -0.12, accuracyM = 5.0, timeUtcMs = 0),
                )
            }
            id
        }

    @Test
    fun AC24_history_profile_recording_and_the_background_make_no_weather_request_and_opening_the_check_makes_one() {
        // Location permission is not yet granted, so opening Home asks for it and makes no request.
        test.permissionGranted = false
        test.stub.enqueueFixture()
        showApp()
        awaitText("Allow approximate location")

        tap("Walk history")
        awaitText("No walks yet")
        back()
        tap("Dog profile")
        composeRule.waitUntil(WAIT_MS) { composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Biscuit")
        tap("Save profile")
        awaitText("Profile saved")
        back()
        tap("Privacy")
        awaitText("There are no accounts, analytics or tracking.")
        back()
        composeRule.onNodeWithTag(START_WALK_TAG).performScrollTo().performClick()
        awaitText("Recording your walk")
        composeRule.onNodeWithTag(STOP_WALK_TAG).performScrollTo().performClick()
        test.container.recordingStateHolder.clear()
        awaitText("Allow approximate location")
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        assertEquals(0, test.stub.server.requestCount)

        tap("Allow approximate location")
        awaitText("Good conditions for a walk")

        assertEquals(1, test.stub.server.requestCount)
    }

    @Test
    fun AC46_privacy_is_one_tap_from_the_main_screen_and_back_returns_there() {
        test.permissionGranted = false
        showApp()
        awaitText("Allow approximate location")

        tap("Privacy")

        composeRule.onNodeWithText("Weather check").assertIsDisplayed()
        composeRule.onNodeWithText("Delete all my data").performScrollTo().assertIsDisplayed()
        back()
        composeRule.onNodeWithText("Dog Walk Buddy").assertIsDisplayed()
    }

    @Test
    fun AC38_a_history_row_opens_the_walk_detail_with_the_route_on_the_map_and_the_attribution() {
        test.permissionGranted = false
        addWalkWithRoute()
        test.connectivity.online = true
        showApp()
        awaitText("Allow approximate location")

        tap("Walk history")
        awaitText("Mon 19 Oct 2026 at 09:15")
        composeRule.onNodeWithText("Mon 19 Oct 2026 at 09:15").performClick()
        awaitText("Walk details")
        awaitMap()

        composeRule.onNodeWithTag(FAKE_MAP_TAG).assertIsDisplayed()
        assertEquals(listOf(LatLon(51.5, -0.12), LatLon(51.51, -0.12), LatLon(51.52, -0.12)), test.routeMap.lastRoute)
        composeRule.onNodeWithText("© OpenStreetMap contributors", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("© OpenMapTiles", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Duration 1:15").assertIsDisplayed()
        composeRule.onNodeWithText("Distance 2.0 mi").assertIsDisplayed()
        back()
        awaitText("This week")
    }

    @Test
    fun AC39_offline_the_walk_detail_shows_the_facts_and_Map_not_available_offline() {
        test.permissionGranted = false
        addWalkWithRoute()
        test.connectivity.online = false
        showApp()
        awaitText("Allow approximate location")

        tap("Walk history")
        awaitText("Mon 19 Oct 2026 at 09:15")
        composeRule.onNodeWithText("Mon 19 Oct 2026 at 09:15").performClick()
        awaitText("Map not available offline")

        assertEquals(0, test.routeMap.drawCount)
        composeRule.onNodeWithText("Mon 19 Oct 2026 at 09:15").assertIsDisplayed()
        composeRule.onNodeWithText("Duration 1:15").assertIsDisplayed()
        composeRule.onNodeWithText("Distance 2.0 mi").assertIsDisplayed()
    }

    @Test
    fun AC39_when_the_map_cannot_load_the_message_replaces_it() {
        test.permissionGranted = false
        addWalkWithRoute()
        showApp()
        awaitText("Allow approximate location")
        tap("Walk history")
        awaitText("Mon 19 Oct 2026 at 09:15")
        composeRule.onNodeWithText("Mon 19 Oct 2026 at 09:15").performClick()
        awaitMap()

        composeRule.runOnUiThread { test.routeMap.failLoading() }

        awaitText("Map not available offline")
        composeRule.onNodeWithText("Duration 1:15").assertIsDisplayed()
    }

    @Test
    fun AC47_delete_all_my_data_removes_walks_profile_photo_and_the_cached_check_and_the_screens_show_it() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")
        val walkId = addWalkWithRoute()
        val photoDirectory = File(test.container.filesDir, PhotoStore.DIRECTORY_NAME)
        runBlocking {
            val ready =
                test.container.photoStore.prepare { ByteArrayInputStream(SyntheticImages.jpeg(64, 64)) } as PhotoPreparation.Ready
            test.container.profileRepository.save(
                ProfileInput("Rex", "Collie"),
                HeatSensitivity.SENSITIVE,
                PhotoChange.Replace(ready.photo),
            )
        }
        assertNotNull(runBlocking { test.database.cachedCheckDao().get() })
        assertTrue(photoDirectory.isDirectory)

        tap("Privacy")
        tap("Delete all my data")
        composeRule.onNodeWithText("Delete all your data?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete everything").performClick()
        awaitText("All your data has been deleted from this phone.")

        assertNull(runBlocking { test.database.walkDao().get(walkId) })
        assertEquals(
            0,
            runBlocking {
                test.database
                    .walkDao()
                    .points(walkId)
                    .size
            },
        )
        assertNull(runBlocking { test.database.dogProfileDao().get() })
        assertNull(runBlocking { test.database.cachedCheckDao().get() })
        assertEquals(false, photoDirectory.exists())
        assertEquals(1, test.mapCache.clearCount)
        // With no location fix and no cached result, the check has nothing to show.
        test.location = null
        back()
        awaitText("Can't check right now")
        tap("Walk history")
        awaitText("No walks yet")
        back()
        tap("Dog profile")
        awaitText("No photo")
        assertEquals(0, composeRule.onAllNodesWithText("Rex").fetchSemanticsNodes().size)
        assertEquals(1, test.stub.server.requestCount)
    }

    @Test
    fun AC47_cancelling_the_confirmation_deletes_nothing() {
        test.permissionGranted = false
        val walkId = addWalkWithRoute()
        showApp()
        awaitText("Allow approximate location")

        tap("Privacy")
        tap("Delete all my data")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        assertNotNull(runBlocking { test.database.walkDao().get(walkId) })
        assertEquals(0, test.mapCache.clearCount)
        assertEquals(0, composeRule.onAllNodesWithText("Delete all your data?").fetchSemanticsNodes().size)
    }

    @Test
    fun AC47_a_failed_deletion_is_shown_and_the_user_can_try_again() {
        test.permissionGranted = false
        addWalkWithRoute()
        test.mapCache.failure = java.io.IOException("map")
        showApp()
        awaitText("Allow approximate location")
        tap("Privacy")
        tap("Delete all my data")
        composeRule.onNodeWithText("Delete everything").performClick()
        awaitText("Couldn't delete all your data. Try again.")

        test.mapCache.failure = null
        composeRule.onNodeWithText("OK").performClick()
        tap("Delete all my data")
        composeRule.onNodeWithText("Delete everything").performClick()
        awaitText("All your data has been deleted from this phone.")

        assertEquals(1, test.mapCache.clearCount)
    }

    private companion object {
        const val WAIT_MS = 10_000L
    }
}
