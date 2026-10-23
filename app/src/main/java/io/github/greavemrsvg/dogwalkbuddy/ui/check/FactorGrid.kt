package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.check.Factor
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorCell
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorValue
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RatingPalette

/** Test-only: true on the factor cell the reason names (AC17). Read in the unmerged tree. */
val FactorHighlighted = SemanticsPropertyKey<Boolean>("FactorHighlighted")
private var SemanticsPropertyReceiver.factorHighlighted by FactorHighlighted

/** Test tag of a factor cell: `factor_air`, `factor_rain`, `factor_wind` or `factor_pavement`. */
fun Factor.testTag(): String = "factor_" + name.lowercase()

private val CellShape = RoundedCornerShape(14.dp)
private val HighlightRing = 2.dp

@StringRes
fun Factor.captionRes(): Int =
    when (this) {
        Factor.AIR -> R.string.check_factor_air
        Factor.RAIN -> R.string.check_factor_rain
        Factor.WIND -> R.string.check_factor_wind
        Factor.PAVEMENT -> R.string.check_factor_pavement
    }

@DrawableRes
private fun Factor.iconRes(): Int =
    when (this) {
        Factor.AIR -> R.drawable.ic_factor_air
        Factor.RAIN -> R.drawable.ic_factor_rain
        Factor.WIND -> R.drawable.ic_factor_wind
        Factor.PAVEMENT -> R.drawable.ic_factor_pavement
    }

/** The exact D3 text of a cell's value, for example "14°C", "0 mm/h", "9 mph", "Warm", "Not rated" or "No data". */
@Composable
fun FactorCell.valueText(): String =
    when (val shown = value) {
        is FactorValue.Whole ->
            when (factor) {
                Factor.AIR -> stringResource(R.string.check_factor_temperature_value, shown.value)
                Factor.RAIN -> stringResource(R.string.check_factor_rain_value, shown.value)
                Factor.WIND, Factor.PAVEMENT -> stringResource(R.string.check_factor_wind_value, shown.value)
            }
        is FactorValue.Pavement ->
            when (shown.rating) {
                Rating.GO -> stringResource(R.string.check_pavement_cool)
                Rating.CAUTION -> stringResource(R.string.check_pavement_warm)
                Rating.AVOID -> stringResource(R.string.check_pavement_hot)
            }
        FactorValue.NotRated -> stringResource(R.string.check_factor_not_rated)
        FactorValue.NoData -> stringResource(R.string.check_factor_no_data)
    }

/**
 * The four factor cells in one row (AC17). The highlighted cell gets the driver fill and a 2 dp ring, so the
 * highlight is not shown by colour alone. The whole card is spoken as one node, so these cells add no semantics.
 */
@Composable
fun FactorGridRow(
    cells: List<FactorCell>,
    palette: RatingPalette,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.forEach { cell ->
            val highlight = palette.driverHighlight
            val cellModifier =
                if (cell.highlighted) {
                    Modifier
                        .background(highlight?.background ?: palette.container, CellShape)
                        .border(HighlightRing, highlight?.ring ?: palette.onContainer, CellShape)
                } else {
                    Modifier
                }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag(cell.factor.testTag())
                        .semantics { factorHighlighted = cell.highlighted }
                        .then(cellModifier)
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    painter = painterResource(cell.factor.iconRes()),
                    contentDescription = null,
                    tint = palette.onContainerMuted,
                    modifier = Modifier.size(FieldKitSizes.iconInline),
                )
                Text(
                    text = cell.valueText(),
                    style = FieldKit.typography.factorValue,
                    color = palette.onContainer,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(cell.factor.captionRes()),
                    style = FieldKit.typography.caption,
                    color = palette.onContainerMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
