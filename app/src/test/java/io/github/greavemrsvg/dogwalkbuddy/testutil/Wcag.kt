package io.github.greavemrsvg.dogwalkbuddy.testutil

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** WCAG 2.1 contrast ratio between two opaque colours. */
object Wcag {
    fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val lighter = maxOf(first.luminance(), second.luminance()).toDouble()
        val darker = minOf(first.luminance(), second.luminance()).toDouble()
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private const val LUMINANCE_OFFSET = 0.05
}
