package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing

/** Test tag of the confirm sheet's decorative delete icon. */
const val CONFIRM_SHEET_ICON_TAG = "confirm_sheet_icon"

private val IconCircleSize = 56.dp
private val IconSize = 28.dp
private const val ICON_CIRCLE_ALPHA = 0.12f
private val TitleSize = 26.sp
private val SheetBottomPadding = 24.dp

/**
 * A destructive confirmation on the Field Kit bottom sheet (design, "Shared components"): a tinted delete-icon circle,
 * the [title] as a heading, the [message], then a full-width destructive [confirmLabel] button and a [cancelLabel]
 * button, each at least 56 dp high. Cancel, a scrim tap, system Back or a drag down all call [onCancel].
 */
@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = FieldKit.colors
    FieldKitBottomSheet(onDismissRequest = onCancel) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FieldKitSpacing.headerSide)
                    .padding(bottom = SheetBottomPadding)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(IconCircleSize)
                        .background(colors.error.copy(alpha = ICON_CIRCLE_ALPHA), CircleShape)
                        .testTag(CONFIRM_SHEET_ICON_TAG),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = colors.errorText,
                    modifier = Modifier.size(IconSize),
                )
            }
            Text(
                text = title,
                style = FieldKit.typography.screenTitle.copy(fontSize = TitleSize),
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = message, style = FieldKit.typography.body, color = colors.onSurfaceVariant)
            Spacer(Modifier.size(FieldKitSpacing.stackGap))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = FieldKitSizes.primaryButtonHeight),
                shape = FieldKitShapes.button,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
            ) {
                Text(confirmLabel, style = FieldKit.typography.button)
            }
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().heightIn(min = FieldKitSizes.primaryButtonHeight),
                shape = FieldKitShapes.button,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceMuted, contentColor = colors.onSurface),
            ) {
                Text(cancelLabel, style = FieldKit.typography.button)
            }
        }
    }
}
