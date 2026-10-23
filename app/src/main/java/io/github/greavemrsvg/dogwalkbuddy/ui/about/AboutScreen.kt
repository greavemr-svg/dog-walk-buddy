package io.github.greavemrsvg.dogwalkbuddy.ui.about

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.common.BackButton
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing

private val TopBarGap = 8.dp
private val HeadlineSize = 30.sp
private val TableBorder = 1.dp
private val PanelGap = 8.dp

/**
 * How the weather is rated (AC56): the headline and intro, the thresholds table and the existing threshold sentences
 * below it (K5); then the guidance sources, the data panel and the open-source licences (AC57), including the
 * licences of the bundled fonts (AC8).
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = FieldKit.colors
    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FieldKitSpacing.gutter, vertical = FieldKitSpacing.stackGap),
            verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TopBarGap)) {
                BackButton(onClick = onBack, contentDescription = stringResource(R.string.about_back))
                Heading(R.string.about_title, style = FieldKit.typography.button)
            }
            Heading(R.string.about_headline, style = FieldKit.typography.screenTitle.copy(fontSize = HeadlineSize))
            Text(
                text = stringResource(R.string.about_intro),
                color = colors.onSurfaceVariant,
                style = FieldKit.typography.body,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface, FieldKitShapes.card)
                        .border(TableBorder, colors.outlineVariant, FieldKitShapes.card),
            ) {
                Heading(
                    R.string.about_thresholds_heading,
                    style = FieldKit.typography.factorValue,
                    modifier =
                        Modifier.padding(
                            start = FieldKitSpacing.cardPadding,
                            end = FieldKitSpacing.cardPadding,
                            top = FieldKitSpacing.rowGap,
                        ),
                )
                ThresholdsTable(modifier = Modifier.padding(vertical = PanelGap).testTag(THRESHOLDS_TABLE_TAG))
            }
            // The existing threshold sentences stay below the table, word for word (K5).
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
            // The muted data panel: the disclaimer (bold) and each attribution as its own unaltered text (K9).
            Column(
                verticalArrangement = Arrangement.spacedBy(PanelGap),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceMuted, FieldKitShapes.card)
                        .padding(FieldKitSpacing.cardPadding),
            ) {
                Text(
                    text = stringResource(R.string.check_disclaimer),
                    style = FieldKit.typography.body.copy(fontWeight = FontWeight.Bold),
                )
                PanelText(R.string.check_attribution)
                PanelText(R.string.map_attribution)
                PanelText(R.string.check_pollen_not_included)
            }
            Heading(R.string.about_licences_heading)
            Body(R.string.about_licences)
            Heading(R.string.about_licence_maplibre_heading)
            LicenceText(R.raw.licence_maplibre_native)
            Heading(R.string.about_licence_native_third_party_heading)
            Body(R.string.about_licence_native_third_party_intro)
            LicenceText(R.raw.licence_maplibre_native_android)
            Heading(R.string.about_licence_gestures_heading)
            LicenceText(R.raw.licence_maplibre_gestures)
            Heading(R.string.about_public_suffix_heading)
            Body(R.string.about_public_suffix_intro)
            LicenceText(R.raw.licence_okhttp_notice)
            LicenceText(R.raw.licence_mpl_2_0)
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
            Heading(R.string.about_font_bricolage_heading)
            LicenceText(R.raw.licence_ofl_bricolage_grotesque)
            Heading(R.string.about_font_figtree_heading)
            LicenceText(R.raw.licence_ofl_figtree)
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
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Text(text = stringResource(resId), style = style, modifier = modifier.semantics { heading() })
}

@Composable
private fun Body(
    @StringRes resId: Int,
) {
    Text(text = stringResource(resId), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun PanelText(
    @StringRes resId: Int,
) {
    Text(text = stringResource(resId), color = FieldKit.colors.onSurfaceVariant, style = FieldKit.typography.body)
}
