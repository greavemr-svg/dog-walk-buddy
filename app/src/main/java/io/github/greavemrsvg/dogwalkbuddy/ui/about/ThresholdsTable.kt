package io.github.greavemrsvg.dogwalkbuddy.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.ui.check.iconRes
import io.github.greavemrsvg.dogwalkbuddy.ui.check.labelRes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LocalRatingColors

/** Test tag of the thresholds table node, which carries the collection semantics (AC56). */
const val THRESHOLDS_TABLE_TAG = "about_thresholds_table"

/** Test tag of the decorative icon in a rating column header (AC56, AC61). */
fun ratingHeaderIconTag(rating: Rating) = "about_rating_icon_${rating.name}"

private val CellPadding = 8.dp
private val EdgePadding = 14.dp
private val HeaderIconGap = 4.dp
private val CellTextSize = 13.sp
private const val FACTOR_COLUMN_WEIGHT = 0.9f

/** One cell's main text and, for the heat bands, the Sensitive value shown muted in brackets. */
internal data class ThresholdCell(
    @StringRes val text: Int,
    @StringRes val sensitive: Int? = null,
)

/** One table row: its header and the Go, Caution and Avoid cells. */
internal data class ThresholdRow(
    @StringRes val header: Int,
    val cells: List<ThresholdCell>,
)

private val temperatureCells =
    listOf(
        ThresholdCell(R.string.about_table_temp_go, R.string.about_table_temp_go_sensitive),
        ThresholdCell(R.string.about_table_temp_caution, R.string.about_table_temp_caution_sensitive),
        ThresholdCell(R.string.about_table_temp_avoid, R.string.about_table_temp_avoid_sensitive),
    )

/** The D11 rows, in the order Heat, Pavement, Rain, Wind, Cold, with the exact rule boundaries (K5). */
internal val thresholdRows =
    listOf(
        ThresholdRow(R.string.about_table_heat, temperatureCells),
        ThresholdRow(R.string.about_table_pavement, temperatureCells),
        ThresholdRow(
            R.string.about_table_rain,
            listOf(
                ThresholdCell(R.string.about_table_rain_go),
                ThresholdCell(R.string.about_table_rain_caution),
                ThresholdCell(R.string.about_table_rain_avoid),
            ),
        ),
        ThresholdRow(
            R.string.about_table_wind,
            listOf(
                ThresholdCell(R.string.about_table_wind_go),
                ThresholdCell(R.string.about_table_wind_caution),
                ThresholdCell(R.string.about_table_wind_avoid),
            ),
        ),
        ThresholdRow(
            R.string.about_table_cold,
            listOf(
                ThresholdCell(R.string.about_table_cold_go),
                ThresholdCell(R.string.about_table_cold_caution),
                ThresholdCell(R.string.about_table_cold_avoid),
            ),
        ),
    )

private val ratingColumns = listOf(Rating.GO, Rating.CAUTION, Rating.AVOID)

/**
 * The rating thresholds as a real table (AC56, AC61): a header row (Factor, Go, Caution, Avoid, each rating with its
 * icon and rating colour) and the D11 rows. The table has collection semantics, each cell its row and column, and each
 * data cell is spoken with its row and column headers, so TalkBack gives the context on every Android version.
 */
@Composable
fun ThresholdsTable(modifier: Modifier = Modifier) {
    val ratingColors = LocalRatingColors.current
    val columnHeaders = listOf(stringResource(R.string.about_table_factor)) + ratingColumns.map { stringResource(it.labelRes()) }
    Column(
        modifier =
            modifier.fillMaxWidth().semantics {
                collectionInfo = CollectionInfo(rowCount = thresholdRows.size + 1, columnCount = columnHeaders.size)
            },
    ) {
        TableRow {
            HeaderCell(row = 0, column = 0, weight = FACTOR_COLUMN_WEIGHT, start = true) {
                Text(
                    text = columnHeaders[0],
                    color = FieldKit.colors.onSurfaceVariant,
                    style = cellStyle(FontWeight.Bold),
                )
            }
            ratingColumns.forEachIndexed { index, rating ->
                val color = rating.headerColor(ratingColors.go, ratingColors.caution, ratingColors.avoid)
                HeaderCell(row = 0, column = index + 1, weight = 1f, end = index == ratingColumns.lastIndex) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Decorative: the word beside it names the rating; the icon makes it clear in greyscale (AC61).
                        Icon(
                            painter = painterResource(rating.iconRes()),
                            contentDescription = null,
                            tint = color,
                            modifier =
                                Modifier
                                    .padding(
                                        end = HeaderIconGap,
                                    ).size(FieldKitSizes.iconSmall)
                                    .testTag(ratingHeaderIconTag(rating)),
                        )
                        Text(text = columnHeaders[index + 1], color = color, style = cellStyle(FontWeight.ExtraBold))
                    }
                }
            }
        }
        thresholdRows.forEachIndexed { rowIndex, row ->
            HorizontalDivider(color = FieldKit.colors.divider)
            val rowHeader = stringResource(row.header)
            TableRow {
                HeaderCell(row = rowIndex + 1, column = 0, weight = FACTOR_COLUMN_WEIGHT, start = true) {
                    Text(text = rowHeader, color = FieldKit.colors.onSurface, style = cellStyle(FontWeight.Bold))
                }
                row.cells.forEachIndexed { cellIndex, cell ->
                    DataCell(
                        cell = cell,
                        row = rowIndex + 1,
                        column = cellIndex + 1,
                        rowHeader = rowHeader,
                        columnHeader = columnHeaders[cellIndex + 1],
                        end = cellIndex == row.cells.lastIndex,
                    )
                }
            }
        }
    }
}

private fun Rating.headerColor(
    go: Color,
    caution: Color,
    avoid: Color,
) = when (this) {
    Rating.GO -> go
    Rating.CAUTION -> caution
    Rating.AVOID -> avoid
}

@Composable
private fun cellStyle(weight: FontWeight) = FieldKit.typography.body.copy(fontSize = CellTextSize, fontWeight = weight)

@Composable
private fun TableRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), content = content)
}

private fun Modifier.cellPadding(
    start: Boolean,
    end: Boolean,
) = padding(
    start = if (start) EdgePadding else CellPadding / 2,
    end = if (end) EdgePadding else CellPadding / 2,
    top = CellPadding,
    bottom = CellPadding,
)

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(
    row: Int,
    column: Int,
    weight: Float,
    start: Boolean = false,
    end: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .weight(weight)
                .semantics(mergeDescendants = true) {
                    heading()
                    collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                }.cellPadding(start, end),
    ) {
        content()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DataCell(
    cell: ThresholdCell,
    row: Int,
    column: Int,
    rowHeader: String,
    columnHeader: String,
    end: Boolean,
) {
    val main = stringResource(cell.text)
    val sensitive = cell.sensitive?.let { stringResource(it) }
    val shown = if (sensitive == null) main else "$main $sensitive"
    val spoken = stringResource(R.string.about_table_cell_spoken, rowHeader, columnHeader, shown)
    val muted = FieldKit.colors.textTertiary
    Text(
        text =
            buildAnnotatedString {
                append(main)
                if (sensitive != null) {
                    append(" ")
                    withStyle(SpanStyle(color = muted)) { append(sensitive) }
                }
            },
        color = FieldKit.colors.onSurface,
        style = cellStyle(FontWeight.Normal),
        modifier =
            Modifier
                .weight(1f)
                .semantics {
                    contentDescription = spoken
                    collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                }.cellPadding(start = false, end = end),
    )
}
