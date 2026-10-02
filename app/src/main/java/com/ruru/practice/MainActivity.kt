package com.ruru.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import com.ruru.practice.core.ui.RuruPracticeTheme
import com.ruru.practice.feature.main.MainShell
import com.ruru.practice.domain.usecase.WeeklyRolloverEvents
import com.ruru.practice.domain.usecase.WeeklyRolloverUseCase
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var weeklyRollover: WeeklyRolloverUseCase
    private val weeklyRolloverMutex = Mutex()

    /**
     * Weekly retention is settled in three complementary ways:
     * 1. WorkManager one-shot aimed at next Monday 00:05 (background).
     * 2. WorkManager daily guard (covers OEM/Doze delay of the one-shot).
     * 3. This foreground path on every resume / cold start (immediate UI consistency).
     * All paths call the same idempotent [WeeklyRolloverUseCase].
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            settleWeeklyRecords()
        }
    }

    private suspend fun settleWeeklyRecords() {
        weeklyRolloverMutex.withLock {
            runCatching { weeklyRollover() }
                .onSuccess { WeeklyRolloverEvents.publish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuruPracticeTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    // V1.32 no longer preloads or caches learning content.
                    // Keep only the real startup settlement and a short, visible splash.
                    settleWeeklyRecords()
                    delay(1200)
                    showSplash = false
                }
                if (showSplash) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "每日实修，记录当下",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "正在准备应用…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    MainShell()
                }
            }
        }
    }
}
