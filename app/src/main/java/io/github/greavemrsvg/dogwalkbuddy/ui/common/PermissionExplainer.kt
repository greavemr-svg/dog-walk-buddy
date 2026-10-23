package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

/** Test tag of the decorative illustration at the top of a permission explanation (AC30, AC32). */
const val PERMISSION_ILLUSTRATION_TAG = "permission_illustration"

/** The illustration height in the artboard (CheckPermission). */
val PermissionIllustrationHeight = 220.dp

private val IllustrationShape = RoundedCornerShape(32.dp)
private val PermissionButtonShape = RoundedCornerShape(20.dp)
private val BadgeSize = 52.dp
private val BadgeIconSize = 26.dp
private val InnerCircleSize = 112.dp
private val OuterRingRadius = 150.dp
private val MiddleRingRadius = 100.dp
private val BulletIconSize = 22.dp
private val SectionGap = 22.dp

// The rings are drawn in `primary` at these alphas, so they follow the theme in light and dark (design, Today).
private const val OUTER_RING_ALPHA = 0.25f
private const val MIDDLE_RING_ALPHA = 0.35f
private const val INNER_FILL_ALPHA = 0.18f
private val RingStroke = 1.5.dp
private val DashedStroke = 2.dp
private val DashOn = 6.dp
private val DashOff = 5.dp

/**
 * The permission explanation pattern (design, "Shared components"): a decorative illustration, a heading, an
 * optional body, check-mark bullets, a primary button at least 60 dp high and an optional link button. The heading
 * is a polite live region, so the explanation is announced when it appears.
 */
@Composable
fun PermissionExplainer(
    @DrawableRes illustrationIcon: Int,
    heading: String,
    buttonLabel: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    bullets: List<String> = emptyList(),
    linkLabel: String? = null,
    onLink: () -> Unit = {},
    illustrationHeight: Dp = PermissionIllustrationHeight,
) {
    val colors = FieldKit.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        PermissionIllustration(illustrationIcon, Modifier.fillMaxWidth().height(illustrationHeight))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = heading,
                style = FieldKit.typography.screenTitle.copy(fontSize = 30.sp),
                color = colors.onSurface,
                modifier =
                    Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            body?.let { Text(it, style = FieldKit.typography.bodyLarge, color = colors.onSurfaceVariant) }
        }
        if (bullets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { bullets.forEach { Bullet(it) } }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = onButton,
                modifier = Modifier.fillMaxWidth().heightIn(min = FieldKitSizes.permissionButtonHeight),
                shape = PermissionButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            ) {
                Text(buttonLabel, style = FieldKit.typography.button.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
            }
            if (linkLabel != null) {
                TextButton(
                    onClick = onLink,
                    modifier = Modifier.fillMaxWidth().heightIn(min = FieldKitSizes.minTouchTarget),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
                ) {
                    Text(linkLabel, style = FieldKit.typography.button.copy(fontSize = 15.sp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = FieldKit.colors.primary,
            modifier = Modifier.size(BulletIconSize),
        )
        Text(text, style = FieldKit.typography.body, color = FieldKit.colors.onSurface)
    }
}

/**
 * Concentric rings around a badge holding [icon], drawn from theme tokens so no placeholder art is shipped (pack
 * rule 6). It is decorative: hidden from accessibility services (AC30, AC32, AC58).
 */
@Composable
private fun PermissionIllustration(
    @DrawableRes icon: Int,
    modifier: Modifier,
) {
    val colors = FieldKit.colors
    Box(
        modifier =
            modifier
                .testTag(PERMISSION_ILLUSTRATION_TAG)
                .semantics { hideFromAccessibility() }
                .clip(IllustrationShape)
                .background(colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ring = Stroke(width = RingStroke.toPx())
            drawCircle(colors.primary.copy(alpha = OUTER_RING_ALPHA), radius = OuterRingRadius.toPx(), style = ring)
            drawCircle(colors.primary.copy(alpha = MIDDLE_RING_ALPHA), radius = MiddleRingRadius.toPx(), style = ring)
            val inner = InnerCircleSize.toPx() / 2
            drawCircle(colors.primary.copy(alpha = INNER_FILL_ALPHA), radius = inner)
            drawCircle(
                colors.primary,
                radius = inner,
                style =
                    Stroke(
                        width = DashedStroke.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx())),
                    ),
            )
        }
        Box(
            modifier = Modifier.size(BadgeSize).background(colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(BadgeIconSize),
            )
        }
    }
}
