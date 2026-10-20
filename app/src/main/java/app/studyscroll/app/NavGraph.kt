package app.studyscroll.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.studyscroll.ui.feed.FeedScreen
import app.studyscroll.ui.feed.FeedViewModel
import app.studyscroll.ui.home.HomeScreen
import app.studyscroll.ui.home.HomeViewModel
import app.studyscroll.ui.importpdf.PdfInspectScreen
import app.studyscroll.ui.importpdf.PdfInspectViewModel
import app.studyscroll.ui.providers.ProviderSettingsScreen
import app.studyscroll.ui.providers.ProviderSettingsViewModel
import app.studyscroll.ui.settings.SettingsScreen
import app.studyscroll.ui.settings.SettingsViewModel

object Destinations {
    const val HOME = "home"
    const val INSPECT = "inspect/{documentId}"
    const val FEED = "feed/{documentId}"
    const val PROVIDERS = "providers"
    const val SETTINGS = "settings"

    fun inspectRoute(documentId: String) = "inspect/$documentId"
    fun feedRoute(documentId: String) = "feed/$documentId"
}

@Composable
fun StudyScrollNavGraph(
    navController: NavHostController,
    app: StudyScrollApplication,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        modifier = modifier
    ) {
        composable(Destinations.HOME) {
            val homeViewModel = viewModel<HomeViewModel> {
                HomeViewModel(app.documentRepository)
            }
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToInspect = { docId ->
                    navController.navigate(Destinations.inspectRoute(docId))
                },
                onNavigateToFeed = { docId ->
                    navController.navigate(Destinations.feedRoute(docId))
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                }
            )
        }

        composable(
            route = Destinations.INSPECT,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val inspectViewModel = viewModel<PdfInspectViewModel> {
                PdfInspectViewModel(
                    documentId = documentId,
                    documentRepository = app.documentRepository,
                    lessonRepository = app.lessonRepository,
                    providerManager = app.providerManager,
                    pageTextDao = app.database.pageTextDao(),
                    settingsRepository = app.settingsRepository
                )
            }
            PdfInspectScreen(
                viewModel = inspectViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFeed = {
                    navController.navigate(Destinations.feedRoute(documentId)) {
                        popUpTo(Destinations.HOME)
                    }
                }
            )
        }

        composable(
            route = Destinations.FEED,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val feedViewModel = viewModel<FeedViewModel> {
                FeedViewModel(
                    documentId = documentId,
                    lessonRepository = app.lessonRepository,
                    documentRepository = app.documentRepository,
                    settingsRepository = app.settingsRepository,
                    narrationManager = app.narrationManager
                )
            }
            FeedScreen(
                viewModel = feedViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.PROVIDERS) {
            val providerViewModel = viewModel<ProviderSettingsViewModel> {
                ProviderSettingsViewModel(app.providerManager)
            }
            ProviderSettingsScreen(
                viewModel = providerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            val settingsViewModel = viewModel<SettingsViewModel> {
                SettingsViewModel(
                    settingsRepository = app.settingsRepository,
                    documentRepository = app.documentRepository,
                    lessonDao = app.database.lessonDao()
                )
            }
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProviders = {
                    navController.navigate(Destinations.PROVIDERS)
                }
            )
        }
    }
}
