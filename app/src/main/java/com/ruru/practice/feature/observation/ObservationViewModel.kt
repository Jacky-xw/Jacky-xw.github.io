package com.ruru.practice.feature.observation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.data.entity.DependentOriginationEntity
import com.ruru.practice.data.entity.FiveAggregateEntity
import com.ruru.practice.data.entity.MindfulnessEventEntity
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ObservationUiState(
    val tab: Int = 0,
    val scene: String = "",
    val description: String = "",
    val feeling: String = "",
    val reaction: String = "",
    val awareness: String = "",
    val event: String = "",
    val rupa: String = "",
    val vedana: String = "",
    val sanna: String = "",
    val sankhara: String = "",
    val vinnana: String = "",
    val reflection: String = "",
    val trigger: String = "",
    val dependentFeeling: String = "",
    val craving: String = "",
    val grasping: String = "",
    val dependentReflection: String = "",
    val recentEvents: List<MindfulnessEventEntity> = emptyList(),
    val recentAggregates: List<FiveAggregateEntity> = emptyList(),
    val recentDependentOriginations: List<DependentOriginationEntity> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ObservationViewModel @Inject constructor(
    private val repository: PracticeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ObservationUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(tab = tab, message = null, error = null)
    }

    fun updateScene(value: String) = update { copy(scene = value) }
    fun updateDescription(value: String) = update { copy(description = value) }
    fun updateFeeling(value: String) = update { copy(feeling = value) }
    fun updateReaction(value: String) = update { copy(reaction = value) }
    fun updateAwareness(value: String) = update { copy(awareness = value) }
    fun updateEvent(value: String) = update { copy(event = value) }
    fun updateRupa(value: String) = update { copy(rupa = value) }
    fun updateVedana(value: String) = update { copy(vedana = value) }
    fun updateSanna(value: String) = update { copy(sanna = value) }
    fun updateSankhara(value: String) = update { copy(sankhara = value) }
    fun updateVinnana(value: String) = update { copy(vinnana = value) }
    fun updateReflection(value: String) = update { copy(reflection = value) }
    fun updateTrigger(value: String) = update { copy(trigger = value) }
    fun updateDependentFeeling(value: String) = update { copy(dependentFeeling = value) }
    fun updateCraving(value: String) = update { copy(craving = value) }
    fun updateGrasping(value: String) = update { copy(grasping = value) }
    fun updateDependentReflection(value: String) = update { copy(dependentReflection = value) }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, message = null, error = null)
            runCatchingSuspend {
                if (current.tab == 0) {
                    require(current.scene.isNotBlank()) { "请先填写发生场景" }
                    require(current.description.isNotBlank()) { "请先写下事件经过" }
                    repository.saveMindfulnessEvent(
                        MindfulnessEventEntity(
                            date = nowLabel(),
                            scene = current.scene.trim(),
                            description = current.description.trim(),
                            feeling = current.feeling.trim(),
                            reaction = current.reaction.trim(),
                            awareness = current.awareness.trim()
                        )
                    )
                } else if (current.tab == 1) {
                    require(current.event.isNotBlank()) { "请先填写观察事件" }
                    repository.saveFiveAggregate(
                        FiveAggregateEntity(
                            event = current.event.trim(),
                            rupa = current.rupa.trim(),
                            vedana = current.vedana.trim(),
                            sanna = current.sanna.trim(),
                            sankhara = current.sankhara.trim(),
                            vinnana = current.vinnana.trim(),
                            reflection = current.reflection.trim()
                        )
                    )
                } else {
                    require(current.trigger.isNotBlank()) { "请先填写触发点" }
                    repository.saveDependentOrigination(
                        DependentOriginationEntity(
                            trigger = current.trigger.trim(),
                            feeling = current.dependentFeeling.trim(),
                            craving = current.craving.trim(),
                            grasping = current.grasping.trim(),
                            reflection = current.dependentReflection.trim()
                        )
                    )
                }
            }.onSuccess {
                _state.value = ObservationUiState(
                    tab = current.tab,
                    recentEvents = _state.value.recentEvents,
                    recentAggregates = _state.value.recentAggregates,
                    recentDependentOriginations = _state.value.recentDependentOriginations,
                    message = "已保存观察"
                )
                refresh()
            }.onFailure { error ->
                _state.value = _state.value.copy(isSaving = false, error = error.message ?: "保存失败")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val events = runCatchingSuspend { repository.getRecentMindfulnessEvents() }.getOrDefault(emptyList())
            val aggregates = runCatchingSuspend { repository.getRecentFiveAggregates() }.getOrDefault(emptyList())
            val dependentOriginations = runCatchingSuspend { repository.getRecentDependentOriginations() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                recentEvents = events,
                recentAggregates = aggregates,
                recentDependentOriginations = dependentOriginations,
                isSaving = false
            )
        }
    }

    private fun update(transform: ObservationUiState.() -> ObservationUiState) {
        _state.value = transform(_state.value).copy(message = null, error = null)
    }

    private fun nowLabel(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
