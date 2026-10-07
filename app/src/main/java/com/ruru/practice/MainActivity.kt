package com.ruru.practice

import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ruru.practice.core.startup.StartupWarmup
import com.ruru.practice.core.ui.RuruPracticeTheme
import com.ruru.practice.domain.usecase.WeeklyRolloverEvents
import com.ruru.practice.domain.usecase.WeeklyRolloverUseCase
import com.ruru.practice.feature.main.MainShell
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var weeklyRollover: WeeklyRolloverUseCase
    @Inject lateinit var startupWarmup: StartupWarmup
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
        com.ruru.practice.feature.meditation.TimerDiagnostics.record(this, "activity_resumed")
        com.ruru.practice.feature.meditation.TimerDiagnostics.capture(this, "activity_resumed")
        lifecycleScope.launch {
            settleWeeklyRecords()
        }
    }

    override fun onPause() {
        com.ruru.practice.feature.meditation.TimerDiagnostics.record(this, "activity_paused")
        super.onPause()
    }

    override fun onStop() {
        com.ruru.practice.feature.meditation.TimerDiagnostics.record(this, "activity_stopped")
        super.onStop()
    }

    private suspend fun settleWeeklyRecords() {
        weeklyRolloverMutex.withLock {
            runCatching { weeklyRollover() }
                .onSuccess { WeeklyRolloverEvents.publish() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        enableEdgeToEdge()
        setContent {
            RuruPracticeTheme {
                var showSplash by remember { mutableStateOf(true) }
                var status by remember { mutableStateOf("正在整理本周记录…") }
                LaunchedEffect(Unit) {
                    val elapsed = measureTimeMillis {
                        status = "正在整理本周记录…"
                        settleWeeklyRecords()
                        status = "正在加载修习与观察数据…"
                        startupWarmup.run()
                        status = "即将进入…"
                    }
                    // Avoid a flash on very fast devices; real work already finished above.
                    val minSplashMs = 500L
                    if (elapsed < minSplashMs) delay(minSplashMs - elapsed)
                    showSplash = false
                }
                if (showSplash) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "一分钟禅修",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = status,
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
