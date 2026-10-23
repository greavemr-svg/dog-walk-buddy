package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit

/**
 * The app shell: the screens with the bottom navigation bar below them (AC9). The bar shows only on the three
 * top-level destinations; Recording, Walk detail, Privacy and About are full screen (AC12). The shell fills the
 * window with the current theme's background. Each screen applies the window insets that [content]'s padding
 * has not already consumed.
 */
@Composable
fun AppScaffold(
    navController: NavHostController,
    dogName: String?,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FieldKit.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (route in Routes.TOP_LEVEL) {
                FieldKitNavBar(
                    selectedRoute = route,
                    dogName = dogName,
                    onSelect = navController::navigateToTab,
                )
            }
        },
        content = content,
    )
}

/**
 * Shows a top-level destination as a tab: the back stack keeps only Today below it, so system Back from Walks or
 * the dog tab returns to Today, and Back on Today leaves the app (AC11). Each tab keeps its state when left.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
