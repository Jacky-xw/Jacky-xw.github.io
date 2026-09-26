package com.ruru.practice.feature.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import com.ruru.practice.feature.home.HomeScreen
import com.ruru.practice.feature.learning.LearningScreen
import com.ruru.practice.feature.navigation.BottomNavItem
import com.ruru.practice.feature.observation.ObservationScreen
import com.ruru.practice.feature.practice.PracticeScreen
import com.ruru.practice.feature.reflection.ReflectionScreen

@Composable
fun MainShell() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("ruru_practice", Context.MODE_PRIVATE) }
    var firstRunDone by remember { mutableStateOf(preferences.getBoolean("first_run_done", false)) }
    if (!firstRunDone) {
        FirstRunScreen(onFinish = { preferences.edit().putBoolean("first_run_done", true).apply(); firstRunDone = true }); return
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { NavigationBar { BottomNavItem.items.forEach { item -> NavigationBarItem(selected = currentRoute == item.route, onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }, icon = { Icon(item.icon(), contentDescription = item.title) }, label = { Text(item.title) }) } } }
    ) { innerPadding ->
        NavHost(navController, BottomNavItem.Home.route, modifier = Modifier.padding(innerPadding)) {
            composable(BottomNavItem.Home.route) { HomeScreen(onStartPractice = { navController.navigate(BottomNavItem.Practice.route) }) }
            composable(BottomNavItem.Practice.route) { PracticeScreen() }
            composable(BottomNavItem.Observation.route) { ObservationScreen() }
            composable(BottomNavItem.Learning.route) { LearningScreen() }
            composable(BottomNavItem.Reflection.route) { ReflectionScreen() }
        }
    }
}

private fun BottomNavItem.icon() = when (this) {
    BottomNavItem.Home -> Icons.Default.Home
    BottomNavItem.Practice -> Icons.Default.SelfImprovement
    BottomNavItem.Observation -> Icons.Default.Visibility
    BottomNavItem.Learning -> Icons.Default.Book
    BottomNavItem.Reflection -> Icons.Default.Edit
}
