package io.github.greavemrsvg.dogwalkbuddy.ui.about

import androidx.annotation.RawRes
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
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
            Heading(R.string.about_licence_maplibre_heading)
            LicenceText(R.raw.licence_maplibre_native)
            Heading(R.string.about_licence_gestures_heading)
            LicenceText(R.raw.licence_maplibre_gestures)
            Heading(R.string.about_apache_libraries_heading)
            Body(R.string.about_apache_libraries_intro)
            Body(R.string.about_apache_lib_androidx)
            Body(R.string.about_apache_lib_compose)
            Body(R.string.about_apache_lib_kotlin)
            Body(R.string.about_apache_lib_okhttp)
            Body(R.string.about_apache_lib_maplibre)
            Body(R.string.about_apache_lib_other)
            Heading(R.string.about_apache_text_heading)
            LicenceText(R.raw.licence_apache_2_0)
        }
    }
}

/** A licence text shown exactly as shipped, one paragraph at a time so that long texts lay out cheaply. */
@Composable
private fun LicenceText(
    @RawRes resId: Int,
) {
    val resources = LocalResources.current
    val paragraphs =
        remember(resId) {
            resources
                .openRawResource(resId)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .replace("\r\n", "\n")
                .split("\n\n")
                .filter { it.isNotBlank() }
        }
    paragraphs.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
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
