package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.about.AboutScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.check.BestWindowFormatter
import io.github.greavemrsvg.dogwalkbuddy.ui.check.CheckRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.check.CheckViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.history.DebugTools
import io.github.greavemrsvg.dogwalkbuddy.ui.history.HistoryRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.history.HistoryViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.history.WalkDateFormats
import io.github.greavemrsvg.dogwalkbuddy.ui.home.HomeScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.privacy.PrivacyRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.privacy.PrivacyViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.profile.ProfileRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.profile.ProfileViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.RecordingRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.RecordingViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.StartWalkRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.StartWalkViewModel
import io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail.WalkDetailRoute
import io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail.WalkDetailViewModel

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val ABOUT = "about"
    const val RECORDING = "recording"
    const val HISTORY = "history"
    const val PRIVACY = "privacy"
    const val WALK_ID_ARG = "id"
    const val WALK_DETAIL = "walk/{$WALK_ID_ARG}"

    fun walkDetail(walkId: Long) = "walk/$walkId"
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
                startWalkContent = {
                    StartWalkRoute(
                        viewModel = viewModel(factory = StartWalkViewModel.factory(container)),
                        onOpenRecording = { navController.navigate(Routes.RECORDING) },
                    )
                },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
            )
        }
        composable(Routes.RECORDING) {
            RecordingRoute(
                viewModel = viewModel(factory = RecordingViewModel.factory(container)),
                clock = container.clock,
                onClose = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            val context = LocalContext.current
            HistoryRoute(
                viewModel = viewModel(factory = HistoryViewModel.factory(container)),
                zoneId = container.zoneId,
                is24Hour = container.is24HourFormat(),
                onBack = { navController.popBackStack() },
                debugActions = remember(container) { DebugTools.historyActions(context, container) },
                onOpenWalk = { walkId -> navController.navigate(Routes.walkDetail(walkId)) },
            )
        }
        composable(
            Routes.WALK_DETAIL,
            arguments = listOf(navArgument(Routes.WALK_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val walkId = entry.arguments?.getLong(Routes.WALK_ID_ARG) ?: 0L
            WalkDetailRoute(
                viewModel = viewModel(factory = WalkDetailViewModel.factory(container, walkId)),
                routeMap = container.routeMap,
                formats = remember(container) { WalkDateFormats(container.zoneId, container.is24HourFormat()) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PRIVACY) {
            PrivacyRoute(
                viewModel = viewModel(factory = PrivacyViewModel.factory(container)),
                onBack = { navController.popBackStack() },
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
