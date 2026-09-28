package com.ruru.practice.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Publishes the completion of a weekly retention pass to visible screens.
 * The value is a version rather than a Boolean so a second Monday can be
 * observed without rebuilding any ViewModel.
 */
object WeeklyRolloverEvents {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun publish() {
        _version.update { it + 1L }
    }
}
