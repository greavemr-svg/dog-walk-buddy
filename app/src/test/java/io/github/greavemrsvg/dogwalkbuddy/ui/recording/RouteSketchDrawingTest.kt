package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.PanelPoint
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RecordingPalette
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RecordingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pixel checks of the live route line (AC35) and its colours (AC4): the line, its casing, the start ring, the current
 * marker and the empty panel are drawn in the recording palette, the same when the system is light or dark.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RouteSketchDrawingTest {
    @get:Rule
    val composeRule = createComposeRule()

    // A goes east to B, then north to C: an L with the start at the bottom left and the current fix at the top right.
    private val a = LatLon(51.5000, -0.1300)
    private val b = LatLon(51.5000, -0.1200)
    private val c = LatLon(51.5050, -0.1200)

    private fun show(route: List<LatLon>) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                RecordingTheme {
                    RouteSketch(route = route, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun px(dp: Dp): Int = with(composeRule.density) { dp.toPx() }.roundToInt()

    private fun points(): List<PanelPoint> = composeRule.onNodeWithTag(ROUTE_SKETCH_TAG).fetchSemanticsNode().config[RouteSketchPoints]

    private fun image(): PixelMap = composeRule.onNodeWithTag(ROUTE_SKETCH_TAG).captureToImage().toPixelMap()

    private fun PixelMap.at(
        x: Float,
        y: Float,
    ): Color = this[x.roundToInt(), y.roundToInt()]

    private fun assertClose(
        what: String,
        expected: Color,
        actual: Color,
    ) {
        val tolerance = 3f / 255f
        val close =
            abs(expected.red - actual.red) <= tolerance &&
                abs(expected.green - actual.green) <= tolerance &&
                abs(expected.blue - actual.blue) <= tolerance
        assertTrue("$what: expected $expected but was $actual", close)
    }

    private fun assertTheRouteIsDrawnInTheRecordingPalette() {
        show(listOf(a, b, c))
        val (start, corner, current) = points()
        val pixels = image()

        // The A-B segment is horizontal: its middle is the route colour and 3 dp above it is the casing.
        val midX = (start.x + corner.x) / 2
        assertEquals("route line", RecordingPalette.route, pixels.at(midX, start.y))
        assertEquals("route casing", RecordingPalette.routeCasing, pixels.at(midX, start.y - px(3.dp)))

        // Start marker at A: a ring in the route colour around a background-coloured centre.
        assertEquals("start ring centre", RecordingPalette.background, pixels.at(start.x, start.y))
        assertEquals("start ring", RecordingPalette.route, pixels.at(start.x, start.y + px(6.dp)))

        // Current marker at C: a white dot inside a translucent route-coloured halo over the panel.
        assertEquals("current dot", RecordingPalette.onDark, pixels.at(current.x, current.y))
        val halo =
            Color(
                red = RecordingPalette.route.red * HALO + RecordingPalette.mapPanel.red * (1 - HALO),
                green = RecordingPalette.route.green * HALO + RecordingPalette.mapPanel.green * (1 - HALO),
                blue = RecordingPalette.route.blue * HALO + RecordingPalette.mapPanel.blue * (1 - HALO),
            )
        assertClose("current halo", halo, pixels.at(current.x + px(9.dp), current.y))

        // Away from the line the panel is the map panel colour, framed by its border.
        assertEquals("panel", RecordingPalette.mapPanel, pixels.at(start.x, current.y))
        assertEquals("panel border", RecordingPalette.mapPanelBorder, pixels[1, pixels.height / 2])
    }

    @Test
    fun UX_AC35_the_route_is_drawn_as_a_cased_line_with_a_start_ring_at_a_and_the_current_marker_at_c() {
        assertTheRouteIsDrawnInTheRecordingPalette()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC4_the_route_line_uses_the_same_recording_colours_when_the_system_is_dark() {
        assertTheRouteIsDrawnInTheRecordingPalette()
    }

    @Test
    fun UX_AC35_with_no_fixes_the_panel_is_empty_and_has_no_line() {
        show(emptyList())

        assertEquals(emptyList<PanelPoint>(), points())
        val pixels = image()
        for (x in listOf(pixels.width / 4, pixels.width / 2, pixels.width * 3 / 4)) {
            for (y in listOf(pixels.height / 4, pixels.height / 2, pixels.height * 3 / 4)) {
                assertEquals("empty panel at $x,$y", RecordingPalette.mapPanel, pixels[x, y])
            }
        }
    }

    @Test
    fun UX_AC35_two_fixes_are_enough_for_a_line_from_the_start_ring_to_the_current_marker() {
        show(listOf(a, b))
        val (start, current) = points()
        val pixels = image()

        assertEquals("route line", RecordingPalette.route, pixels.at((start.x + current.x) / 2, start.y))
        assertEquals("start ring centre", RecordingPalette.background, pixels.at(start.x, start.y))
        assertEquals("current dot", RecordingPalette.onDark, pixels.at(current.x, current.y))
    }

    private companion object {
        const val HALO = 0.25f
    }
}
