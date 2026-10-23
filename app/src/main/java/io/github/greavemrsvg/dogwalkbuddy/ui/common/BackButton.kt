package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

private val BackIconSize = 24.dp
private val FloatingShadow = 4.dp

/**
 * The back control of a full-screen destination (AC12): a 48 dp circular icon button spoken as
 * [contentDescription] ("Back"). The [floating] variant has a `surface` fill and a soft shadow, so it stays visible
 * over a map. The icon takes the surrounding content colour, so it also suits the always-dark Recording screen.
 */
@Composable
fun BackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    val sized = modifier.size(FieldKitSizes.iconButton)
    val colors =
        if (floating) {
            IconButtonDefaults.iconButtonColors(
                containerColor = FieldKit.colors.surface,
                contentColor = FieldKit.colors.onSurface,
            )
        } else {
            IconButtonDefaults.iconButtonColors()
        }
    IconButton(
        onClick = onClick,
        colors = colors,
        modifier = if (floating) sized.shadow(FloatingShadow, CircleShape) else sized,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = contentDescription,
            modifier = Modifier.size(BackIconSize),
        )
    }
}
