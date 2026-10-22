package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Today, the first tab: the check card, then the full-width Start walk control, so the primary actions are on the
 * first screen and usable one-handed. Other screens are reached through the bottom navigation bar (AC10).
 */
@Composable
fun TodayScreen(
    checkContent: @Composable () -> Unit,
    startWalkContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            checkContent()
            startWalkContent()
        }
    }
}
