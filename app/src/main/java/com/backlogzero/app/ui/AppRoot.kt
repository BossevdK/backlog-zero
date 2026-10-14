package com.backlogzero.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.backlogzero.app.BacklogZeroApp
import com.backlogzero.app.domain.ItemStatus
import com.backlogzero.app.ui.auth.AuthScreen
import com.backlogzero.app.ui.detail.DetailScreen
import com.backlogzero.app.ui.edit.EditScreen
import com.backlogzero.app.ui.home.HomeScreen
import com.backlogzero.app.ui.library.LibraryScreen
import com.backlogzero.app.ui.pick.PickScreen
import com.backlogzero.app.ui.profile.ProfileScreen
import com.backlogzero.app.ui.stats.StatsScreen

private object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LIBRARY_PATTERN = "library?status={status}"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit?id={id}"
    const val PICK = "pick"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.LIBRARY_PATTERN, "Bibliotheek", Icons.Filled.VideoLibrary),
    Tab(Routes.STATS, "Statistieken", Icons.Filled.BarChart),
    Tab(Routes.PROFILE, "Profiel", Icons.Filled.Person),
)

@Composable
fun AppRoot() {
    val app = LocalContext.current.applicationContext as BacklogZeroApp
    val userId by app.container.auth.currentUserId.collectAsStateWithLifecycle()
    if (userId == null) {
        AuthScreen()
    } else {
        // Nieuwe navigatiestatus per gebruiker, zodat er niets van een vorig account blijft hangen.
        key(userId) { MainNav() }
    }
}

private fun enter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition =
    { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } }

private fun exit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition =
    { fadeOut(tween(160)) }

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    fun openTab(route: String) {
        nav.navigate(if (route == Routes.LIBRARY_PATTERN) Routes.LIBRARY else route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenItem = { nav.navigate("detail/$it") },
                    onAdd = { nav.navigate("edit") },
                    onPick = { nav.navigate(Routes.PICK) },
                    onOpenLibrary = { status ->
                        nav.navigate("library?status=${status.key}") {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                Routes.LIBRARY_PATTERN,
                arguments = listOf(navArgument("status") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val status = entry.arguments?.getString("status")?.let(ItemStatus::fromKey)
                LibraryScreen(status, onOpenItem = { nav.navigate("detail/$it") }, onAdd = { nav.navigate("edit") })
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.PROFILE) { ProfileScreen() }

            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                enterTransition = enter(), exitTransition = exit(), popEnterTransition = enter(), popExitTransition = exit(),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                DetailScreen(id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("edit?id=$it") })
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
                enterTransition = enter(), exitTransition = exit(), popEnterTransition = enter(), popExitTransition = exit(),
            ) { entry ->
                val id = entry.arguments?.getString("id")
                EditScreen(
                    itemId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = { savedId ->
                        if (id == null) {
                            nav.navigate("detail/$savedId") { popUpTo(Routes.EDIT) { inclusive = true } }
                        } else {
                            nav.popBackStack()
                        }
                    },
                )
            }
            composable(
                Routes.PICK,
                enterTransition = enter(), exitTransition = exit(), popEnterTransition = enter(), popExitTransition = exit(),
            ) {
                PickScreen(
                    onBack = { nav.popBackStack() },
                    onOpenItem = { nav.navigate("detail/$it") { popUpTo(Routes.PICK) { inclusive = true } } },
                )
            }
        }
    }
}
