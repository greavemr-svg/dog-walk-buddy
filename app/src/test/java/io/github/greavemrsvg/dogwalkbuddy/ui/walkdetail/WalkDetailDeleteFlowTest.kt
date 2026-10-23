package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.RoutePointEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakePermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.WithPermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.DogWalkBuddyNavHost
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_WALKS_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * UX AC48 end to end: "Delete this walk" on Walk detail, through the real navigation, ViewModel and database.
 * Coordinates are synthetic.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class WalkDetailDeleteFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private lateinit var registry: FakePermissionRegistry

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        test.permissionGranted = false
        registry = FakePermissionRegistry { asked -> asked.toSet() }
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitGone(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun addWalk(start: Instant): Long =
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
            repeat(3) { seq ->
                dao.insertPoint(
                    RoutePointEntity(walkId = id, seq = seq, lat = 51.5 + seq * 0.01, lon = -0.12, accuracyM = 5.0, timeUtcMs = 0),
                )
            }
            id
        }

    private fun openDetailOfThe0915Walk() {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithPermissionRegistry(registry) { DogWalkBuddyNavHost(container = test.container) }
            }
        }
        awaitText("Allow approximate location")
        composeRule.onNodeWithTag(NAV_WALKS_TAG).performClick()
        awaitText("at 09:15")
        composeRule.onNode(hasText("Mon 19 Oct") and hasText("at 09:15")).performClick()
        awaitText("Monday 19 October")
    }

    @Test
    fun UX_AC48_confirming_removes_only_this_walk_and_its_route_and_returns_to_walks() {
        val deleted = addWalk(Instant.parse("2026-10-19T09:15:00Z"))
        val kept = addWalk(Instant.parse("2026-10-20T07:42:00Z"))
        openDetailOfThe0915Walk()

        composeRule.onNodeWithText("Delete this walk").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete walk").performClick()

        awaitText("This week")
        awaitGone("at 09:15")
        composeRule.onNodeWithText("Monday 19 October").assertDoesNotExist()
        assertNull(runBlocking { test.database.walkDao().get(deleted) })
        assertEquals(0, runBlocking { test.database.walkDao().points(deleted) }.size)
        assertNotNull(runBlocking { test.database.walkDao().get(kept) })
        assertEquals(3, runBlocking { test.database.walkDao().points(kept) }.size)
        awaitText("at 07:42")
    }

    @Test
    fun UX_AC48_cancel_closes_the_sheet_keeps_the_walk_and_stays_on_the_detail() {
        val id = addWalk(Instant.parse("2026-10-19T09:15:00Z"))
        openDetailOfThe0915Walk()

        composeRule.onNodeWithText("Delete this walk").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithText("Delete this walk?").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Monday 19 October").assertIsDisplayed()
        assertNotNull(runBlocking { test.database.walkDao().get(id) })
        assertEquals(3, runBlocking { test.database.walkDao().points(id) }.size)
    }

    private companion object {
        const val WAIT_MS = 10_000L
    }
}
