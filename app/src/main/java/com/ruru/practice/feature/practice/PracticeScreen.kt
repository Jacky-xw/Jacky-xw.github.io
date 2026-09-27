package com.ruru.practice.feature.practice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruru.practice.feature.meditation.MeditationScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Six labels cannot fit reliably in a fixed TabRow on narrow
            // screens. Scrolling keeps every label readable and preserves the
            // content area below it.
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("安般念", "经行", "护根", "五盖", "戒行", "八戒").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { androidx.compose.material3.Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> MeditationScreen(modifier = Modifier.weight(1f))
                1 -> WalkingScreen(viewModel = androidx.hilt.navigation.compose.hiltViewModel(), modifier = Modifier.weight(1f))
                2 -> RootProtectionScreen(viewModel = androidx.hilt.navigation.compose.hiltViewModel(), modifier = Modifier.weight(1f))
                3 -> PracticeLogScreen(mode = 0, modifier = Modifier.weight(1f))
                5 -> EightPreceptsScreen(modifier = Modifier.weight(1f))
                else -> PracticeLogScreen(mode = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}
