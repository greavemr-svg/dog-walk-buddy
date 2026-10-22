package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing

/**
 * Today, the first tab: the scrolling check content, then the full-width Start walk control pinned below it, so the
 * primary actions are on the first screen and usable one-handed. Other screens are reached through the bottom
 * navigation bar (AC10).
 */
@Composable
fun TodayScreen(
    checkContent: @Composable () -> Unit,
    startWalkContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = FieldKit.colors.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // Only the check content scrolls. Start walk stays pinned above the bottom navigation bar, so it is
            // visible without scrolling at every font size (AC26, AC27).
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(TODAY_SCROLL_TAG)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = FieldKitSpacing.gutter)
                        .padding(bottom = FieldKitSpacing.gutter),
            ) {
                checkContent()
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FieldKitSpacing.gutter, vertical = 12.dp)) {
                startWalkContent()
            }
        }
    }
}

/** Test tag of Today's scrolling content, the viewport that "without scrolling" refers to (AC25). */
const val TODAY_SCROLL_TAG = "today_scroll"
