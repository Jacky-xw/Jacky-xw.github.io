package com.ruru.practice

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ruru.practice.core.work.WeeklyRolloverScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RuruApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        com.ruru.practice.feature.meditation.TimerDiagnostics.record(this, "application_created")
        // Arm Monday one-shot + daily guard. Foreground onResume remains a backup.
        WeeklyRolloverScheduler.schedule(this)
    }
}
