package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

/**
 * The Start walk control (AC26): full width, at least 72 dp high, radius 22, `primary` fill and `onPrimary` text,
 * with a decorative icon before the label, so the spoken label is the label alone.
 */
@Composable
fun StartWalkButton(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FieldKit.colors
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = FieldKitSizes.startStopHeight),
        shape = FieldKitShapes.startButton,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(StartIconSize))
        Spacer(Modifier.width(10.dp))
        Text(text = label, style = FieldKit.typography.buttonStart, textAlign = TextAlign.Center)
    }
}

private val StartIconSize = 26.dp

/**
 * The full-width primary action, such as the dog tab's Save (AC53): at least 56 dp high, radius 18, `primary` fill and
 * `onPrimary` text. Disabled, it uses the Material 3 disabled colours.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FieldKit.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = FieldKitSizes.primaryButtonHeight),
        shape = FieldKitShapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
    ) {
        Text(text = label, style = FieldKit.typography.button, textAlign = TextAlign.Center)
    }
}
