package io.github.greavemrsvg.dogwalkbuddy.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/** The rating thresholds, the guidance sources, the data attribution and the open-source licences (AC26, AC27). */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.about_back))
            }
            Heading(R.string.about_title, MaterialTheme.typography.headlineMedium)
            Body(R.string.check_disclaimer)
            Heading(R.string.about_thresholds_heading)
            Body(R.string.about_heat)
            Body(R.string.about_pavement)
            Body(R.string.about_rain)
            Body(R.string.about_wind)
            Body(R.string.about_cold)
            Body(R.string.about_overall)
            Heading(R.string.about_sources_heading)
            Body(R.string.about_sources_intro)
            Body(R.string.about_source_vets_now)
            Body(R.string.about_source_four_paws)
            Body(R.string.about_source_beaufort)
            Heading(R.string.about_data_heading)
            Body(R.string.check_attribution)
            Body(R.string.check_pollen_not_included)
            Heading(R.string.about_licences_heading)
            Body(R.string.about_licences)
        }
    }
}

@Composable
private fun Heading(
    @StringRes resId: Int,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Text(text = stringResource(resId), style = style, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(
    @StringRes resId: Int,
) {
    Text(text = stringResource(resId), style = MaterialTheme.typography.bodyLarge)
}
