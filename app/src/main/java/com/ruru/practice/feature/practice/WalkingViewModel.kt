package com.ruru.practice.feature.practice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class WalkingUiState(val method: String = "全身行走", val running: Boolean = false, val elapsed: Int = 0, val note: String = "", val saving: Boolean = false, val saved: Boolean = false, val message: String? = null, val history: List<WalkingSessionEntity> = emptyList())

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val repo: PracticeRepository,
    @ApplicationContext context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(WalkingUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var startedAtMillis: Long? = null
    private var pausedElapsed = 0

    init { restore(); refresh() }
    fun method(v: String) { if (!_state.value.running) _state.value = _state.value.copy(method = v) }
    fun note(v: String) { _state.value = _state.value.copy(note = v, message = null); persist() }
    fun start() {
        if (_state.value.running) return
        startedAtMillis = System.currentTimeMillis(); pausedElapsed = 0
        _state.value = _state.value.copy(running = true, elapsed = 0, note = "", saved = false, message = null)
        persist(); startTicker()
    }
    fun finish() {
        if (!_state.value.running) return
        updateElapsed(); job?.cancel(); startedAtMillis = null; pausedElapsed = _state.value.elapsed; _state.value = _state.value.copy(running = false); persist()
    }
    fun save() {
        val s = _state.value
        if (s.running || s.elapsed <= 0 || s.saving || s.saved) return
        viewModelScope.launch {
            _state.value = s.copy(saving = true, message = null)
            runCatchingSuspend { repo.saveWalking(WalkingSessionEntity(date = now(), durationSeconds = s.elapsed, method = s.method, observation = s.note.trim())) }
                .onSuccess {
                    clearPersistence()
                    _state.value = s.copy(running = false, elapsed = 0, note = "", saving = false, saved = true, message = "已保存经行；如需再练一次，请点开始")
                    refresh()
                }
                .onFailure { _state.value = s.copy(saving = false, message = it.message ?: "保存失败，请稍后重试") }
        }
    }

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatchingSuspend { repo.deleteWalking(ids.toList()) }
                .onSuccess {
                    _state.value = _state.value.copy(message = "已删除 ${ids.size} 条经行记录")
                    refresh()
                }
                .onFailure { _state.value = _state.value.copy(message = it.message ?: "删除失败") }
        }
    }
    private fun startTicker() {
        job?.cancel(); job = viewModelScope.launch { while (isActive && _state.value.running) { delay(250); updateElapsed() } }
    }
    private fun updateElapsed() {
        val s = _state.value
        if (!s.running || startedAtMillis == null) return
        val elapsed = (pausedElapsed + ((System.currentTimeMillis() - startedAtMillis!!) / 1000L).toInt()).coerceAtLeast(0)
        _state.value = s.copy(elapsed = elapsed); persist()
    }
    private fun restore() {
        if (!prefs.getBoolean("active", false)) return
        val elapsed = prefs.getInt("elapsed", 0).coerceAtLeast(0); val started = prefs.getLong("started_at", 0L)
        val current = if (started > 0L) elapsed + ((System.currentTimeMillis() - started) / 1000L).toInt() else elapsed
        _state.value = WalkingUiState(method = prefs.getString("method", "全身行走") ?: "全身行走", running = started > 0L, elapsed = current, note = prefs.getString("note", "") ?: "")
        pausedElapsed = elapsed
        if (started > 0L) { startedAtMillis = started; startTicker() }
    }
    private fun persist() {
        val s = _state.value
        if (s.elapsed <= 0 && !s.running && s.note.isBlank()) {
            prefs.edit().clear().apply()
            return
        }
        prefs.edit()
            .putBoolean("active", true)
            .putString("method", s.method)
            .putInt("elapsed", pausedElapsed)
            .putLong("started_at", startedAtMillis ?: 0L)
            .putString("note", s.note)
            .apply()
    }
    private fun clearPersistence() { prefs.edit().clear().apply() }
    private fun refresh() { viewModelScope.launch { runCatchingSuspend { repo.getRecentWalking() }.onSuccess { _state.value = _state.value.copy(history = it) } } }
    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    override fun onCleared() { job?.cancel(); super.onCleared() }
}
