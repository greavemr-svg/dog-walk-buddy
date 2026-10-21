package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * The shared way to show a failed action over the screen where the user is. The snackbar is a polite live region,
 * so it is announced, and it stays until the user dismisses it. [onDismissed] is called when it closes, so the
 * ViewModel can clear its failure flag.
 */
@Composable
fun ErrorSnackbarEffect(
    host: SnackbarHostState,
    show: Boolean,
    message: String,
    dismissLabel: String,
    onDismissed: () -> Unit,
) {
    LaunchedEffect(show) {
        if (show) {
            host.showSnackbar(message, actionLabel = dismissLabel, duration = SnackbarDuration.Indefinite)
            onDismissed()
        }
    }
}
