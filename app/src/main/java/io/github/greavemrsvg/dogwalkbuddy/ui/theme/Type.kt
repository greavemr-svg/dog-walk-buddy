package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R

/**
 * The Field Kit type scale (AC5): one style per token in `tokens.json` → `type.scale`, named as the token.
 * Display styles use the bundled Bricolage Grotesque and body styles the bundled Figtree (ADR 0006). Both are
 * variable fonts in `res/font`, so nothing is fetched over the network. The `*Variant` styles are the sizes and
 * weights the token notes give for particular uses.
 */
@Immutable
data class FieldKitTypography(
    val ratingWord: TextStyle,
    /** "Caution" at 52 so it fits on one line at the default size. */
    val ratingWordCaution: TextStyle,
    val timer: TextStyle,
    val screenTitle: TextStyle,
    val checkQuestion: TextStyle,
    val statLarge: TextStyle,
    val bestWindow: TextStyle,
    val reason: TextStyle,
    /** The long pavement reasons at 21. */
    val reasonLong: TextStyle,
    val factorValue: TextStyle,
    val button: TextStyle,
    val buttonStart: TextStyle,
    val buttonStop: TextStyle,
    val body: TextStyle,
    val bodyLarge: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val navLabel: TextStyle,
    val navLabelActive: TextStyle,
    val axisLabel: TextStyle,
)

/** Bricolage Grotesque's optical-size axis runs from 12 to 96. */
private const val MIN_OPTICAL_SIZE = 12f
private const val MAX_OPTICAL_SIZE = 96f
private const val OPTICAL_SIZE_AXIS = "opsz"

/** A single-font family on a bundled variable font, set to [weight] and, if given, an optical size. */
private fun variableFamily(
    @FontRes resId: Int,
    weight: Int,
    opticalSize: Float? = null,
): FontFamily {
    val axes =
        listOfNotNull(
            FontVariation.weight(weight),
            opticalSize?.let { FontVariation.Setting(OPTICAL_SIZE_AXIS, it.coerceIn(MIN_OPTICAL_SIZE, MAX_OPTICAL_SIZE)) },
        )
    return FontFamily(
        Font(resId = resId, weight = FontWeight(weight), variationSettings = FontVariation.Settings(*axes.toTypedArray())),
    )
}

/** A display style: Bricolage Grotesque at the optical size of its own point size (ADR 0006). */
private fun display(
    size: Int,
    weight: Int,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    tabularNumbers: Boolean = false,
) = TextStyle(
    fontFamily = variableFamily(R.font.bricolage_grotesque, weight, opticalSize = size.toFloat()),
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabularNumbers) TABULAR_NUMBERS else null,
)

/** A body style: Figtree. */
private fun body(
    size: Int,
    weight: Int,
    lineHeight: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = variableFamily(R.font.figtree, weight),
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight,
)

private const val TABULAR_NUMBERS = "tnum"

private const val REGULAR = 400
private const val MEDIUM = 500
private const val SEMI_BOLD = 600
private const val BOLD = 700
private const val EXTRA_BOLD = 800

val DefaultFieldKitTypography =
    FieldKitTypography(
        ratingWord = display(size = 60, weight = EXTRA_BOLD, lineHeight = 0.95.em, letterSpacing = (-0.02).em),
        ratingWordCaution = display(size = 52, weight = EXTRA_BOLD, lineHeight = 0.95.em, letterSpacing = (-0.02).em),
        timer = display(size = 92, weight = BOLD, tabularNumbers = true),
        screenTitle = display(size = 32, weight = EXTRA_BOLD, lineHeight = 1.1.em),
        checkQuestion = display(size = 27, weight = BOLD, lineHeight = 1.1.em),
        statLarge = display(size = 36, weight = BOLD, tabularNumbers = true),
        bestWindow = display(size = 24, weight = BOLD),
        reason = body(size = 22, weight = BOLD, lineHeight = 1.25.em),
        reasonLong = body(size = 21, weight = BOLD, lineHeight = 1.25.em),
        factorValue = body(size = 17, weight = BOLD),
        button = body(size = 17, weight = BOLD),
        buttonStart = body(size = 20, weight = BOLD),
        buttonStop = body(size = 21, weight = EXTRA_BOLD),
        body = body(size = 15, weight = REGULAR, lineHeight = 1.5.em),
        bodyLarge = body(size = 16, weight = REGULAR, lineHeight = 1.5.em),
        label = body(size = 13, weight = SEMI_BOLD),
        caption = body(size = 12, weight = MEDIUM, lineHeight = 1.4.em),
        navLabel = body(size = 12, weight = SEMI_BOLD),
        navLabelActive = body(size = 12, weight = BOLD),
        axisLabel = body(size = 11, weight = SEMI_BOLD),
    )

/** The Material 3 slots each Field Kit style also fills, so Material components use the same scale. */
fun FieldKitTypography.toMaterialTypography(): Typography =
    Typography(
        displayLarge = timer,
        displayMedium = ratingWord,
        displaySmall = statLarge,
        headlineLarge = screenTitle,
        headlineMedium = checkQuestion,
        headlineSmall = bestWindow,
        titleLarge = reason,
        titleMedium = factorValue,
        bodyLarge = bodyLarge,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = button,
        labelMedium = label,
        labelSmall = axisLabel,
    )

val LocalFieldKitTypography = staticCompositionLocalOf { DefaultFieldKitTypography }

val FieldKitMaterialTypography: Typography = DefaultFieldKitTypography.toMaterialTypography()
