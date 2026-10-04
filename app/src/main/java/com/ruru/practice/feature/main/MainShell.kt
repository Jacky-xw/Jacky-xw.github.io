package com.ruru.practice.feature.main

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ruru.practice.feature.home.HomeScreen
import com.ruru.practice.feature.learning.LearningScreen
import com.ruru.practice.feature.navigation.BottomNavItem
import com.ruru.practice.feature.observation.ObservationScreen
import com.ruru.practice.feature.practice.PracticeScreen
import com.ruru.practice.feature.reflection.ReflectionScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("ruru_practice", Context.MODE_PRIVATE) }
    var firstRunDone by remember { mutableStateOf(preferences.getBoolean("first_run_done", false)) }
    if (!firstRunDone) {
        FirstRunScreen(onFinish = {
            preferences.edit().putBoolean("first_run_done", true).apply()
            firstRunDone = true
        })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navigateTopLevel: (BottomNavItem) -> Unit = { item ->
        navigateToTopLevel(navController, item.route)
    }
    val currentTitle = BottomNavItem.items.firstOrNull { it.route == currentRoute }?.title
        ?: BottomNavItem.Home.title

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text(currentTitle) })
        },
        bottomBar = {
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { navigateTopLevel(item) },
                        icon = { Icon(item.icon(), contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(onStartPractice = { navigateTopLevel(BottomNavItem.Practice) })
            }
            composable(BottomNavItem.Practice.route) { PracticeScreen() }
            composable(BottomNavItem.Observation.route) { ObservationScreen() }
            composable(BottomNavItem.Learning.route) { LearningScreen() }
            composable(BottomNavItem.Reflection.route) { ReflectionScreen() }
        }
    }
}

/**
 * Keeps one destination per top-level section and saves each section's UI
 * state. This also makes tapping 首页 reliable after an asynchronous
 * meditation completion has finished writing its record.
 */
private fun navigateToTopLevel(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun BottomNavItem.icon() = when (this) {
    BottomNavItem.Home -> Icons.Default.Home
    BottomNavItem.Practice -> Icons.Default.SelfImprovement
    BottomNavItem.Observation -> Icons.Default.Visibility
    BottomNavItem.Learning -> Icons.Default.Book
    BottomNavItem.Reflection -> Icons.Default.Edit
}
