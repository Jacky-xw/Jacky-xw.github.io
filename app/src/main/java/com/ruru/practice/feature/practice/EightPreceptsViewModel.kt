package com.ruru.practice.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.data.entity.EightPreceptSessionEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class EightPreceptsUiState(
    val checked: List<Boolean> = List(8) { false },
    val note: String = "",
    val saving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class EightPreceptsViewModel @Inject constructor(
    private val repository: PracticeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EightPreceptsUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun toggle(index: Int, value: Boolean) {
        val next = _state.value.checked.toMutableList()
        next[index] = value
        _state.value = _state.value.copy(checked = next, message = null)
        save()
    }

    fun updateNote(value: String) {
        _state.value = _state.value.copy(note = value, message = null)
        save()
    }

    private fun load() {
        viewModelScope.launch {
            val saved = repository.getEightPreceptForDate(today()) ?: return@launch
            _state.value = EightPreceptsUiState(maskToList(saved.checkedMask), saved.note)
        }
    }

    private fun save() {
        val current = _state.value
        viewModelScope.launch {
            _state.value = current.copy(saving = true)
            val mask = current.checked.foldIndexed(0) { index, acc, checked -> if (checked) acc or (1 shl index) else acc }
            runCatching { repository.saveEightPreceptSession(EightPreceptSessionEntity(date = today(), checkedMask = mask, note = current.note.trim())) }
                .onSuccess { _state.value = _state.value.copy(saving = false, message = "已保存今日八戒自检") }
                .onFailure { _state.value = _state.value.copy(saving = false, message = "保存失败：${it.message ?: "请稍后重试"}") }
        }
    }

    private fun maskToList(mask: Int): List<Boolean> = List(8) { (mask and (1 shl it)) != 0 }
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
