package io.github.greavemrsvg.dogwalkbuddy

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_screen_shows_the_app_title() {
        composeRule.onNodeWithText("Dog Walk Buddy").assertExists()
    }

    @Test
    fun application_builds_a_container_with_the_system_zone() {
        val app = ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()

        assertEquals(ZoneId.systemDefault(), app.container.zoneId)
    }

    @Test
    fun container_uses_the_injected_clock_and_zone() {
        val fixed = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC)
        val zone = ZoneId.of("Europe/London")

        val container = AppContainer(clock = fixed, zoneId = zone)

        assertEquals(Instant.parse("2026-10-19T12:00:00Z"), container.clock.instant())
        assertEquals(zone, container.zoneId)
    }
}
