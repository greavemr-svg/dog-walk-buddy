package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner shapes from `tokens.json` → `radius` (ADR 0007). */
object FieldKitShapes {
    val hero = RoundedCornerShape(28.dp)
    val card = RoundedCornerShape(22.dp)

    /** Bottom sheets round their top corners only. */
    val sheet = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val startButton = RoundedCornerShape(22.dp)
    val button = RoundedCornerShape(18.dp)
    val input = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(percent = 50)
    val iconTile = RoundedCornerShape(14.dp)

    /** The best-window card uses 24, as its artboard does. */
    val bestWindowCard = RoundedCornerShape(24.dp)
}

/** The Material 3 shape slots, filled from the Field Kit radii. */
val FieldKitMaterialShapes =
    Shapes(
        small = FieldKitShapes.input,
        medium = FieldKitShapes.button,
        large = FieldKitShapes.card,
        extraLarge = FieldKitShapes.hero,
    )

/** Spacing from `tokens.json` → `space`. */
object FieldKitSpacing {
    val gutter = 16.dp
    val headerSide = 20.dp
    val cardPadding = 18.dp
    val heroPadding = 22.dp
    val stackGap = 12.dp
    val sectionGap = 18.dp
    val rowGap = 14.dp
}

/** Sizes from `tokens.json` → `size`. The accessibility floors are the existing MVP constants (AC51). */
object FieldKitSizes {
    val minTouchTarget = MinTouchTarget
    val startStopHeight = StartStopMinHeight
    val stopHeight = 76.dp
    val primaryButtonHeight = 56.dp
    val permissionButtonHeight = 60.dp
    val inputHeight = 52.dp
    val iconButton = 48.dp
    val ratingBadge = 60.dp

    /** A minimum: the bar grows with the label at large font sizes. */
    val bottomNavHeight = 78.dp
    val navIndicatorWidth = 60.dp
    val navIndicatorHeight = 32.dp
    val listRowMinHeight = 64.dp
    val avatar = 88.dp
    val iconNav = 22.dp
    val iconInline = 18.dp
    val iconSmall = 14.dp

    /** The 12-hour strip bar heights, so ratings differ by height as well as colour (AC53, AC61). */
    val barGo = 44.dp
    val barCaution = 28.dp
    val barAvoid = 14.dp
}
