package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The type scale (AC5): display styles use the bundled Bricolage Grotesque and body styles the bundled Figtree,
 * at the sizes and weights of `tokens.json` → `type.scale`. Fonts ship in the app and are never fetched (AC65).
 */
class FieldKitTypographyTest {
    private val typography = DefaultFieldKitTypography

    private data class Expected(
        val fontRes: Int,
        val size: Int,
        val weight: Int,
        val lineHeight: TextUnit = TextUnit.Unspecified,
        val letterSpacing: TextUnit = TextUnit.Unspecified,
        val features: String? = null,
    )

    private fun resourceFontOf(style: TextStyle): ResourceFont {
        val family = style.fontFamily as FontListFontFamily
        assertEquals(1, family.fonts.size)
        return family.fonts.single() as ResourceFont
    }

    private fun axes(font: ResourceFont): Map<String, Float> =
        font.variationSettings.settings.associate { it.axisName to it.toVariationValue(null) }

    private fun assertStyle(
        name: String,
        style: TextStyle,
        expected: Expected,
    ) {
        val font = resourceFontOf(style)
        assertEquals("$name font file", expected.fontRes, font.resId)
        assertEquals("$name size", expected.size.sp, style.fontSize)
        assertEquals("$name weight", FontWeight(expected.weight), style.fontWeight)
        assertEquals("$name font weight", FontWeight(expected.weight), font.weight)
        assertEquals("$name line height", expected.lineHeight, style.lineHeight)
        assertEquals("$name letter spacing", expected.letterSpacing, style.letterSpacing)
        assertEquals("$name font features", expected.features, style.fontFeatureSettings)
        val expectedAxes =
            if (expected.fontRes == R.font.bricolage_grotesque) {
                mapOf("wght" to expected.weight.toFloat(), "opsz" to expected.size.toFloat())
            } else {
                mapOf("wght" to expected.weight.toFloat())
            }
        assertEquals("$name variation axes", expectedAxes, axes(font))
    }

    private val bricolage = R.font.bricolage_grotesque
    private val figtree = R.font.figtree

    @Test
    fun UX_AC5_display_styles_use_bricolage_grotesque_at_the_token_sizes_and_weights() {
        mapOf(
            "ratingWord" to (typography.ratingWord to Expected(bricolage, 60, 800, 0.95.em, (-0.02).em)),
            "ratingWord (Caution)" to (typography.ratingWordCaution to Expected(bricolage, 52, 800, 0.95.em, (-0.02).em)),
            "timer" to (typography.timer to Expected(bricolage, 92, 700, features = "tnum")),
            "screenTitle" to (typography.screenTitle to Expected(bricolage, 32, 800, 1.1.em)),
            "checkQuestion" to (typography.checkQuestion to Expected(bricolage, 27, 700, 1.1.em)),
            "statLarge" to (typography.statLarge to Expected(bricolage, 36, 700, features = "tnum")),
            "bestWindow" to (typography.bestWindow to Expected(bricolage, 24, 700)),
        ).forEach { (name, pair) -> assertStyle(name, pair.first, pair.second) }
    }

    @Test
    fun UX_AC5_body_styles_use_figtree_at_the_token_sizes_and_weights() {
        mapOf(
            "reason" to (typography.reason to Expected(figtree, 22, 700, 1.25.em)),
            "reason (long)" to (typography.reasonLong to Expected(figtree, 21, 700, 1.25.em)),
            "factorValue" to (typography.factorValue to Expected(figtree, 17, 700)),
            "button" to (typography.button to Expected(figtree, 17, 700)),
            "button (Start walk)" to (typography.buttonStart to Expected(figtree, 20, 700)),
            "button (Stop)" to (typography.buttonStop to Expected(figtree, 21, 800)),
            "body" to (typography.body to Expected(figtree, 15, 400, 1.5.em)),
            "body (large)" to (typography.bodyLarge to Expected(figtree, 16, 400, 1.5.em)),
            "label" to (typography.label to Expected(figtree, 13, 600)),
            "caption" to (typography.caption to Expected(figtree, 12, 500, 1.4.em)),
            "navLabel" to (typography.navLabel to Expected(figtree, 12, 600)),
            "navLabel (active)" to (typography.navLabelActive to Expected(figtree, 12, 700)),
            "axisLabel" to (typography.axisLabel to Expected(figtree, 11, 600)),
        ).forEach { (name, pair) -> assertStyle(name, pair.first, pair.second) }
    }

    @Test
    fun UX_AC5_material_slots_use_the_field_kit_styles() {
        with(FieldKitMaterialTypography) {
            assertSame(typography.timer, displayLarge)
            assertSame(typography.ratingWord, displayMedium)
            assertSame(typography.statLarge, displaySmall)
            assertSame(typography.screenTitle, headlineLarge)
            assertSame(typography.checkQuestion, headlineMedium)
            assertSame(typography.bestWindow, headlineSmall)
            assertSame(typography.reason, titleLarge)
            assertSame(typography.factorValue, titleMedium)
            assertSame(typography.bodyLarge, bodyLarge)
            assertSame(typography.body, bodyMedium)
            assertSame(typography.caption, bodySmall)
            assertSame(typography.button, labelLarge)
            assertSame(typography.label, labelMedium)
            assertSame(typography.axisLabel, labelSmall)
        }
    }

    @Test
    fun UX_AC65_every_font_is_a_bundled_resource_font() {
        val styles =
            with(typography) {
                listOf(
                    ratingWord,
                    ratingWordCaution,
                    timer,
                    screenTitle,
                    checkQuestion,
                    statLarge,
                    bestWindow,
                    reason,
                    reasonLong,
                    factorValue,
                    button,
                    buttonStart,
                    buttonStop,
                    body,
                    bodyLarge,
                    label,
                    caption,
                    navLabel,
                    navLabelActive,
                    axisLabel,
                )
            }

        styles.forEach { style ->
            val font = resourceFontOf(style)
            assertTrue("font ${font.resId} is bundled", font.resId in setOf(R.font.bricolage_grotesque, R.font.figtree))
        }
    }

    @Test
    fun UX_AC65_the_downloadable_google_fonts_library_is_not_on_the_classpath() {
        assertThrows(ClassNotFoundException::class.java) {
            Class.forName("androidx.compose.ui.text.googlefonts.GoogleFont")
        }
    }
}
