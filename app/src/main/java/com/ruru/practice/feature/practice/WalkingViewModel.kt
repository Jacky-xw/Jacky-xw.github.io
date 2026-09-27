package com.ruru.practice.feature.practice

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.WalkingSessionEntity
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

enum class WalkingTimerPhase { READY, RUNNING, PAUSED, FINISHED }
data class WalkingUiState(
    val method: String = "全身行走", val phase: WalkingTimerPhase = WalkingTimerPhase.READY,
    val targetSeconds: Int = 60 * 60, val elapsed: Int = 0, val note: String = "",
    val saving: Boolean = false, val saved: Boolean = false, val message: String? = null,
    val history: List<WalkingSessionEntity> = emptyList()
) {
    val remaining: Int get() = (targetSeconds - elapsed).coerceAtLeast(0)
}

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val repo: PracticeRepository,
    private val bellSoundPlayer: BellSoundPlayer,
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(WalkingUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var startedAtMillis: Long? = null
    private var accumulatedSeconds = 0

    init { restore(); refresh() }
    fun method(v: String) { if (_state.value.phase != WalkingTimerPhase.RUNNING) _state.value = _state.value.copy(method = v) }
    fun note(v: String) { _state.value = _state.value.copy(note = v, message = null); persist() }

    fun start() {
        if (_state.value.phase == WalkingTimerPhase.RUNNING) return
        if (_state.value.phase == WalkingTimerPhase.PAUSED) { resume(); return }
        bellSoundPlayer.stop(); accumulatedSeconds = 0; startedAtMillis = System.currentTimeMillis()
        _state.value = _state.value.copy(phase = WalkingTimerPhase.RUNNING, elapsed = 0, note = "", saved = false, message = null)
        persist(); startTicker()
    }
    fun pause() {
        if (_state.value.phase != WalkingTimerPhase.RUNNING) return
        updateElapsed(); job?.cancel(); accumulatedSeconds = _state.value.elapsed; startedAtMillis = null
        _state.value = _state.value.copy(phase = WalkingTimerPhase.PAUSED); persist()
    }
    fun resume() {
        if (_state.value.phase != WalkingTimerPhase.PAUSED) return
        startedAtMillis = System.currentTimeMillis(); _state.value = _state.value.copy(phase = WalkingTimerPhase.RUNNING)
        persist(); startTicker()
    }
    fun finish() {
        if (_state.value.phase != WalkingTimerPhase.RUNNING && _state.value.phase != WalkingTimerPhase.PAUSED) return
        if (_state.value.phase == WalkingTimerPhase.RUNNING) updateElapsed()
        job?.cancel(); accumulatedSeconds = _state.value.elapsed; startedAtMillis = null
        _state.value = _state.value.copy(phase = WalkingTimerPhase.FINISHED); persist()
    }
    private fun naturalFinish() {
        job?.cancel(); accumulatedSeconds = _state.value.targetSeconds; startedAtMillis = null
        _state.value = _state.value.copy(phase = WalkingTimerPhase.FINISHED, elapsed = _state.value.targetSeconds)
        persist(); bellSoundPlayer.play()
    }
    fun save() {
        val s = _state.value
        if (s.phase == WalkingTimerPhase.RUNNING || s.elapsed <= 0 || s.saving || s.saved) return
        viewModelScope.launch {
            _state.value = s.copy(saving = true, message = null)
            runCatchingSuspend { repo.saveWalking(WalkingSessionEntity(date = now(), durationSeconds = s.elapsed, method = s.method, observation = s.note.trim())) }
                .onSuccess {
                    clearPersistence(); accumulatedSeconds = 0; startedAtMillis = null
                    _state.value = s.copy(phase = WalkingTimerPhase.READY, elapsed = 0, note = "", saving = false, saved = true, message = "已保存经行 ${formatDuration(s.elapsed)}")
                    refresh()
                }
                .onFailure { _state.value = s.copy(saving = false, message = it.message ?: "保存失败，请稍后重试") }
        }
    }
    fun setBellSound(uri: Uri, displayName: String?) { bellSoundPlayer.setCustomSound(uri, displayName); previewBell() }
    fun useDefaultBellSound() = bellSoundPlayer.useDefaultSound()
    fun bellSoundName(): String = bellSoundPlayer.customSoundName ?: "软件内置引磬"
    fun previewBell() = bellSoundPlayer.play()

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { runCatchingSuspend { repo.deleteWalking(ids.toList()) }.onSuccess { _state.value = _state.value.copy(message = "已删除 ${ids.size} 条经行记录"); refresh() }.onFailure { _state.value = _state.value.copy(message = it.message ?: "删除失败") } }
    }
    private fun startTicker() {
        job?.cancel(); job = viewModelScope.launch {
            while (isActive && _state.value.phase == WalkingTimerPhase.RUNNING) {
                delay(250); updateElapsed()
                if (_state.value.elapsed >= _state.value.targetSeconds) { naturalFinish(); break }
            }
        }
    }
    private fun updateElapsed() {
        val s = _state.value
        if (s.phase != WalkingTimerPhase.RUNNING || startedAtMillis == null) return
        val elapsed = (accumulatedSeconds + ((System.currentTimeMillis() - (startedAtMillis ?: System.currentTimeMillis())) / 1000L).toInt()).coerceIn(0, s.targetSeconds)
        _state.value = s.copy(elapsed = elapsed); persist()
    }
    private fun restore() {
        if (!prefs.getBoolean("active", false)) return
        val phase = runCatching { WalkingTimerPhase.valueOf(prefs.getString("phase", WalkingTimerPhase.PAUSED.name) ?: WalkingTimerPhase.PAUSED.name) }.getOrDefault(WalkingTimerPhase.PAUSED)
        val storedElapsed = prefs.getInt("elapsed", 0).coerceAtLeast(0)
        val started = prefs.getLong("started_at", 0L)
        val live = if (phase == WalkingTimerPhase.RUNNING && started > 0L) storedElapsed + ((System.currentTimeMillis() - started) / 1000L).toInt() else storedElapsed
        val bounded = live.coerceIn(0, 3600)
        val restoredPhase = if (bounded >= 3600) WalkingTimerPhase.FINISHED else phase
        _state.value = WalkingUiState(method = prefs.getString("method", "全身行走") ?: "全身行走", phase = restoredPhase, elapsed = bounded, note = prefs.getString("note", "") ?: "")
        accumulatedSeconds = bounded
        if (restoredPhase == WalkingTimerPhase.RUNNING) { startedAtMillis = System.currentTimeMillis(); startTicker() }
    }
    private fun persist() {
        val s = _state.value
        if (s.phase == WalkingTimerPhase.READY && s.elapsed == 0 && s.note.isBlank()) { clearPersistence(); return }
        // Store elapsed accumulated up to this moment. On restore, only time after
        // started_at is added, so pause/resume and process recreation never double-count.
        val storedElapsed = if (s.phase == WalkingTimerPhase.RUNNING) accumulatedSeconds else s.elapsed
        prefs.edit().putBoolean("active", true).putString("method", s.method).putString("phase", s.phase.name)
            .putInt("elapsed", storedElapsed).putLong("started_at", if (s.phase == WalkingTimerPhase.RUNNING) (startedAtMillis ?: 0L) else 0L)
            .putString("note", s.note).apply()
    }
    private fun clearPersistence() { prefs.edit().clear().apply() }
    private fun refresh() { viewModelScope.launch { runCatchingSuspend { repo.getRecentWalking(50) }.onSuccess { _state.value = _state.value.copy(history = it) } } }
    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    private fun formatDuration(seconds: Int) = "${seconds / 60}分 ${seconds % 60}秒"
    override fun onCleared() { job?.cancel(); bellSoundPlayer.stop(); super.onCleared() }
}
