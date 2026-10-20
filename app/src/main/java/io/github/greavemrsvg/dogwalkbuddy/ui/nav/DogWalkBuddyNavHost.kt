package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.about.AboutScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.check.BestWindowFormatter
import io.github.greavemrsvg.dogwalkbuddy.ui.check.CheckRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.check.CheckViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.home.HomeScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.profile.ProfileRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.profile.ProfileViewModel

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val ABOUT = "about"
}

/** The app's screens (design, Screens and navigation). Later tasks add their routes here. */
@Composable
fun DogWalkBuddyNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            val resources = LocalResources.current
            val windowFormatter =
                remember(resources, container) { BestWindowFormatter(resources, container.zoneId, container.is24HourFormat) }
            HomeScreen(
                checkContent = {
                    CheckRoute(
                        viewModel = viewModel(factory = CheckViewModel.factory(container)),
                        windowFormatter = windowFormatter,
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                        onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    )
                },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                viewModel = viewModel(factory = ProfileViewModel.factory(container)),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
