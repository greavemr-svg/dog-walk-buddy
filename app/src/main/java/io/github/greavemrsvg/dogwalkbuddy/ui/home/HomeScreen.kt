package io.github.greavemrsvg.dogwalkbuddy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/**
 * The first screen: the check card at the top, then the full-width Start walk control, then the links, so the
 * primary actions are on the first screen and usable one-handed.
 */
@Composable
fun HomeScreen(
    checkContent: @Composable () -> Unit,
    startWalkContent: @Composable () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            checkContent()
            startWalkContent()
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.home_history))
            }
            OutlinedButton(onClick = onOpenProfile, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.home_dog_profile))
            }
            OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.home_privacy))
            }
            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.home_about))
            }
        }
    }
}
