package com.ruru.practice.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.RootProtectionEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RootProtectionUiState(
    val sense: String = "眼",
    val contact: String = "",
    val feeling: String = "",
    val craving: String = "",
    val grasping: String = "",
    val response: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val history: List<RootProtectionEntity> = emptyList()
)

@HiltViewModel
class RootProtectionViewModel @Inject constructor(
    private val repository: PracticeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RootProtectionUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun sense(value: String) = update { copy(sense = value) }
    fun contact(value: String) = update { copy(contact = value) }
    fun feeling(value: String) = update { copy(feeling = value) }
    fun craving(value: String) = update { copy(craving = value) }
    fun grasping(value: String) = update { copy(grasping = value) }
    fun response(value: String) = update { copy(response = value) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        viewModelScope.launch {
            _state.value = current.copy(saving = true, message = null, error = null)
            runCatchingSuspend {
                require(current.contact.isNotBlank()) { "请先写下发生了什么接触" }
                repository.saveRootProtection(
                    RootProtectionEntity(
                        date = now(),
                        sense = current.sense,
                        contact = current.contact.trim(),
                        feeling = current.feeling.trim(),
                        craving = current.craving.trim(),
                        grasping = current.grasping.trim(),
                        response = current.response.trim()
                    )
                )
            }.onSuccess {
                _state.value = RootProtectionUiState(message = "已保存护根观察")
                refresh()
            }.onFailure { error ->
                _state.value = _state.value.copy(saving = false, error = error.message ?: "保存失败")
            }
        }
    }

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatchingSuspend { repository.deleteRootProtections(ids.toList()) }
                .onSuccess {
                    _state.value = _state.value.copy(message = "已删除 ${ids.size} 条记录", error = null)
                    refresh()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error.message ?: "删除失败")
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val history = runCatchingSuspend { repository.getRecentRootProtections() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(history = history, saving = false)
        }
    }

    private fun update(transform: RootProtectionUiState.() -> RootProtectionUiState) {
        _state.value = transform(_state.value).copy(message = null, error = null)
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
