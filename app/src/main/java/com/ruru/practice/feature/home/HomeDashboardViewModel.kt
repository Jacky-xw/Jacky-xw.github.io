package com.ruru.practice.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruru.practice.core.util.runCatchingSuspend
import com.ruru.practice.domain.usecase.GetPracticeSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val getPracticeSummary: GetPracticeSummaryUseCase
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
                        meditationCount = summary.meditationCount,
                        totalMinutes = summary.totalMinutes,
                        recentPractices = summary.recentPractices,
                        todayMeditationCount = summary.todayMeditationCount,
                        todayWalkingCount = summary.todayWalkingCount,
                        todayRootCount = summary.todayRootCount,
                        todayObservationCount = summary.todayObservationCount,
                        todayHindranceCount = summary.todayHindranceCount,
                        todayPreceptCount = summary.todayPreceptCount,
                        sevenDayMeditationCount = summary.sevenDayMeditationCount,
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
}
