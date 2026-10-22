package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes

/**
 * The Field Kit bottom sheet (design, "Shared components"): a Material 3 modal sheet that opens fully expanded, with
 * top corners of 30 dp, a drag handle and the Field Kit scrim. It closes on a scrim tap, system Back or a drag down,
 * each of which calls [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldKitBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FieldKit.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = FieldKitShapes.sheet,
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        scrimColor = colors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.iconMuted) },
        content = content,
    )
}
