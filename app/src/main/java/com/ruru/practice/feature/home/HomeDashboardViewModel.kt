package com.ruru.practice.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.domain.usecase.PracticeRecordType
import com.ruru.practice.domain.usecase.GetPracticeSummaryUseCase
import com.ruru.practice.data.repository.PracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val getPracticeSummary: GetPracticeSummaryUseCase,
    private val repository: PracticeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeDashboardState())
    val state: StateFlow<HomeDashboardState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatchingSuspend { getPracticeSummary() }
                .onSuccess { summary ->
                    _state.value = HomeDashboardState(
                        isLoading = false,
                        totals = summary.totals,
                        recentPractices = summary.recentPractices,
                        todayMeditationCount = summary.todayMeditationCount,
                        todayWalkingCount = summary.todayWalkingCount,
                        todayRootCount = summary.todayRootCount,
                        todayObservationCount = summary.todayObservationCount,
                        todayHindranceCount = summary.todayHindranceCount,
                        todayPreceptCount = summary.todayPreceptCount,
                        sevenDayPracticeCount = summary.sevenDayPracticeCount,
                        sevenDayWalkingCount = summary.sevenDayWalkingCount,
                        strongestHindrance = summary.strongestHindrance,
                        hindranceGuidance = summary.hindranceGuidance,
                        nextAction = summary.nextAction
                    )
                }
                .onFailure { error ->
                    _state.value = HomeDashboardState(
                        isLoading = false,
                        errorMessage = error.message ?: "暂时无法读取练习记录"
                    )
                }
        }
    }

    /** Deletes mixed recent-practice rows through their source repository. */
    fun deleteSelected(keys: Set<String>) {
        if (keys.isEmpty()) return
        viewModelScope.launch {
            val selected = _state.value.recentPractices.filter { it.key in keys }
            runCatchingSuspend {
                for (record in selected) {
                    when (record.recordType) {
                        PracticeRecordType.MEDITATION -> repository.deleteMeditations(listOf(record.sourceId))
                        PracticeRecordType.WALKING -> repository.deleteWalking(listOf(record.sourceId))
                        PracticeRecordType.ROOT_PROTECTION -> repository.deleteRootProtections(listOf(record.sourceId))
                    }
                }
            }.onSuccess { refresh() }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "删除失败，请稍后重试"
                    )
                }
        }
    }
}
