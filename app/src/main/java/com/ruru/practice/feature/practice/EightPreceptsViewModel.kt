package com.ruru.practice.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.EightPreceptSessionEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class EightPreceptsUiState(
    val checked: List<Boolean> = List(8) { false },
    val note: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val history: List<EightPreceptSessionEntity> = emptyList()
)

@HiltViewModel
class EightPreceptsViewModel @Inject constructor(
    private val repository: PracticeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EightPreceptsUiState())
    val state = _state.asStateFlow()
    private var saveJob: Job? = null
    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    private var editRevision = 0L

    init {
        load()
    }

    fun toggle(index: Int, value: Boolean) {
        val next = _state.value.checked.toMutableList()
        next[index] = value
        editRevision++
        _state.value = _state.value.copy(checked = next, message = null)
        scheduleSave()
    }

    fun updateNote(value: String) {
        editRevision++
        _state.value = _state.value.copy(note = value, message = null)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            // A short debounce turns rapid checkbox/note edits into one local
            // write and avoids creating a new row for every keystroke.
            delay(220)
            saveMutex.withLock {
                val current = _state.value
                _state.value = current.copy(saving = true)
                val mask = current.checked.foldIndexed(0) { index, acc, checked -> if (checked) acc or (1 shl index) else acc }
                val result = runCatchingSuspend {
                    val existing = repository.getEightPreceptForDate(today())
                    if (existing == null) {
                        repository.saveEightPreceptSession(
                            EightPreceptSessionEntity(
                                date = today(),
                                checkedMask = mask,
                                note = current.note.trim()
                            )
                        )
                    } else {
                        repository.updateEightPreceptSession(existing.id, mask, current.note.trim())
                    }
                }
                if (result.isSuccess) {
                    val history = runCatchingSuspend { repository.getRecentEightPreceptSessions() }.getOrDefault(emptyList())
                    _state.value = _state.value.copy(saving = false, history = history, message = "已保存今日八戒自检")
                } else {
                    _state.value = _state.value.copy(saving = false, message = "保存失败：${result.exceptionOrNull()?.message ?: "请稍后重试"}")
                }
            }
        }
    }

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        saveJob?.cancel()
        viewModelScope.launch {
            val result = runCatchingSuspend { repository.deleteEightPreceptSessions(ids.toList()) }
            if (result.isSuccess) {
                val history = runCatchingSuspend { repository.getRecentEightPreceptSessions() }.getOrDefault(emptyList())
                val currentDay = history.firstOrNull { it.date == today() }
                if (currentDay == null) {
                    _state.value = _state.value.copy(
                        checked = List(8) { false }, note = "", message = "已删除 ${ids.size} 条记录", history = history
                    )
                } else {
                    _state.value = _state.value.copy(
                        checked = maskToList(currentDay.checkedMask), note = currentDay.note,
                        message = "已删除 ${ids.size} 条记录", history = history
                    )
                }
            } else {
                _state.value = _state.value.copy(message = "删除失败：${result.exceptionOrNull()?.message ?: "请稍后重试"}")
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val revision = editRevision
            val saved = runCatchingSuspend { repository.getEightPreceptForDate(today()) }.getOrNull()
            val history = runCatchingSuspend { repository.getRecentEightPreceptSessions() }.getOrDefault(emptyList())
            if (revision == editRevision) {
                _state.value = if (saved == null) {
                    EightPreceptsUiState(history = history)
                } else {
                    EightPreceptsUiState(maskToList(saved.checkedMask), saved.note, history = history)
                }
            } else {
                _state.value = _state.value.copy(history = history)
            }
        }
    }

    private fun maskToList(mask: Int): List<Boolean> = List(8) { (mask and (1 shl it)) != 0 }
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
