package com.ruru.practice.core.startup

import android.content.Context
import android.media.MediaPlayer
import com.ruru.practice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Performs only small, deterministic startup warmups. Feature data is loaded by
 * the owning screen when it becomes visible, avoiding duplicated Room I/O.
 */
@Singleton
class StartupWarmup @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun run() = withContext(Dispatchers.IO) {
        // Keep startup work intentionally small. The home screen owns its data
        // query and refreshes when it becomes visible; doing the same Room work
        // during splash only duplicated I/O and delayed first navigation.
        coroutineScope {
            listOf(
                async { runCatching { LearningTextCache.preload(context) } },
                async { runCatching { warmDefaultBell() } }
            ).awaitAll()
        }
    }

    private fun warmDefaultBell() {
        // Open and release once so the first natural-end play avoids cold codec setup.
        val player = MediaPlayer.create(context, R.raw.soft_bell) ?: return
        runCatching { player.release() }
    }
}
