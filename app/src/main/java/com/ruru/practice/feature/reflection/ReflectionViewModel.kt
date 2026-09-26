package com.ruru.practice.feature.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.ReflectionEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReflectionUiState(
    val content: String = "",
    val reflections: List<ReflectionEntity> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val repository: PracticeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReflectionUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun updateContent(value: String) {
        _state.value = _state.value.copy(content = value, message = null, error = null)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, message = null, error = null)
            runCatchingSuspend {
                require(current.content.isNotBlank()) { "请先写下一点复盘内容" }
                repository.saveReflection(ReflectionEntity(content = current.content.trim()))
            }.onSuccess {
                _state.value = ReflectionUiState(message = "已保存复盘")
                refresh()
            }.onFailure { error ->
                _state.value = _state.value.copy(isSaving = false, error = error.message ?: "保存失败")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val reflections = runCatchingSuspend { repository.getRecentReflections() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(reflections = reflections, isSaving = false)
        }
    }
}
