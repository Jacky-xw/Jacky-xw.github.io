package com.ruru.practice.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.FiveCoverEntity
import com.ruru.practice.data.entity.PreceptEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PracticeLogUiState(
    val coverType: String = "贪欲",
    val coverIntensity: Int = 1,
    val coverTrigger: String = "",
    val coverResponse: String = "",
    val coverObservation: String = "",
    val preceptType: String = "不妄语",
    val preceptEvent: String = "",
    val preceptReflection: String = "",
    val recentCovers: List<FiveCoverEntity> = emptyList(),
    val recentPrecepts: List<PreceptEntity> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class PracticeLogViewModel @Inject constructor(
    private val repository: PracticeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PracticeLogUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun updateCoverType(value: String) = update { copy(coverType = value) }
    fun updateCoverIntensity(value: Int) = update { copy(coverIntensity = value) }
    fun updateCoverTrigger(value: String) = update { copy(coverTrigger = value) }
    fun updateCoverResponse(value: String) = update { copy(coverResponse = value) }
    fun updateCoverObservation(value: String) = update { copy(coverObservation = value) }
    fun updatePreceptType(value: String) = update { copy(preceptType = value) }
    fun updatePreceptEvent(value: String) = update { copy(preceptEvent = value) }
    fun updatePreceptReflection(value: String) = update { copy(preceptReflection = value) }

    fun saveCover() {
        val current = _state.value
        if (current.isSaving) return
        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, message = null, error = null)
            runCatchingSuspend {
                require(current.coverTrigger.isNotBlank()) { "请先写下触发条件" }
                repository.saveFiveCover(
                    FiveCoverEntity(
                        date = nowLabel(),
                        type = current.coverType,
                        intensity = current.coverIntensity,
                        trigger = current.coverTrigger.trim(),
                        response = current.coverResponse.trim(),
                        observation = current.coverObservation.trim()
                    )
                )
            }.onSuccess {
                _state.value = PracticeLogUiState(message = "已保存五盖记录")
                refresh()
            }.onFailure { error ->
                _state.value = _state.value.copy(isSaving = false, error = error.message ?: "保存失败")
            }
        }
    }

    fun savePrecept() {
        val current = _state.value
        if (current.isSaving) return
        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, message = null, error = null)
            runCatchingSuspend {
                require(current.preceptEvent.isNotBlank()) { "请先写下发生的事情" }
                repository.savePrecept(
                    PreceptEntity(
                        date = nowLabel(),
                        type = current.preceptType,
                        event = current.preceptEvent.trim(),
                        reflection = current.preceptReflection.trim()
                    )
                )
            }.onSuccess {
                _state.value = PracticeLogUiState(message = "已保存戒行记录")
                refresh()
            }.onFailure { error ->
                _state.value = _state.value.copy(isSaving = false, error = error.message ?: "保存失败")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val covers = runCatchingSuspend { repository.getRecentFiveCovers() }.getOrDefault(emptyList())
            val precepts = runCatchingSuspend { repository.getRecentPrecepts() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(recentCovers = covers, recentPrecepts = precepts, isSaving = false)
        }
    }

    private fun update(transform: PracticeLogUiState.() -> PracticeLogUiState) {
        _state.value = transform(_state.value).copy(message = null, error = null)
    }

    private fun nowLabel(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
