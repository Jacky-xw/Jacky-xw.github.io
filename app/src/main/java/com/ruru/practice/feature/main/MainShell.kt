package com.ruru.practice.feature.main

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
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
        FirstRunScreen(onFinish = { preferences.edit().putBoolean("first_run_done", true).apply(); firstRunDone = true }); return
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navigateTopLevel: (BottomNavItem) -> Unit = { item ->
        navigateToTopLevel(navController, item.route)
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentTitle = BottomNavItem.items.firstOrNull { it.route == currentRoute }?.title
        ?: BottomNavItem.Home.title

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "实修日",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "主导航",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                    BottomNavItem.items.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    navigateTopLevel(item)
                                }
                            },
                            icon = { Icon(item.icon(), contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "打开主导航")
                        }
                    }
                )
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
            NavHost(navController, BottomNavItem.Home.route, modifier = Modifier.padding(innerPadding)) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(onStartPractice = { navigateTopLevel(BottomNavItem.Practice) })
                }
                composable(BottomNavItem.Practice.route) { PracticeScreen() }
                composable(BottomNavItem.Observation.route) { ObservationScreen() }
                composable(BottomNavItem.Learning.route) {
                    LearningScreen(
                        currentTopLevelRoute = currentRoute,
                        onNavigateTopLevel = navigateTopLevel
                    )
                }
                composable(BottomNavItem.Reflection.route) { ReflectionScreen() }
            }
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

    // Use the standard single top-level stack pattern. It avoids leaving a
    // completed timer destination above 首页, which previously made a tap on
    // the bottom bar appear to do nothing while the asynchronous save ran.
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
