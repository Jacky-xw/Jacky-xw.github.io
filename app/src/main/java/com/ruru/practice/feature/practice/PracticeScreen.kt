package com.ruru.practice.feature.practice

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruru.practice.domain.usecase.WeeklyRolloverEvents
import com.ruru.practice.feature.meditation.MeditationScreen
import com.ruru.practice.feature.meditation.MeditationSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen() {
    // Top-level navigation saves and removes the practice destination when
    // the user switches to another section.  Keep both timer ViewModels in
    // the Activity store so their ticker and completion bell survive that
    // destination change.
    val activity = LocalContext.current.findComponentActivity()
    val meditationViewModel: MeditationSessionViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val walkingViewModel: WalkingViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val rootProtectionViewModel: RootProtectionViewModel = hiltViewModel()
    val practiceLogViewModel: PracticeLogViewModel = hiltViewModel()
    val eightPreceptsViewModel: EightPreceptsViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val weeklyRolloverVersion by WeeklyRolloverEvents.version.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    fun refreshHistories() {
        meditationViewModel.refreshHistory()
        walkingViewModel.refresh()
        rootProtectionViewModel.refresh()
        practiceLogViewModel.refresh()
        eightPreceptsViewModel.refreshHistory()
    }

    // The weekly rollover also runs when the Activity returns from the
    // background. Refresh every practice list after that boundary is checked.
    LaunchedEffect(Unit) { refreshHistories() }
    LaunchedEffect(weeklyRolloverVersion) {
        if (weeklyRolloverVersion > 0L) refreshHistories()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshHistories()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                0 -> MeditationScreen(viewModel = meditationViewModel, modifier = Modifier.weight(1f))
                1 -> WalkingScreen(viewModel = walkingViewModel, modifier = Modifier.weight(1f))
                2 -> RootProtectionScreen(viewModel = rootProtectionViewModel, modifier = Modifier.weight(1f))
                3 -> PracticeLogScreen(mode = 0, viewModel = practiceLogViewModel, modifier = Modifier.weight(1f))
                5 -> EightPreceptsScreen(viewModel = eightPreceptsViewModel, modifier = Modifier.weight(1f))
                else -> PracticeLogScreen(mode = 1, viewModel = practiceLogViewModel, modifier = Modifier.weight(1f))
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity {
    when (this) {
        is ComponentActivity -> return this
        is ContextWrapper -> return baseContext.findComponentActivity()
        else -> error("Practice timers require a ComponentActivity context")
    }
}
