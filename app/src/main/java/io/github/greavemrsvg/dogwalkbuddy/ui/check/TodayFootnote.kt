package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

/** What the footnote adds in a rated state (AC24). Null in the other states, which show no sensitivity or pollen line. */
data class FootnoteRatedLines(
    val usingDefaultSensitivity: Boolean,
    val sensitiveDogName: String?,
)

/**
 * The footnote block (AC24): the disclaimer after an info icon, the "About this advice" button, then in rated states
 * the sensitivity line and the pollen notice, then the attribution in every state. Each text is complete and on its
 * own line, never joined with " · " (K2, K9).
 */
@Composable
fun TodayFootnote(
    rated: FootnoteRatedLines?,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FieldKit.colors
    val small = FieldKit.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp).size(FieldKitSizes.iconInline),
            )
            Text(stringResource(R.string.check_disclaimer), style = small, color = colors.onSurfaceVariant)
        }
        FootnoteButton(stringResource(R.string.home_about), onOpenAbout, bold = true)
        if (rated != null) {
            if (rated.sensitiveDogName != null) {
                Text(
                    stringResource(R.string.check_sensitive_thresholds, rated.sensitiveDogName),
                    style = small,
                    color = colors.onSurfaceVariant,
                )
            }
            if (rated.usingDefaultSensitivity) {
                FootnoteButton(stringResource(R.string.check_default_sensitivity), onOpenProfile, bold = false)
            }
            Text(stringResource(R.string.check_pollen_not_included), style = small, color = colors.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.check_attribution),
            style = FieldKit.typography.body.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = colors.textTertiary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun FootnoteButton(
    text: String,
    onClick: () -> Unit,
    bold: Boolean,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = FieldKitSizes.minTouchTarget),
        // A pill shape would clip the first letter of each wrapped line, as the text starts at the left edge.
        shape = RectangleShape,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = FieldKit.colors.primary),
    ) {
        Text(
            text,
            style = FieldKit.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
            fontWeight = if (bold) FontWeight.Bold else null,
        )
    }
}
