package com.ruru.practice.feature.meditation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.MeditationSessionEntity
import com.ruru.practice.data.repository.PracticeRepository
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
    private val saveMeditation: com.ruru.practice.domain.usecase.SaveMeditationUseCase,
    private val repository: PracticeRepository,
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(MeditationTimerState())
    val state = _state.asStateFlow()
    private val _history = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())
    val history = _history.asStateFlow()
    private var timerJob: Job? = null
    private var startedAtMillis: Long? = null
    private var pausedElapsedSeconds: Int = 0

    init {
        restoreActiveTimer()
        refreshHistory()
    }

    fun selectStep(step: Int) {
        if (_state.value.phase == MeditationPhase.RUNNING || _state.value.phase == MeditationPhase.PAUSED) return
        _state.value = _state.value.copy(practiceStep = step.coerceIn(1, 16), observation = "", afterState = "")
        persistState()
    }

    fun selectMinutes(minutes: Int) {
        if (_state.value.phase == MeditationPhase.RUNNING || _state.value.phase == MeditationPhase.PAUSED) return
        val safe = minutes.coerceIn(1, 60)
        val current = _state.value
        _state.value = MeditationTimerState(selectedMinutes = safe, practiceStep = current.practiceStep, remainingSeconds = safe * 60)
        clearPersistence()
    }

    fun start() {
        if (_state.value.phase == MeditationPhase.COMPLETED || _state.value.phase == MeditationPhase.SAVED) reset()
        val now = System.currentTimeMillis()
        pausedElapsedSeconds = 0
        startedAtMillis = now
        _state.value = _state.value.copy(phase = MeditationPhase.RUNNING, elapsedSeconds = 0, remainingSeconds = _state.value.selectedMinutes * 60, saveState = MeditationSaveState())
        persistState()
        startTicker()
    }

    fun pause() {
        if (_state.value.phase != MeditationPhase.RUNNING) return
        updateElapsed()
        timerJob?.cancel()
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
    }

    fun finish() {
        if (_state.value.phase != MeditationPhase.RUNNING && _state.value.phase != MeditationPhase.PAUSED) return
        if (_state.value.phase == MeditationPhase.RUNNING) updateElapsed()
        timerJob?.cancel()
        startedAtMillis = null
        pausedElapsedSeconds = _state.value.elapsedSeconds
        _state.value = _state.value.copy(phase = MeditationPhase.COMPLETED, remainingSeconds = (_state.value.selectedMinutes * 60 - _state.value.elapsedSeconds).coerceAtLeast(0))
        persistState()
    }

    fun reset() {
        timerJob?.cancel(); startedAtMillis = null; pausedElapsedSeconds = 0
        val minutes = _state.value.selectedMinutes; val step = _state.value.practiceStep
        _state.value = MeditationTimerState(selectedMinutes = minutes, practiceStep = step, remainingSeconds = minutes * 60)
        clearPersistence()
    }

    fun updateObservation(value: String) { _state.value = _state.value.copy(observation = value, saveState = _state.value.saveState.copy(error = null)); persistState() }
    fun updateAfterState(value: String) { _state.value = _state.value.copy(afterState = value, saveState = _state.value.saveState.copy(error = null)); persistState() }

    fun save() {
        val current = _state.value
        if (current.phase != MeditationPhase.COMPLETED || current.elapsedSeconds <= 0 || current.saveState.isSaving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saveState = MeditationSaveState(isSaving = true))
            runCatchingSuspend {
                saveMeditation(MeditationSessionEntity(nowLabel(), current.elapsedSeconds, current.observation.trim(), current.afterState.trim(), current.practiceStep, AnapanStepCatalog.title(current.practiceStep)))
            }.onSuccess {
                clearPersistence()
                _state.value = _state.value.copy(phase = MeditationPhase.SAVED, saveState = MeditationSaveState(saved = true, message = "已保存这次练习"))
                refreshHistory()
            }.onFailure { error -> _state.value = _state.value.copy(saveState = MeditationSaveState(error = error.message ?: "保存失败，请稍后重试")) }
        }
    }

    fun newSession() = reset()

    fun refreshHistory() { viewModelScope.launch { runCatchingSuspend { repository.getRecentMeditations(20) }.onSuccess { _history.value = it } } }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _state.value.phase == MeditationPhase.RUNNING) {
                delay(250)
                updateElapsed()
                if (_state.value.elapsedSeconds >= _state.value.selectedMinutes * 60) { finish(); break }
            }
        }
    }

    private fun updateElapsed() {
        val current = _state.value
        val live = if (current.phase == MeditationPhase.RUNNING && startedAtMillis != null) {
            pausedElapsedSeconds + ((System.currentTimeMillis() - startedAtMillis!!) / 1000L).toInt()
        } else current.elapsedSeconds
        val elapsed = live.coerceIn(0, current.selectedMinutes * 60)
        _state.value = current.copy(elapsedSeconds = elapsed, remainingSeconds = current.selectedMinutes * 60 - elapsed)
        persistState()
    }

    private fun restoreActiveTimer() {
        if (!prefs.getBoolean("active", false)) return
        val minutes = prefs.getInt("minutes", 20).coerceIn(1, 60)
        val step = prefs.getInt("step", 1).coerceIn(1, 16)
        val phase = runCatching { MeditationPhase.valueOf(prefs.getString("phase", MeditationPhase.PAUSED.name)!!) }.getOrDefault(MeditationPhase.PAUSED)
        val paused = prefs.getInt("paused_elapsed", 0).coerceAtLeast(0)
        val storedStart = prefs.getLong("started_at", 0L)
        val storedPaused = prefs.getInt("elapsed", paused).coerceAtLeast(0)
        val elapsed = if (phase == MeditationPhase.RUNNING && storedStart > 0L) paused + ((System.currentTimeMillis() - storedStart) / 1000L).toInt() else storedPaused
        val bounded = elapsed.coerceAtMost(minutes * 60)
        _state.value = MeditationTimerState(phase = if (bounded >= minutes * 60) MeditationPhase.COMPLETED else phase, selectedMinutes = minutes, practiceStep = step, elapsedSeconds = bounded, remainingSeconds = minutes * 60 - bounded, observation = prefs.getString("observation", "") ?: "", afterState = prefs.getString("after_state", "") ?: "")
        pausedElapsedSeconds = bounded
        if (_state.value.phase == MeditationPhase.RUNNING) { startedAtMillis = System.currentTimeMillis() - ((bounded - paused).coerceAtLeast(0) * 1000L); startTicker() }
    }

    private fun persistState() {
        val s = _state.value
        if (s.phase == MeditationPhase.READY || s.phase == MeditationPhase.SAVED) return
        prefs.edit().putBoolean("active", true).putInt("minutes", s.selectedMinutes).putInt("step", s.practiceStep).putString("phase", s.phase.name).putInt("elapsed", s.elapsedSeconds).putInt("paused_elapsed", pausedElapsedSeconds).putLong("started_at", startedAtMillis ?: 0L).putString("observation", s.observation).putString("after_state", s.afterState).apply()
    }
    private fun clearPersistence() { prefs.edit().clear().apply() }
    private fun nowLabel() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    override fun onCleared() { timerJob?.cancel(); super.onCleared() }
}
