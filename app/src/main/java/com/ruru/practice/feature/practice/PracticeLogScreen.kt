package com.ruru.practice.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PracticeLogScreen(
    mode: Int,
    viewModel: PracticeLogViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedIds by remember(mode) { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember(mode) { mutableStateOf(false) }
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(if (mode == 0) "五盖记录" else "戒行记录", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (mode == 0) "看见障碍的条件，给回应留一点空间。" else "把戒行放回具体事件中，温和地练习。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                if (mode == 0) FiveCoverForm(state, viewModel) else PreceptForm(state, viewModel)
            }
            item {
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (selectedIds.isNotEmpty()) TextButton(onClick = { confirmDelete = true }) { Text("删除(${selectedIds.size})") }
                }
                Text("可勾选单条或多条记录后删除。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (mode == 0) {
                if (state.recentCovers.isEmpty()) item { Text("还没有五盖记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(state.recentCovers, key = { it.id }) { cover ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = cover.id in selectedIds, onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + cover.id else selectedIds - cover.id })
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${cover.date} · ${cover.type} · 强度 ${cover.intensity}/5", fontWeight = FontWeight.SemiBold)
                                Text("触发：${cover.trigger}")
                                if (cover.observation.isNotBlank()) Text("观察：${cover.observation}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                if (state.recentPrecepts.isEmpty()) item { Text("还没有戒行记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(state.recentPrecepts, key = { it.id }) { precept ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = precept.id in selectedIds, onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + precept.id else selectedIds - precept.id })
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${precept.date} · ${precept.type}", fontWeight = FontWeight.SemiBold)
                                Text(precept.event)
                                if (precept.reflection.isNotBlank()) Text("回看：${precept.reflection}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(if (mode == 0) "删除五盖记录？" else "删除戒行记录？") },
                text = { Text("将删除已选择的 ${selectedIds.size} 条记录，删除后无法恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSelected(mode, selectedIds)
                        selectedIds = emptySet()
                        confirmDelete = false
                    }) { Text("删除") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
            )
        }
    }
}

@Composable
private fun FiveCoverForm(state: PracticeLogUiState, viewModel: PracticeLogViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("是哪一种障碍？", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("贪欲", "瞋恚", "昏沉", "掉举", "疑").forEach { type ->
                FilterChip(selected = state.coverType == type, onClick = { viewModel.updateCoverType(type) }, label = { Text(type) })
            }
        }
        Text("强度：${state.coverIntensity}/5", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            (1..5).forEach { intensity ->
                FilterChip(selected = state.coverIntensity == intensity, onClick = { viewModel.updateCoverIntensity(intensity) }, label = { Text(intensity.toString()) })
            }
        }
        LogField("触发条件", state.coverTrigger, viewModel::updateCoverTrigger, "什么让它出现？", 2)
        LogField("我的回应", state.coverResponse, viewModel::updateCoverResponse, "我做了什么？", 2)
        LogField("回看", state.coverObservation, viewModel::updateCoverObservation, "下一次想提前看见什么？", 2)
        Button(onClick = viewModel::saveCover, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSaving) "保存中…" else "保存五盖记录")
        }
    }
}

@Composable
private fun PreceptForm(state: PracticeLogUiState, viewModel: PracticeLogViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("这次涉及哪一项？", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("不杀生", "不偷盗", "不邪淫", "不妄语", "不饮酒").forEach { type ->
                FilterChip(selected = state.preceptType == type, onClick = { viewModel.updatePreceptType(type) }, label = { Text(type) })
            }
        }
        LogField("发生的事情", state.preceptEvent, viewModel::updatePreceptEvent, "只记录具体行为", 3)
        LogField("回看与下一步", state.preceptReflection, viewModel::updatePreceptReflection, "如何更慈悲、诚实地回应？", 3)
        Button(onClick = viewModel::savePrecept, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSaving) "保存中…" else "保存戒行记录")
        }
    }
}

@Composable
private fun LogField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth()
    )
}
