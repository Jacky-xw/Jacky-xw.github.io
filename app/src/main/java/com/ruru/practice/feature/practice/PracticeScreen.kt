package com.ruru.practice.feature.practice

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruru.practice.domain.usecase.WeeklyRolloverEvents
import com.ruru.practice.feature.meditation.MeditationScreen
import com.ruru.practice.feature.meditation.MeditationSessionViewModel
import kotlinx.coroutines.launch

private val practiceTabs = listOf("安般念", "经行", "护根", "五盖", "戒行", "八戒")

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
    val pagerState = rememberPagerState(pageCount = { practiceTabs.size })
    val scope = rememberCoroutineScope()

    fun refreshHistories() {
        meditationViewModel.refreshHistory()
        walkingViewModel.refresh()
        rootProtectionViewModel.refresh()
        practiceLogViewModel.refresh()
        eightPreceptsViewModel.refreshHistory()
    }

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
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                practiceTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 0
            ) { page ->
                when (page) {
                    0 -> MeditationScreen(viewModel = meditationViewModel, modifier = Modifier.fillMaxSize())
                    1 -> WalkingScreen(viewModel = walkingViewModel, modifier = Modifier.fillMaxSize())
                    2 -> RootProtectionScreen(viewModel = rootProtectionViewModel, modifier = Modifier.fillMaxSize())
                    3 -> PracticeLogScreen(mode = 0, viewModel = practiceLogViewModel, modifier = Modifier.fillMaxSize())
                    4 -> PracticeLogScreen(mode = 1, viewModel = practiceLogViewModel, modifier = Modifier.fillMaxSize())
                    else -> EightPreceptsScreen(viewModel = eightPreceptsViewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity {
    when (this) {
        is ComponentActivity -> return this
        is ContextWrapper -> return baseContext.findComponentActivity()
        else -> error("PracticeScreen must be hosted in a ComponentActivity")
    }
}
