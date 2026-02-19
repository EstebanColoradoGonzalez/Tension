package com.estebancoloradogonzalez.tension.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.catalog.CreateExerciseScreen
import com.estebancoloradogonzalez.tension.ui.catalog.ExerciseDetailScreen
import com.estebancoloradogonzalez.tension.ui.catalog.ExerciseDictionaryScreen
import com.estebancoloradogonzalez.tension.ui.catalog.PlanVersionDetailScreen
import com.estebancoloradogonzalez.tension.ui.catalog.TrainingPlanScreen
import com.estebancoloradogonzalez.tension.ui.components.BottomNavigationBar
import com.estebancoloradogonzalez.tension.ui.deload.DeloadManagementScreen
import com.estebancoloradogonzalez.tension.ui.history.ExerciseHistoryScreen
import com.estebancoloradogonzalez.tension.ui.history.SessionDetailScreen
import com.estebancoloradogonzalez.tension.ui.history.SessionHistoryScreen
import com.estebancoloradogonzalez.tension.ui.home.HomeScreen
import com.estebancoloradogonzalez.tension.ui.metrics.MetricsScreen
import com.estebancoloradogonzalez.tension.ui.metrics.TrendScreen
import com.estebancoloradogonzalez.tension.ui.metrics.VolumeScreen
import com.estebancoloradogonzalez.tension.ui.onboarding.RegisterProfileScreen
import com.estebancoloradogonzalez.tension.ui.profile.ProfileScreen
import com.estebancoloradogonzalez.tension.ui.profile.WeightHistoryScreen
import com.estebancoloradogonzalez.tension.ui.session.ActiveSessionScreen
import com.estebancoloradogonzalez.tension.ui.session.RegisterSetScreen
import com.estebancoloradogonzalez.tension.ui.session.SessionSummaryScreen
import com.estebancoloradogonzalez.tension.ui.session.SubstituteExerciseScreen
import com.estebancoloradogonzalez.tension.ui.settings.SettingsScreen

