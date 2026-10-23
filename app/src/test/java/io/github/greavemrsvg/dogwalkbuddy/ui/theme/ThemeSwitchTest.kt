package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Dark mode follows the system theme (AC1, AC2, AC7). `MainActivity` handles `uiMode` itself, so a switch
 * redraws in the other theme without re-creating the activity or losing state. Recording stays dark (AC4).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ThemeSwitchTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** What one composition of the theme saw. */
    private data class Seen(
        val state: Any,
        val colors: FieldKitColors,
        val scheme: ColorScheme,
    )

    private val seen = mutableListOf<Seen>()

    /** Records the theme on every composition, and a remembered object that is lost if the UI is rebuilt. */
    @Composable
    private fun ThemeProbe() {
        val state = remember { Any() }
        val colors = FieldKit.colors
        val scheme = MaterialTheme.colorScheme
        SideEffect { seen += Seen(state, colors, scheme) }
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MILLIS))
        composeRule.waitForIdle()
    }

    private fun switchNightMode(
        controller: ActivityController<MainActivity>,
        night: Boolean,
    ) {
        val configuration = Configuration(controller.get().resources.configuration)
        val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
        controller.configurationChange(configuration)
        idle()
    }

    private fun lightStatusBarIcons(activity: MainActivity): Boolean =
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars

    @Test
    fun UX_AC7_a_system_theme_switch_redraws_in_the_other_theme_without_recreating_the_activity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent { DogWalkBuddyTheme { ThemeProbe() } }
        idle()
        val first = seen.last()
        assertEquals(LightFieldKitColors, first.colors)
        assertTrue(lightStatusBarIcons(activity))

        switchNightMode(controller, night = true)

        assertSame(activity, controller.get())
        assertFalse(activity.isFinishing)
        assertEquals(DarkFieldKitColors, seen.last().colors)
        assertEquals(DarkColors.background, seen.last().scheme.background)
        assertSame("remembered state survives the switch", first.state, seen.last().state)
        assertFalse(lightStatusBarIcons(activity))

        switchNightMode(controller, night = false)

        assertSame(activity, controller.get())
        assertEquals(LightFieldKitColors, seen.last().colors)
        assertSame(first.state, seen.last().state)
        assertTrue(lightStatusBarIcons(activity))
        controller.pause().stop().destroy()
    }

    private fun windowBackground(activity: MainActivity): Int = (activity.window.decorView.background as ColorDrawable).color

    @Test
    fun UX_AC7_the_window_background_follows_a_theme_switch() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        idle()
        assertEquals(LightFieldKitColors.background.toArgb(), windowBackground(activity))

        switchNightMode(controller, night = true)

        assertEquals(DarkFieldKitColors.background.toArgb(), windowBackground(activity))

        switchNightMode(controller, night = false)

        assertEquals(LightFieldKitColors.background.toArgb(), windowBackground(activity))
        controller.pause().stop().destroy()
    }

    @Test
    fun UX_AC7_the_app_keeps_its_activity_and_view_models_through_a_theme_switch() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        idle()
        val viewModel = ViewModelProvider(activity)[MarkerViewModel::class.java]
        val content = activity.window.decorView

        switchNightMode(controller, night = true)

        assertSame(activity, controller.get())
        assertSame(content, activity.window.decorView)
        assertSame(viewModel, ViewModelProvider(activity)[MarkerViewModel::class.java])
        assertEquals(
            Configuration.UI_MODE_NIGHT_YES,
            activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
        )
        controller.pause().stop().destroy()
    }

    /** A ViewModel kept in the activity's store, which a re-created activity would replace. */
    class MarkerViewModel : ViewModel()

    private class Provided(
        val colors: FieldKitColors,
        val palettes: RatingPalettes,
        val ratingColors: RatingColors,
        val typography: FieldKitTypography,
        val recording: RecordingColors,
        val scheme: ColorScheme,
        val materialTypography: Typography,
    )

    private fun provided(content: @Composable (@Composable () -> Unit) -> Unit): Provided {
        var result: Provided? = null
        composeRule.setContent {
            content {
                result =
                    Provided(
                        colors = FieldKit.colors,
                        palettes = FieldKit.ratingPalettes,
                        ratingColors = LocalRatingColors.current,
                        typography = FieldKit.typography,
                        recording = FieldKit.recording,
                        scheme = MaterialTheme.colorScheme,
                        materialTypography = MaterialTheme.typography,
                    )
            }
        }
        composeRule.waitForIdle()
        return checkNotNull(result)
    }

    @Test
    fun UX_AC1_the_theme_provides_the_light_roles_when_the_system_is_light() {
        val theme = provided { DogWalkBuddyTheme(it) }

        assertEquals(LightFieldKitColors, theme.colors)
        assertEquals(LightRatingPalettes, theme.palettes)
        assertEquals(LightRatingColors, theme.ratingColors)
        assertSame(LightColors, theme.scheme)
        assertSame(DefaultFieldKitTypography, theme.typography)
        assertSame(FieldKitMaterialTypography, theme.materialTypography)
        assertSame(RecordingPalette, theme.recording)
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC2_the_theme_provides_the_dark_roles_when_the_system_is_dark() {
        val theme = provided { DogWalkBuddyTheme(it) }

        assertEquals(DarkFieldKitColors, theme.colors)
        assertEquals(DarkRatingPalettes, theme.palettes)
        assertEquals(DarkRatingColors, theme.ratingColors)
        assertSame(DarkColors, theme.scheme)
        assertSame(RecordingPalette, theme.recording)
    }

    @Test
    fun UX_AC4_the_recording_theme_uses_the_recording_palette_when_the_system_is_light() {
        val theme = provided { DogWalkBuddyTheme { RecordingTheme(it) } }

        assertSame(RecordingPalette, theme.recording)
        assertSame(RecordingColorScheme, theme.scheme)
        assertSame(FieldKitMaterialTypography, theme.materialTypography)
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC4_the_recording_theme_uses_the_recording_palette_when_the_system_is_dark() {
        val theme = provided { DogWalkBuddyTheme { RecordingTheme(it) } }

        assertSame(RecordingPalette, theme.recording)
        assertSame(RecordingColorScheme, theme.scheme)
    }

    private companion object {
        const val FRAME_MILLIS = 100L
    }
}
