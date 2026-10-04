package com.ruru.practice.feature.navigation

sealed class BottomNavItem(
    val route: String,
    val title: String
) {
    data object Home : BottomNavItem("home", "首页")
    data object Practice : BottomNavItem("practice", "修习")
    data object Observation : BottomNavItem("observation", "观察")
    data object Learning : BottomNavItem("learning", "学习")
    data object Reflection : BottomNavItem("reflection", "复盘")

    companion object {
        val items: List<BottomNavItem> = listOf(Home, Practice, Observation, Learning, Reflection)
    }
}
