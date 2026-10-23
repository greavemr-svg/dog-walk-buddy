package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorCell
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RatingPalette
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RatingPalettes

private val BadgeIconSize = 34.dp

/** The palette of [rating] (AC18). */
fun RatingPalettes.of(rating: Rating): RatingPalette =
    when (rating) {
        Rating.GO -> go
        Rating.CAUTION -> caution
        Rating.AVOID -> avoid
    }

/**
 * The hero card (AC17-AC19): the rating badge and word, the reason, a divider and the factor grid, on the rating's
 * container colour. The card is one accessibility node in a polite live region, spoken as "<Rating>. <Reason>."
 * (MVP AC50, K8) followed by the four cells as "<caption> <value>" joined with ", ".
 */
@Composable
fun RatingHeroCard(
    rating: Rating,
    reason: Reason,
    factors: List<FactorCell>,
    modifier: Modifier = Modifier,
) {
    val palette = FieldKit.ratingPalettes.of(rating)
    val label = stringResource(rating.labelRes())
    val reasonText = stringResource(reason.textRes())
    val cells = factors.map { stringResource(R.string.check_factor_spoken, stringResource(it.factor.captionRes()), it.valueText()) }
    val spoken =
        stringResource(R.string.check_rating_announcement, label, reasonText) +
            if (cells.isEmpty()) "" else " " + cells.joinToString(", ")
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(RATING_BLOCK_TAG)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                }.background(palette.container, FieldKitShapes.hero)
                .padding(FieldKitSpacing.heroPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(FieldKitSizes.ratingBadge).background(palette.badge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(rating.iconRes()),
                    contentDescription = null,
                    tint = palette.onBadge,
                    modifier = Modifier.size(BadgeIconSize).testTag(RATING_ICON_TAG_PREFIX + rating.iconRes()),
                )
            }
            Text(
                text = label,
                style = if (rating == Rating.CAUTION) FieldKit.typography.ratingWordCaution else FieldKit.typography.ratingWord,
                color = palette.onContainer,
            )
        }
        Text(
            text = reasonText,
            style = if (reason.isLong()) FieldKit.typography.reasonLong else FieldKit.typography.reason,
            color = palette.onContainer,
        )
        if (factors.isNotEmpty()) {
            HorizontalDivider(thickness = 1.dp, color = palette.divider)
            FactorGridRow(cells = factors, palette = palette)
        }
    }
}

private fun Reason.isLong(): Boolean = this == Reason.PAVEMENT_WARM || this == Reason.PAVEMENT_TOO_HOT

/** The loading placeholder (AC28): a muted hero-shaped card with only "Checking the weather", politely announced. */
@Composable
fun LoadingHeroCard(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(FieldKit.colors.surfaceMuted, FieldKitShapes.hero)
                .padding(horizontal = FieldKitSpacing.heroPadding, vertical = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.check_loading),
            style = FieldKit.typography.bodyLarge,
            color = FieldKit.colors.onSurface,
            modifier = Modifier.testTag(LOADING_TAG).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Test tag of the loading placeholder text (AC28). */
const val LOADING_TAG = "check_loading"
