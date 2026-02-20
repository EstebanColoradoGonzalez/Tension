package com.estebancoloradogonzalez.tension.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.estebancoloradogonzalez.tension.R
import com.estebancoloradogonzalez.tension.ui.navigation.NavigationRoutes

private data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val childRoutes: Set<String> = emptySet(),
    val childRoutePrefixes: Set<String> = emptySet(),
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem(
            route = NavigationRoutes.HOME,
            labelResId = R.string.nav_home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            childRoutePrefixes = setOf(
                "alert-center",
                "alert-detail",
            ),
        ),
        BottomNavItem(
            route = NavigationRoutes.EXERCISE_DICTIONARY,
            labelResId = R.string.nav_dictionary,
            selectedIcon = Icons.Filled.MenuBook,
            unselectedIcon = Icons.Outlined.MenuBook,
            childRoutes = setOf(
                NavigationRoutes.TRAINING_PLAN,
                NavigationRoutes.CREATE_EXERCISE,
            ),
            childRoutePrefixes = setOf(
                "exercise-detail",
                "plan-version-detail",
            ),
        ),
        BottomNavItem(
            route = NavigationRoutes.SESSION_HISTORY,
            labelResId = R.string.nav_history,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            childRoutePrefixes = setOf(
                "exercise-history",
                "session-detail",
            ),
        ),
        BottomNavItem(
            route = NavigationRoutes.METRICS,
            labelResId = R.string.nav_metrics,
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
            childRoutes = setOf(
                NavigationRoutes.MUSCLE_VOLUME,
                NavigationRoutes.PROGRESSION_TREND,
            ),
        ),
        BottomNavItem(
            route = NavigationRoutes.SETTINGS,
            labelResId = R.string.nav_settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            childRoutes = setOf(
                NavigationRoutes.PROFILE,
                NavigationRoutes.WEIGHT_HISTORY,
            ),
        ),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route ||
                currentRoute in item.childRoutes ||
                item.childRoutePrefixes.any { prefix ->
                    currentRoute?.startsWith(prefix) == true
                }
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelResId),
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelResId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
