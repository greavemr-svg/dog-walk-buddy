package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.home.HomeScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.profile.ProfileRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.profile.ProfileViewModel

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
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
            HomeScreen(onOpenProfile = { navController.navigate(Routes.PROFILE) })
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                viewModel = viewModel(factory = ProfileViewModel.factory(container)),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
