package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

private val BorderWidth = 1.5.dp
private val IconSize = 22.dp
private val IconGap = 8.dp

/**
 * An outlined destructive action (design, "Shared components"): full width, at least 56 dp high, radius 18, on
 * `surface` with a 1.5 dp `errorOutline` border, and an `errorText` label after a decorative delete icon. The label is
 * the button's accessible name.
 */
@Composable
fun DestructiveOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FieldKit.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = FieldKitSizes.primaryButtonHeight),
        shape = FieldKitShapes.button,
        border = BorderStroke(BorderWidth, colors.errorOutline),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = colors.surface,
                contentColor = colors.errorText,
            ),
    ) {
        Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(IconSize))
        Spacer(Modifier.size(IconGap))
        Text(text = label, style = FieldKit.typography.button)
    }
}
