package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import io.github.greavemrsvg.dogwalkbuddy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The window themes and manifest that sit around the Compose theme (ADR 0007): the window background is the
 * Field Kit background of the current system theme (AC1, AC2), `MainActivity` handles a light/dark switch itself
 * (AC7), and the app offers no theme setting of its own (AC7).
 */
@RunWith(AndroidJUnit4::class)
class WindowThemeResourcesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun windowBackground(): Int {
        val themed = context.createConfigurationContext(context.resources.configuration)
        themed.setTheme(R.style.Theme_DogWalkBuddy)
        val attributes = themed.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        try {
            return attributes.getColor(0, 0)
        } finally {
            attributes.recycle()
        }
    }

    @Test
    fun UX_AC1_the_window_background_colours_are_the_background_tokens() {
        assertEquals(LightFieldKitColors.background.toArgb(), context.getColor(R.color.field_kit_background_light))
        assertEquals(DarkFieldKitColors.background.toArgb(), context.getColor(R.color.field_kit_background_dark))
    }

    @Test
    fun UX_AC1_the_light_window_theme_uses_the_light_background() {
        assertEquals(LightFieldKitColors.background.toArgb(), windowBackground())
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC2_the_night_window_theme_uses_the_dark_background() {
        assertEquals(DarkFieldKitColors.background.toArgb(), windowBackground())
        assertNotEquals(LightFieldKitColors.background.toArgb(), windowBackground())
    }

    @Test
    fun UX_AC7_main_activity_handles_a_ui_mode_change_without_being_recreated() {
        val info = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertTrue("MainActivity must declare configChanges=\"uiMode\"", info.configChanges and ActivityInfo.CONFIG_UI_MODE != 0)
    }

    @Test
    fun UX_AC7_there_is_no_in_app_theme_setting() {
        val words = listOf("theme", "dark mode", "light mode", "night mode", "appearance")
        val strings =
            R.string::class.java.fields.map { field ->
                val id = field.getInt(null)
                context.resources.getResourceEntryName(id) to context.getString(id)
            }

        assertTrue("no string resources found", strings.size > MIN_STRINGS)
        strings.forEach { (name, text) ->
            words.forEach { word ->
                assertFalse("$name offers a theme choice: \"$text\"", text.contains(word, ignoreCase = true))
            }
        }
    }

    private companion object {
        const val MIN_STRINGS = 50
    }
}
