package com.ruru.practice.core.startup

import android.content.Context
import android.media.MediaPlayer
import com.ruru.practice.R
import com.ruru.practice.data.repository.PracticeRepository
import com.ruru.practice.domain.usecase.GetPracticeSummaryUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Runs during the splash screen so first navigation into 首页 / 修习 / 观察 /
 * 复盘 / 学习 does not pay first-hit Room and asset costs on the UI thread.
 */
@Singleton
class StartupWarmup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PracticeRepository,
    private val getPracticeSummary: GetPracticeSummaryUseCase
) {
    suspend fun run() = withContext(Dispatchers.IO) {
        coroutineScope {
            val jobs = listOf(
                // Home summary (same query graph the home screen will run).
                async { runCatching { getPracticeSummary() } },
                // 修习 histories
                async { runCatching { repository.getRecentMeditations(50) } },
                async { runCatching { repository.getRecentWalking(50) } },
                async { runCatching { repository.getRecentRootProtections(50) } },
                async { runCatching { repository.getRecentFiveCovers(50) } },
                async { runCatching { repository.getRecentPrecepts(50) } },
                async { runCatching { repository.getRecentEightPreceptSessions(30) } },
                // 观察
                async { runCatching { repository.getRecentMindfulnessEvents(50) } },
                async { runCatching { repository.getRecentFiveAggregates(50) } },
                async { runCatching { repository.getRecentDependentOriginations(50) } },
                // 复盘
                async { runCatching { repository.getRecentReflections(50) } },
                async { runCatching { repository.getRecentDailyPractices(50) } },
                async { runCatching { repository.getWeeklyReport() } },
                // 学习 texts + default bell decode path
                async { runCatching { LearningTextCache.preload(context) } },
                async { runCatching { warmDefaultBell() } }
            )
            jobs.awaitAll()
        }
    }

    private fun warmDefaultBell() {
        // Open and release once so the first natural-end play avoids cold codec setup.
        val player = MediaPlayer.create(context, R.raw.soft_bell) ?: return
        runCatching { player.release() }
    }
}
