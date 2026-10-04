package com.ruru.practice.feature.meditation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.audio.EndSessionAlert
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.MeditationSessionEntity
import com.ruru.practice.data.repository.PracticeRepository
import com.ruru.practice.domain.usecase.SaveMeditationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MeditationSessionViewModel @Inject constructor(
    private val saveMeditation: SaveMeditationUseCase,
    private val repository: PracticeRepository,
    private val bellSoundPlayer: BellSoundPlayer,
    private val endSessionAlert: EndSessionAlert,
    @ApplicationContext context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(MeditationTimerState())
    val state = _state.asStateFlow()
    private val _history = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())
    val history = _history.asStateFlow()
    private var timerJob: Job? = null
    private var startedAtMillis: Long? = null
    private var pausedElapsedSeconds: Int = 0
    private var currentSessionId: Long = 0L
    private var isCompleting = false
    private var sessionGeneration = 0L

    init {
        restoreActiveTimer()
        refreshHistory()
        viewModelScope.launch {
            MeditationTimerBridge.naturalEndDue.collect {
                // AlarmManager path: audio already started in the receiver/service.
                if (_state.value.phase == MeditationPhase.RUNNING ||
                    _state.value.phase == MeditationPhase.PAUSED
                ) {
                    completeSession(naturalEnd = true, alertAlreadyStarted = true)
                } else if (_state.value.phase == MeditationPhase.COMPLETED &&
                    !_state.value.endAlarmActive &&
                    _state.value.selectedMinutes >= 60
                ) {
                    signalNaturalEnd()
                }
            }
        }
    }

    /**
     * Kept for callers compiled against the previous screen-lifecycle API.
     * Timer ownership is now independent of screen visibility, so this is
     * intentionally a no-op.
     */
    fun setScreenActive(@Suppress("UNUSED_PARAMETER") active: Boolean) = Unit

    fun selectStep(step: Int) {
        if (_state.value.phase == MeditationPhase.RUNNING || _state.value.phase == MeditationPhase.PAUSED) return
        val safe = step.coerceIn(1, 16)
        if (_state.value.phase == MeditationPhase.COMPLETED || _state.value.phase == MeditationPhase.SAVED) {
            val minutes = _state.value.selectedMinutes
            reset()
            _state.value = _state.value.copy(practiceStep = safe, remainingSeconds = minutes * 60)
        } else {
            _state.value = _state.value.copy(practiceStep = safe, observation = "", afterState = "")
            persistState()
        }
    }

    fun selectMinutes(minutes: Int) {
        if (_state.value.phase == MeditationPhase.RUNNING || _state.value.phase == MeditationPhase.PAUSED) return
        val safe = minutes.coerceIn(1, 360)
        val current = _state.value
        sessionGeneration++
        bellSoundPlayer.stop()
        isCompleting = false
        _state.value = MeditationTimerState(selectedMinutes = safe, practiceStep = current.practiceStep, remainingSeconds = safe * 60)
        currentSessionId = 0L
        clearPersistence()
    }

    fun start() {
        if (_state.value.phase == MeditationPhase.COMPLETED || _state.value.phase == MeditationPhase.SAVED) reset()
        val now = System.currentTimeMillis()
        sessionGeneration++
        bellSoundPlayer.stop()
        currentSessionId = 0L
        isCompleting = false
        pausedElapsedSeconds = 0
        startedAtMillis = now
        _state.value = _state.value.copy(
            phase = MeditationPhase.RUNNING,
            elapsedSeconds = 0,
            remainingSeconds = _state.value.selectedMinutes * 60,
            saveState = MeditationSaveState(),
            endAlarmActive = false
        )
        persistState()
        startTicker()
        scheduleWallClockEnd()
    }

    fun pause() {
        if (_state.value.phase != MeditationPhase.RUNNING) return
        updateElapsed()
        timerJob?.cancel()
        cancelWallClockEnd()
        pausedElapsedSeconds = _state.value.elapsedSeconds
        startedAtMillis = null
        _state.value = _state.value.copy(phase = MeditationPhase.PAUSED)
        persistState()
    }

    fun resume() {
        if (_state.value.phase != MeditationPhase.PAUSED) return
        startedAtMillis = System.currentTimeMillis()
        _state.value = _state.value.copy(phase = MeditationPhase.RUNNING)
        persistState()
        startTicker()
        scheduleWallClockEnd()
    }

    /** User-triggered completion: count once, but do not play the bell. */
    fun finish() = completeSession(naturalEnd = false)

    private fun completeSession(naturalEnd: Boolean, alertAlreadyStarted: Boolean = false) {
        val phase = _state.value.phase
        if ((phase != MeditationPhase.RUNNING && phase != MeditationPhase.PAUSED) || isCompleting) return
        if (phase == MeditationPhase.RUNNING) updateElapsed()
        // Wall-clock natural end (AlarmManager) may arrive while UI elapsed was frozen at 0.
        if (naturalEnd) {
            val target = _state.value.selectedMinutes * 60
            if (_state.value.elapsedSeconds < target) {
                _state.value = _state.value.copy(
                    elapsedSeconds = target,
                    remainingSeconds = 0
                )
            }
        }
        if (_state.value.elapsedSeconds <= 0) return
        isCompleting = true
        timerJob?.cancel()
        cancelWallClockEnd()
        startedAtMillis = null
        pausedElapsedSeconds = _state.value.elapsedSeconds
        _state.value = _state.value.copy(
            phase = MeditationPhase.COMPLETED,
            remainingSeconds = (_state.value.selectedMinutes * 60 - _state.value.elapsedSeconds).coerceAtLeast(0),
            saveState = MeditationSaveState(isSaving = true, message = "正在记录本次练习…")
        )
        persistState()
        // A natural completion is the only event that may ring. Manual finish,
        // pause/resume, reset, and leaving the page never ring the bell.  The
        // session ViewModel is activity-scoped, so this also works after the
        // practice destination is removed from the navigation back stack.
        if (naturalEnd) {
            if (!alertAlreadyStarted) signalNaturalEnd()
            else if (_state.value.selectedMinutes >= 60) {
                // Receiver/service already looping; only mark UI flag.
                _state.value = _state.value.copy(endAlarmActive = true)
                persistState()
            }
        } else {
            MeditationKeepAliveService.stop(appContext)
            bellSoundPlayer.stop()
            endSessionAlert.stopVibration()
            _state.value = _state.value.copy(endAlarmActive = false)
        }
        createSessionRecordIfNeeded()
    }

    private fun createSessionRecordIfNeeded() {
        if (currentSessionId > 0L) { isCompleting = false; return }
        val current = _state.value
        val generation = sessionGeneration
        viewModelScope.launch {
            runCatchingSuspend {
                saveMeditation(
                    MeditationSessionEntity(
                        date = nowLabel(),
                        durationSeconds = current.elapsedSeconds,
                        observation = "",
                        afterState = "",
                        practiceStep = current.practiceStep,
                        stepTitle = AnapanStepCatalog.title(current.practiceStep)
                    )
                )
            }.onSuccess { id ->
                // The insert may finish after the user has started a new
                // session. Keep the record, but do not let the old callback
                // mutate the new timer state.
                if (generation == sessionGeneration) {
                    currentSessionId = id
                    isCompleting = false
                    _state.value = _state.value.copy(saveState = MeditationSaveState(message = "本次练习已自动计入统计，可继续写下这一刻"))
                    persistState()
                }
                // Do not refresh history here. This row is intentionally created
                // before the optional reflection is saved, so publishing it now
                // briefly renders an empty history card. The saved-note path below
                // refreshes immediately; if the user skips notes, the record is
                // loaded normally the next time history is refreshed/opened.
            }.onFailure { error ->
                if (generation == sessionGeneration) {
                    isCompleting = false
                    _state.value = _state.value.copy(saveState = MeditationSaveState(error = error.message ?: "练习记录写入失败，请重试"))
                }
            }
        }
    }

    fun reset() {
        cancelWallClockEnd()
        acknowledgeEndAlarm()
        MeditationKeepAliveService.stop(appContext)
        sessionGeneration++
        timerJob?.cancel(); bellSoundPlayer.stop(); startedAtMillis = null; pausedElapsedSeconds = 0; currentSessionId = 0L; isCompleting = false
        val minutes = _state.value.selectedMinutes; val step = _state.value.practiceStep
        _state.value = MeditationTimerState(selectedMinutes = minutes, practiceStep = step, remainingSeconds = minutes * 60)
        clearPersistence()
    }

    fun updateObservation(value: String) { _state.value = _state.value.copy(observation = value, saveState = _state.value.saveState.copy(error = null)); persistState() }
    fun updateAfterState(value: String) { _state.value = _state.value.copy(afterState = value, saveState = _state.value.saveState.copy(error = null)); persistState() }

    fun setBellSound(uri: Uri, displayName: String?): Result<Unit> = bellSoundPlayer.setCustomSound(uri, displayName)
    fun useDefaultBellSound(): Result<Unit> = bellSoundPlayer.useDefaultSound()
    fun bellSoundName(): String = bellSoundPlayer.customSoundName ?: "软件内置引磬"

    /** Plays the local bell once so the user can confirm the reminder volume. */
    fun previewBell(): Boolean = bellSoundPlayer.play()

    var endVibrationEnabled: Boolean
        get() = endSessionAlert.vibrationEnabled
        set(value) { endSessionAlert.vibrationEnabled = value }

    fun isMediaMutedOrVeryLow(): Boolean = endSessionAlert.isMediaMutedOrVeryLow()
    fun mediaVolumePercent(): Int = endSessionAlert.mediaVolumePercent()

    private fun signalNaturalEnd() {
        val longSeat = _state.value.selectedMinutes >= 60
        if (longSeat) {
            // Mandatory continuous alert until the practitioner confirms on-screen.
            _state.value = _state.value.copy(endAlarmActive = true)
            persistState()
        }
        // Always deliver through the foreground service so background / lock-screen
        // paths use the same reliable playback stack.
        MeditationKeepAliveService.notifyEnd(appContext, longSeat = longSeat)
    }

    /** Stops the mandatory long-seat end alarm. Cannot be skipped by settings. */
    fun acknowledgeEndAlarm() {
        if (!_state.value.endAlarmActive) return
        bellSoundPlayer.stop()
        endSessionAlert.stopVibration()
        _state.value = _state.value.copy(endAlarmActive = false)
        prefs.edit().putBoolean("end_alarm_active", false).apply()
        MeditationKeepAliveService.stop(appContext)
    }

    fun isLongSeat(minutes: Int = _state.value.selectedMinutes): Boolean = minutes >= 60
    fun batteryPercent(): Int = endSessionAlert.batteryPercent()


    /** Updates the already-counted session. It never inserts a second session. */
    fun save() {
        val current = _state.value
        if (current.phase != MeditationPhase.COMPLETED || current.elapsedSeconds <= 0 || current.saveState.isSaving || isCompleting) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saveState = MeditationSaveState(isSaving = true))
            runCatchingSuspend {
                if (currentSessionId <= 0L) {
                    currentSessionId = saveMeditation(
                        MeditationSessionEntity(
                            date = nowLabel(), durationSeconds = current.elapsedSeconds,
                            observation = current.observation.trim(), afterState = current.afterState.trim(),
                            practiceStep = current.practiceStep, stepTitle = AnapanStepCatalog.title(current.practiceStep)
                        )
                    )
                } else {
                    repository.updateMeditationNotes(currentSessionId, current.observation.trim(), current.afterState.trim())
                }
            }.onSuccess {
                clearPersistence()
                _state.value = _state.value.copy(phase = MeditationPhase.SAVED, saveState = MeditationSaveState(saved = true, message = "这一刻已记录，不会重复增加练习次数"))
                refreshHistory()
            }.onFailure { error -> _state.value = _state.value.copy(saveState = MeditationSaveState(error = error.message ?: "记录失败，请稍后重试")) }
        }
    }

    fun deleteSessions(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatchingSuspend { repository.deleteMeditations(ids.toList()) }
                .onSuccess {
                    if (currentSessionId in ids) reset()
                    refreshHistory()
                }
        }
    }

    fun newSession() = reset()

    fun refreshHistory() { viewModelScope.launch { runCatchingSuspend { repository.getRecentMeditations(50) }.onSuccess { _history.value = it } } }

    
    private fun scheduleWallClockEnd() {
        val current = _state.value
        if (current.phase != MeditationPhase.RUNNING) {
            MeditationEndScheduler.cancel(appContext)
            return
        }
        val remaining = (current.selectedMinutes * 60 - current.elapsedSeconds).coerceAtLeast(0)
        val endAt = System.currentTimeMillis() + remaining * 1000L
        MeditationEndScheduler.schedule(appContext, endAt)
        // Keep a foreground service for the whole seat so OEM freezers cannot
        // silence the end alarm when the user leaves the app.
        MeditationKeepAliveService.start(appContext)
    }

    private fun cancelWallClockEnd() {
        MeditationEndScheduler.cancel(appContext)
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _state.value.phase == MeditationPhase.RUNNING) {
                delay(1000)
                updateElapsed()
                if (_state.value.elapsedSeconds >= _state.value.selectedMinutes * 60) { completeSession(naturalEnd = true); break }
            }
        }
    }

    private fun updateElapsed() {
        val current = _state.value
        val live = if (current.phase == MeditationPhase.RUNNING && startedAtMillis != null) {
            pausedElapsedSeconds + ((System.currentTimeMillis() - (startedAtMillis ?: System.currentTimeMillis())) / 1000L).toInt()
        } else current.elapsedSeconds
        val elapsed = live.coerceIn(0, current.selectedMinutes * 60)
        // Only push UI state when the displayed second changes.
        // Do NOT write SharedPreferences here: restore uses started_at + paused base,
        // so per-second disk I/O is unnecessary and was the main scroll hitch.
        if (elapsed == current.elapsedSeconds) return
        _state.value = current.copy(elapsedSeconds = elapsed, remainingSeconds = current.selectedMinutes * 60 - elapsed)
    }

    private fun restoreActiveTimer() {
        if (!prefs.getBoolean("active", false)) return
        val minutes = prefs.getInt("minutes", 20).coerceIn(1, 360)
        val step = prefs.getInt("step", 1).coerceIn(1, 16)
        val phase = runCatching { MeditationPhase.valueOf(prefs.getString("phase", MeditationPhase.PAUSED.name) ?: MeditationPhase.PAUSED.name) }.getOrDefault(MeditationPhase.PAUSED)
        val paused = prefs.getInt("paused_elapsed", 0).coerceAtLeast(0)
        val storedStart = prefs.getLong("started_at", 0L)
        val storedPaused = prefs.getInt("elapsed", paused).coerceAtLeast(0)
        currentSessionId = prefs.getLong("session_id", 0L)
        val elapsed = if (phase == MeditationPhase.RUNNING && storedStart > 0L) paused + ((System.currentTimeMillis() - storedStart) / 1000L).toInt() else storedPaused
        val bounded = elapsed.coerceAtMost(minutes * 60)
        val reachedNaturalEnd = phase == MeditationPhase.RUNNING && bounded >= minutes * 60
        _state.value = MeditationTimerState(phase = if (reachedNaturalEnd) MeditationPhase.COMPLETED else phase, selectedMinutes = minutes, practiceStep = step, elapsedSeconds = bounded, remainingSeconds = minutes * 60 - bounded, observation = prefs.getString("observation", "") ?: "", afterState = prefs.getString("after_state", "") ?: "")
        // Fold already-elapsed time into the base, then start a fresh segment.
        // Using the full bounded value (not the old pause base) prevents
        // double-counting after process death while RUNNING after pause/resume.
        pausedElapsedSeconds = bounded
        val storedEndAlarm = prefs.getBoolean("end_alarm_active", false)
        if (reachedNaturalEnd || storedEndAlarm) {
            // Process may have been killed at/after the deadline. Resume the same
            // natural-end signalling (looping for long seats) until the user confirms.
            startedAtMillis = null
            if (reachedNaturalEnd || _state.value.phase == MeditationPhase.RUNNING) {
                _state.value = _state.value.copy(phase = MeditationPhase.COMPLETED)
            }
            persistState()
            signalNaturalEnd()
        } else if (_state.value.phase == MeditationPhase.RUNNING) {
            startedAtMillis = System.currentTimeMillis()
            startTicker()
            scheduleWallClockEnd()
        } else {
            startedAtMillis = null
        }
        if (_state.value.phase == MeditationPhase.COMPLETED && currentSessionId <= 0L && bounded > 0) createSessionRecordIfNeeded()
    }

    private fun persistState() {
        val s = _state.value
        if (s.phase == MeditationPhase.READY || s.phase == MeditationPhase.SAVED) {
            // Still persist mandatory end-alarm flag so a killed process can resume the alert.
            if (s.endAlarmActive) {
                prefs.edit().putBoolean("end_alarm_active", true).putInt("minutes", s.selectedMinutes).apply()
            }
            return
        }
        prefs.edit()
            .putBoolean("active", true)
            .putInt("minutes", s.selectedMinutes)
            .putInt("step", s.practiceStep)
            .putString("phase", s.phase.name)
            .putInt("elapsed", s.elapsedSeconds)
            .putInt("paused_elapsed", pausedElapsedSeconds)
            .putLong("started_at", startedAtMillis ?: 0L)
            .putLong("session_id", currentSessionId)
            .putString("observation", s.observation)
            .putString("after_state", s.afterState)
            .putBoolean("end_alarm_active", s.endAlarmActive)
            .apply()
    }
    private fun clearPersistence() { prefs.edit().clear().apply() }
    private fun nowLabel() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    override fun onCleared() {
        if (_state.value.phase == MeditationPhase.RUNNING || _state.value.phase == MeditationPhase.PAUSED) {
            persistState()
        }
        // Do not stop a mandatory end alarm here — process may be recreated; service holds the foreground.
        if (!_state.value.endAlarmActive) {
            timerJob?.cancel()
            bellSoundPlayer.stop()
            endSessionAlert.stopVibration()
            MeditationKeepAliveService.stop(appContext)
        } else {
            timerJob?.cancel()
        }
        super.onCleared()
    }
}