@Composable
fun TensionNavHost(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

    when (startDestination) {
        StartDestination.LOADING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = stringResource(R.string.logo_content_description),
                        modifier = Modifier.size(120.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        else -> {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val showBottomBar = currentRoute != null &&
                currentRoute != NavigationRoutes.REGISTER &&
                !currentRoute.startsWith("active-session") &&
                !currentRoute.startsWith("register-set") &&
                !currentRoute.startsWith("substitute-exercise") &&
                !currentRoute.startsWith("session-summary") &&
                !(currentRoute.startsWith("exercise-detail") &&
                    navController.previousBackStackEntry?.destination?.route
                        ?.startsWith("active-session") == true) &&
                !(currentRoute.startsWith("exercise-history") &&
                    navController.previousBackStackEntry?.destination?.route
                        ?.startsWith("session-summary") == true)

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavigationBar(navController = navController)
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (startDestination == StartDestination.ONBOARDING) {
                        NavigationRoutes.REGISTER
                    } else {
                        NavigationRoutes.HOME
                    },
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(NavigationRoutes.REGISTER) {
                        RegisterProfileScreen(
                            onNavigateToHome = {
                                navController.navigate(NavigationRoutes.HOME) {
                                    popUpTo(NavigationRoutes.REGISTER) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(NavigationRoutes.HOME) {
                        HomeScreen(
                            onNavigateToAlerts = { /* TODO: HU-17 */ },
                            onNavigateToDeloadManagement = {
                                navController.navigate(NavigationRoutes.DELOAD_MANAGEMENT)
                            },
                            onNavigateToActiveSession = { sessionId ->
                                navController.navigate(
                                    NavigationRoutes.activeSessionRoute(sessionId),
                                )
                            },
                        )
                    }

                    composable(NavigationRoutes.PROFILE) {
                        ProfileScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToWeightHistory = {
                                navController.navigate(NavigationRoutes.WEIGHT_HISTORY)
                            },
                        )
                    }

                    composable(NavigationRoutes.WEIGHT_HISTORY) {
                        WeightHistoryScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(NavigationRoutes.SETTINGS) {
                        SettingsScreen(
                            onNavigateToProfile = {
                                navController.navigate(NavigationRoutes.PROFILE)
                            },
                        )
                    }

                    // Placeholder destinations for Bottom Navigation tabs
                    composable(NavigationRoutes.EXERCISE_DICTIONARY) {
                        ExerciseDictionaryScreen(
                            onNavigateToExerciseDetail = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseDetailRoute(exerciseId),
                                )
                            },
                            onNavigateToTrainingPlan = {
                                navController.navigate(NavigationRoutes.TRAINING_PLAN) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToCreateExercise = {
                                navController.navigate(NavigationRoutes.CREATE_EXERCISE)
                            },
                        )
                    }

                    composable(NavigationRoutes.CREATE_EXERCISE) {
                        CreateExerciseScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = NavigationRoutes.EXERCISE_DETAIL,
                        arguments = listOf(
                            navArgument("exerciseId") { type = NavType.LongType },
                        ),
                    ) {
                        ExerciseDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExerciseHistory = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseHistoryRoute(exerciseId),
                                )
                            },
                        )
                    }

                    composable(NavigationRoutes.TRAINING_PLAN) {
                        TrainingPlanScreen(
                            onNavigateToExerciseDictionary = {
                                navController.navigate(NavigationRoutes.EXERCISE_DICTIONARY) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToPlanVersionDetail = { moduleVersionId ->
                                navController.navigate(
                                    NavigationRoutes.planVersionDetailRoute(moduleVersionId),
                                )
                            },
                        )
                    }

                    composable(
                        route = NavigationRoutes.PLAN_VERSION_DETAIL,
                        arguments = listOf(
                            navArgument("moduleVersionId") { type = NavType.LongType },
                        ),
                    ) {
                        PlanVersionDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExerciseDetail = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseDetailRoute(exerciseId),
                                )
                            },
                        )
                    }

                    composable(
                        route = NavigationRoutes.EXERCISE_HISTORY,
                        arguments = listOf(
                            navArgument("exerciseId") { type = NavType.LongType },
                        ),
                    ) {
                        ExerciseHistoryScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExerciseDetail = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseDetailRoute(exerciseId),
                                )
                            },
                        )
                    }

                    composable(NavigationRoutes.SESSION_HISTORY) {
                        SessionHistoryScreen(
                            onNavigateToSessionDetail = { sessionId ->
                                navController.navigate(
                                    NavigationRoutes.sessionDetailRoute(sessionId),
                                )
                            },
                        )
                    }

                    composable(
                        route = NavigationRoutes.SESSION_DETAIL,
                        arguments = listOf(
                            navArgument("sessionId") { type = NavType.LongType },
                        ),
                    ) {
                        SessionDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExerciseHistory = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseHistoryRoute(exerciseId),
                                )
                            },
                        )
                    }

                    composable(NavigationRoutes.METRICS) {
                        MetricsScreen(
                            onNavigateToVolume = {
                                navController.navigate(NavigationRoutes.MUSCLE_VOLUME)
                            },
                            onNavigateToTrend = {
                                navController.navigate(NavigationRoutes.PROGRESSION_TREND)
                            },
                            onNavigateToExerciseHistory = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseHistoryRoute(exerciseId),
                                )
                            },
                        )
                    }

                    composable(NavigationRoutes.MUSCLE_VOLUME) {
                        VolumeScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(NavigationRoutes.PROGRESSION_TREND) {
                        TrendScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = NavigationRoutes.ACTIVE_SESSION,
                        arguments = listOf(
                            navArgument("sessionId") { type = NavType.LongType },
                        ),
                    ) {
                        ActiveSessionScreen(
                            onNavigateToRegisterSet = { sessionExerciseId ->
                                navController.navigate(
                                    NavigationRoutes.registerSetRoute(sessionExerciseId),
                                )
                            },
                            onNavigateToSubstitute = { sessionExerciseId ->
                                navController.navigate(
                                    NavigationRoutes.substituteExerciseRoute(sessionExerciseId),
                                )
                            },
                            onNavigateToExerciseDetail = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseDetailRoute(exerciseId),
                                )
                            },
                            onNavigateToSessionSummary = { sessionId ->
                                navController.navigate(
                                    NavigationRoutes.sessionSummaryRoute(sessionId),
                                ) {
                                    popUpTo(NavigationRoutes.HOME) { inclusive = false }
                                }
                            },
                            onNavigateToHome = {
                                navController.navigate(NavigationRoutes.HOME) {
                                    popUpTo(NavigationRoutes.HOME) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(
                        route = NavigationRoutes.REGISTER_SET,
                        arguments = listOf(
                            navArgument("sessionExerciseId") { type = NavType.LongType },
                        ),
                    ) {
                        RegisterSetScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = NavigationRoutes.SUBSTITUTE_EXERCISE,
                        arguments = listOf(
                            navArgument("sessionExerciseId") { type = NavType.LongType },
                        ),
                    ) {
                        SubstituteExerciseScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = NavigationRoutes.SESSION_SUMMARY,
                        arguments = listOf(
                            navArgument("sessionId") { type = NavType.LongType },
                        ),
                    ) {
                        SessionSummaryScreen(
                            onNavigateToHome = {
                                navController.navigate(NavigationRoutes.HOME) {
                                    popUpTo(NavigationRoutes.HOME) { inclusive = true }
                                }
                            },
                            onNavigateToExerciseHistory = { exerciseId ->
                                navController.navigate(
                                    NavigationRoutes.exerciseHistoryRoute(exerciseId),
                                )
                            },
                        )
                    }

                    composable(NavigationRoutes.DELOAD_MANAGEMENT) {
                        DeloadManagementScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title — ${stringResource(R.string.coming_soon)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
