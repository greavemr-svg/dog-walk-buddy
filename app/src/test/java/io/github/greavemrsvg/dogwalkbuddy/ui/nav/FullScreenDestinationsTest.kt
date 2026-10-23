package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.DogWalkBuddyApp
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakePermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.WithPermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.START_WALK_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Recording, Walk detail, Privacy and About are full-screen destinations (AC12): no bottom nav, and a back control
 * spoken "Back", at least 48 x 48 dp, that returns to the screen each was opened from.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class FullScreenDestinationsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        test.stub.enqueueFixture()
        shadowOf(ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun showApp() {
        val registry =
            FakePermissionRegistry { asked ->
                if (Manifest.permission.ACCESS_FINE_LOCATION in asked) test.fineGranted = true
                asked.toSet()
            }
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithPermissionRegistry(registry) { DogWalkBuddyNavHost(container = test.container) }
            }
        }
        awaitText("Good conditions for a walk")
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** No nav bar, and a "Back" control at least 48 x 48 dp, which is then tapped. */
    private fun assertFullScreenAndGoBack() {
        listOf(NAV_TODAY_TAG, NAV_WALKS_TAG, NAV_DOG_TAG).forEach { composeRule.onNodeWithTag(it).assertDoesNotExist() }
        composeRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
            .performClick()
    }

    @Test
    fun UX_AC12_recording_has_no_nav_and_back_returns_to_today() {
        showApp()
        composeRule.onNodeWithTag(START_WALK_TAG).assertIsDisplayed().performClick()
        awaitText("Elapsed time")

        assertFullScreenAndGoBack()

        awaitText("Walk in progress")
        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()
    }

    @Test
    fun UX_AC12_walk_detail_has_no_nav_and_back_returns_to_walks() {
        runBlocking {
            val start = Instant.parse("2026-10-19T09:15:00Z")
            test.database.walkDao().insertWalk(
                WalkEntity(
                    startUtcMs = start.toEpochMilli(),
                    endUtcMs = start.plusSeconds(WALK_SECONDS).toEpochMilli(),
                    status = WalkStatus.COMPLETE,
                    distanceM = WALK_METRES,
                ),
            )
        }
        showApp()
        composeRule.onNodeWithTag(NAV_WALKS_TAG).performClick()
        awaitText("at 09:15")
        composeRule.onNode(hasText("Mon 19 Oct") and hasText("at 09:15")).performClick()
        awaitText("Walk details")

        assertFullScreenAndGoBack()

        awaitText("This week")
        composeRule.onNodeWithTag(NAV_WALKS_TAG).assertIsSelected()
    }

    @Test
    fun UX_AC12_privacy_has_no_nav_and_back_returns_to_the_dog_tab() {
        showApp()
        composeRule.onNodeWithTag(NAV_DOG_TAG).performClick()
        composeRule.onNodeWithText("Privacy & data").performScrollTo().performClick()
        awaitText("There are no accounts, analytics or tracking.")

        assertFullScreenAndGoBack()

        awaitText("Save profile")
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertIsSelected()
    }

    @Test
    fun UX_AC12_about_has_no_nav_and_back_returns_to_today() {
        showApp()
        composeRule.onNode(hasText("About this advice") and hasClickAction()).performScrollTo().performClick()
        awaitText("Rating thresholds")

        assertFullScreenAndGoBack()

        awaitText("Good conditions for a walk")
        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()
    }

    private companion object {
        const val WAIT_MS = 10_000L
        const val WALK_SECONDS = 4_500L
        const val WALK_METRES = 3_218.688
    }
}
