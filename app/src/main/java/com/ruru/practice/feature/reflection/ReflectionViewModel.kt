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
    val linkedPractice: String? = null,
    val recentPracticeLinks: List<String> = emptyList(),
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

    fun selectLinkedPractice(value: String?) {
        _state.value = _state.value.copy(linkedPractice = value, message = null, error = null)
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, message = null, error = null)
            runCatchingSuspend {
                require(current.content.isNotBlank()) { "请先写下一点复盘内容" }
                repository.saveReflection(
                    ReflectionEntity(
                        content = current.content.trim(),
                        linkedPractice = current.linkedPractice
                    )
                )
            }.onSuccess {
                _state.value = _state.value.copy(
                    content = "",
                    linkedPractice = null,
                    isSaving = false,
                    message = "已保存复盘"
                )
                refresh()
            }.onFailure { error ->
                _state.value = _state.value.copy(isSaving = false, error = error.message ?: "保存失败")
            }
        }
    }

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatchingSuspend { repository.deleteReflections(ids.toList()) }
                .onSuccess {
                    _state.value = _state.value.copy(message = "已删除 ${ids.size} 条复盘", error = null)
                    refresh()
                }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "删除失败") }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val reflections = runCatchingSuspend { repository.getRecentReflections() }.getOrDefault(emptyList())
            val links = buildRecentPracticeLinks()
            _state.value = _state.value.copy(reflections = reflections, recentPracticeLinks = links, isSaving = false)
        }
    }

    private suspend fun buildRecentPracticeLinks(): List<String> {
        data class Link(val date: String, val label: String)
        val links = buildList {
            runCatchingSuspend { repository.getRecentMeditations(8) }.getOrDefault(emptyList()).forEach {
                add(Link(it.date, "安般念 · ${it.date} · 第${it.practiceStep}步"))
            }
            runCatchingSuspend { repository.getRecentWalking(8) }.getOrDefault(emptyList()).forEach {
                add(Link(it.date, "经行 · ${it.date} · ${it.method}"))
            }
            runCatchingSuspend { repository.getRecentRootProtections(8) }.getOrDefault(emptyList()).forEach {
                add(Link(it.date, "护根 · ${it.date} · ${it.sense}"))
            }
        }
        return links.sortedByDescending { it.date }.map { it.label }.distinct().take(12)
    }
}
