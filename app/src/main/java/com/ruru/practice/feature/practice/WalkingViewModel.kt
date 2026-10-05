package com.ruru.practice.feature.practice

import com.ruru.practice.core.util.NotificationPermission

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.feature.meditation.MeditationKeepAliveService
import com.ruru.practice.core.audio.EndSessionAlert
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
    val method: String = "全身行走",
    val phase: WalkingTimerPhase = WalkingTimerPhase.READY,
    val targetSeconds: Int = 60 * 60,
    val elapsed: Int = 0,
    val note: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null,
    val history: List<WalkingSessionEntity> = emptyList()
) {
    val remaining: Int get() = (targetSeconds - elapsed).coerceAtLeast(0)
}

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val repo: PracticeRepository,
    private val bellSoundPlayer: BellSoundPlayer,
    private val endSessionAlert: EndSessionAlert,
    @ApplicationContext context: Context
) : ViewModel() {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(WalkingUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var startedAtMillis: Long? = null
    private var accumulatedSeconds = 0
    private var isFinishing = false

    init {
        restore()
        refresh()
        viewModelScope.launch {
            WalkingTimerBridge.naturalEndDue.collect {
                if (_state.value.phase == WalkingTimerPhase.RUNNING ||
                    _state.value.phase == WalkingTimerPhase.PAUSED
                ) {
                    naturalFinish(alertAlreadyStarted = true)
                }
            }
        }
    }

    fun setScreenActive(@Suppress("UNUSED_PARAMETER") active: Boolean) = Unit
    fun method(v: String) {
        if (_state.value.phase != WalkingTimerPhase.RUNNING) {
            _state.value = _state.value.copy(method = v)
        }
    }
    fun note(v: String) {
        // Do not persist on every keystroke — IO on main path hurts scroll/typing.
        _state.value = _state.value.copy(note = v, message = null)
    }

    fun start() {
        if (!NotificationPermission.isGranted(appContext)) return
        if (_state.value.phase == WalkingTimerPhase.RUNNING) return
        if (_state.value.phase == WalkingTimerPhase.PAUSED) {
            resume()
            return
        }
        bellSoundPlayer.stop()
        accumulatedSeconds = 0
        isFinishing = false
        startedAtMillis = System.currentTimeMillis()
        _state.value = _state.value.copy(
            phase = WalkingTimerPhase.RUNNING,
            elapsed = 0,
            note = "",
            saved = false,
            message = null
        )
        persist()
        startTicker()
        scheduleWallClockEnd()
    }

    fun pause() {
        if (_state.value.phase != WalkingTimerPhase.RUNNING) return
        updateElapsed()
        job?.cancel()
        cancelWallClockEnd()
        accumulatedSeconds = _state.value.elapsed
        startedAtMillis = null
        _state.value = _state.value.copy(phase = WalkingTimerPhase.PAUSED)
        persist()
    }

    fun resume() {
        if (_state.value.phase != WalkingTimerPhase.PAUSED) return
        startedAtMillis = System.currentTimeMillis()
        _state.value = _state.value.copy(phase = WalkingTimerPhase.RUNNING)
        persist()
        startTicker()
        scheduleWallClockEnd()
    }

    fun finish() {
        if (_state.value.phase != WalkingTimerPhase.RUNNING &&
            _state.value.phase != WalkingTimerPhase.PAUSED
        ) return
        if (_state.value.phase == WalkingTimerPhase.RUNNING) updateElapsed()
        job?.cancel()
        cancelWallClockEnd()
        startedAtMillis = null
        _state.value = _state.value.copy(phase = WalkingTimerPhase.FINISHED)
        persist()
    }

    private fun naturalFinish(alertAlreadyStarted: Boolean = false) {
        if (isFinishing) return
        val phase = _state.value.phase
        if (phase != WalkingTimerPhase.RUNNING && phase != WalkingTimerPhase.PAUSED) return
        isFinishing = true
        if (phase == WalkingTimerPhase.RUNNING) updateElapsed()
        val target = _state.value.targetSeconds
        if (_state.value.elapsed < target) {
            _state.value = _state.value.copy(elapsed = target)
        }
        job?.cancel()
        cancelWallClockEnd()
        startedAtMillis = null
        _state.value = _state.value.copy(phase = WalkingTimerPhase.FINISHED, elapsed = target)
        persist()
        if (!alertAlreadyStarted) {
            bellSoundPlayer.play(alarm = true)
            endSessionAlert.vibrateEndPattern()
        }
        isFinishing = false
    }

    fun save() {
        val s = _state.value
        if (s.phase == WalkingTimerPhase.RUNNING || s.elapsed <= 0 || s.saving || s.saved) return
        _state.value = s.copy(saving = true, message = null)
        viewModelScope.launch {
            runCatchingSuspend {
                repo.saveWalking(
                    WalkingSessionEntity(
                        date = now(),
                        durationSeconds = s.elapsed,
                        method = s.method,
                        observation = s.note.trim()
                    )
                )
            }.onSuccess {
                clearPersistence()
                accumulatedSeconds = 0
                startedAtMillis = null
                cancelWallClockEnd()
                _state.value = s.copy(
                    phase = WalkingTimerPhase.READY,
                    elapsed = 0,
                    note = "",
                    saving = false,
                    saved = true,
                    message = "已保存经行 ${formatDuration(s.elapsed)}"
                )
                refresh()
            }.onFailure {
                _state.value = s.copy(saving = false, message = it.message ?: "保存失败，请稍后重试")
            }
        }
    }

    fun setBellSound(uri: Uri, displayName: String?): Result<Unit> =
        bellSoundPlayer.setCustomSound(uri, displayName)
    fun useDefaultBellSound(): Result<Unit> = bellSoundPlayer.useDefaultSound()
    fun bellSoundName(): String = bellSoundPlayer.customSoundName ?: "软件内置引磬"
    fun previewBell(): Boolean = bellSoundPlayer.play()
    var endVibrationEnabled: Boolean
        get() = endSessionAlert.vibrationEnabled
        set(value) { endSessionAlert.vibrationEnabled = value }

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatchingSuspend { repo.deleteWalking(ids.toList()) }
                .onSuccess {
                    _state.value = _state.value.copy(message = "已删除 ${ids.size} 条经行记录")
                    refresh()
                }
                .onFailure {
                    _state.value = _state.value.copy(message = it.message ?: "删除失败")
                }
        }
    }

    private fun scheduleWallClockEnd() {
        val s = _state.value
        if (s.phase != WalkingTimerPhase.RUNNING) {
            WalkingEndScheduler.cancel(appContext)
            MeditationKeepAliveService.stop(appContext)
            return
        }
        val remaining = (s.targetSeconds - s.elapsed).coerceAtLeast(0)
        val endAt = System.currentTimeMillis() + remaining * 1000L
        WalkingEndScheduler.schedule(appContext, endAt)
        // Share the meditation FGS countdown so background end is reliable.
        MeditationKeepAliveService.schedule(appContext, endAtMillis = endAt, longSeat = false)
    }

    private fun cancelWallClockEnd() {
        WalkingEndScheduler.cancel(appContext)
        MeditationKeepAliveService.stop(appContext)
    }

    private fun startTicker() {
        job?.cancel()
        job = viewModelScope.launch {
            while (isActive && _state.value.phase == WalkingTimerPhase.RUNNING) {
                delay(1000)
                updateElapsed()
                if (_state.value.elapsed >= _state.value.targetSeconds) {
                    naturalFinish()
                    break
                }
            }
        }
    }

    private fun updateElapsed() {
        val s = _state.value
        if (s.phase != WalkingTimerPhase.RUNNING || startedAtMillis == null) return
        val elapsed = (
            accumulatedSeconds +
                ((System.currentTimeMillis() - (startedAtMillis ?: System.currentTimeMillis())) / 1000L).toInt()
            ).coerceIn(0, s.targetSeconds)
        if (elapsed == s.elapsed) return
        _state.value = s.copy(elapsed = elapsed)
    }

    private fun restore() {
        if (!prefs.getBoolean("active", false)) return
        val phase = runCatching {
            WalkingTimerPhase.valueOf(
                prefs.getString("phase", WalkingTimerPhase.PAUSED.name) ?: WalkingTimerPhase.PAUSED.name
            )
        }.getOrDefault(WalkingTimerPhase.PAUSED)
        val target = prefs.getInt("target", 60 * 60).coerceAtLeast(1)
        val storedElapsed = prefs.getInt("elapsed", 0).coerceAtLeast(0)
        val started = prefs.getLong("started_at", 0L)
        val live = if (phase == WalkingTimerPhase.RUNNING && started > 0L) {
            storedElapsed + ((System.currentTimeMillis() - started) / 1000L).toInt()
        } else storedElapsed
        val bounded = live.coerceAtMost(target)
        val reachedNaturalEnd = phase == WalkingTimerPhase.RUNNING && bounded >= target
        val restoredPhase = if (reachedNaturalEnd) WalkingTimerPhase.FINISHED else phase
        _state.value = WalkingUiState(
            method = prefs.getString("method", "全身行走") ?: "全身行走",
            phase = restoredPhase,
            targetSeconds = target,
            elapsed = bounded,
            note = prefs.getString("note", "") ?: ""
        )
        accumulatedSeconds = bounded
        if (reachedNaturalEnd) {
            startedAtMillis = null
            persist()
            bellSoundPlayer.play(alarm = true)
            endSessionAlert.vibrateEndPattern()
        } else if (restoredPhase == WalkingTimerPhase.RUNNING) {
            startedAtMillis = System.currentTimeMillis()
            startTicker()
            scheduleWallClockEnd()
        }
    }

    private fun persist() {
        val s = _state.value
        if (s.phase == WalkingTimerPhase.READY && s.elapsed == 0 && s.note.isBlank()) {
            clearPersistence()
            return
        }
        val storedElapsed = if (s.phase == WalkingTimerPhase.RUNNING) accumulatedSeconds else s.elapsed
        prefs.edit()
            .putBoolean("active", true)
            .putString("method", s.method)
            .putString("phase", s.phase.name)
            .putInt("target", s.targetSeconds)
            .putInt("elapsed", storedElapsed)
            .putLong("started_at", if (s.phase == WalkingTimerPhase.RUNNING) (startedAtMillis ?: 0L) else 0L)
            .putString("note", s.note)
            .apply()
    }

    private fun clearPersistence() { prefs.edit().clear().apply() }
    fun refresh() {
        viewModelScope.launch {
            runCatchingSuspend { repo.getRecentWalking(50) }
                .onSuccess { _state.value = _state.value.copy(history = it) }
        }
    }
    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    private fun formatDuration(seconds: Int) = "${seconds / 60}分 ${seconds % 60}秒"

    override fun onCleared() {
        if (_state.value.phase == WalkingTimerPhase.RUNNING ||
            _state.value.phase == WalkingTimerPhase.PAUSED
        ) {
            persist()
        }
        job?.cancel()
        super.onCleared()
    }
}
