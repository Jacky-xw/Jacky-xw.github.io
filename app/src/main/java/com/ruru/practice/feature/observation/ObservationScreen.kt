package com.ruru.practice.feature.observation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ObservationScreen(viewModel: ObservationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("观察中心", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("把经验拆开来看，反应就多一分空间。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(selected = state.tab == 0, onClick = { viewModel.selectTab(0) }, label = { Text("当下事件") })
                    FilterChip(selected = state.tab == 1, onClick = { viewModel.selectTab(1) }, label = { Text("五蕴拆解") })
                    FilterChip(selected = state.tab == 2, onClick = { viewModel.selectTab(2) }, label = { Text("缘起链条") })
                }
            }
            item {
                when (state.tab) {
                    0 -> MindfulnessForm(state, viewModel)
                    1 -> FiveAggregateForm(state, viewModel)
                    else -> DependentOriginationForm(state, viewModel)
                }
            }
            item {
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            if (state.tab == 0) {
                if (state.recentEvents.isEmpty()) item { Text("还没有当下观察。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(state.recentEvents, key = { it.id }) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${event.date} · ${event.scene}", fontWeight = FontWeight.SemiBold)
                            Text(event.description, maxLines = 3)
                            if (event.awareness.isNotBlank()) Text("觉察：${event.awareness}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (state.tab == 1) {
                if (state.recentAggregates.isEmpty()) item { Text("还没有五蕴拆解。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(state.recentAggregates, key = { it.id }) { aggregate ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(aggregate.event, fontWeight = FontWeight.SemiBold)
                            Text("色：${aggregate.rupa}  ·  受：${aggregate.vedana}", maxLines = 2)
                            Text("想：${aggregate.sanna}  ·  行：${aggregate.sankhara}", maxLines = 2)
                            Text("识：${aggregate.vinnana}", maxLines = 2)
                        }
                    }
                }
            } else {
                if (state.recentDependentOriginations.isEmpty()) item { Text("还没有缘起链条记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else items(state.recentDependentOriginations, key = { it.id }) { chain ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("触：${chain.trigger}  ·  受：${chain.feeling}", fontWeight = FontWeight.SemiBold)
                            Text("爱：${chain.craving}  ·  取：${chain.grasping}", maxLines = 2)
                            if (chain.reflection.isNotBlank()) Text("回看：${chain.reflection}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MindfulnessForm(state: ObservationUiState, viewModel: ObservationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Field("发生场景", state.scene, viewModel::updateScene, "例如：会议中被打断")
        Field("事件经过", state.description, viewModel::updateDescription, "只写事实，不急着解释", 3)
        Field("身体与感受", state.feeling, viewModel::updateFeeling, "紧、热、沉、轻？")
        Field("即时反应", state.reaction, viewModel::updateReaction, "我做了什么或想做什么？")
        Field("回看后的觉察", state.awareness, viewModel::updateAwareness, "看见了哪一个习惯？", 3)
        SaveButton(state.isSaving, viewModel::save)
    }
}

@Composable
private fun FiveAggregateForm(state: ObservationUiState, viewModel: ObservationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Field("观察事件", state.event, viewModel::updateEvent, "例如：收到一条批评消息")
        Field("色 · 身体", state.rupa, viewModel::updateRupa, "身体出现了什么？")
        Field("受 · 感受", state.vedana, viewModel::updateVedana, "苦、乐或不苦不乐？")
        Field("想 · 识别", state.sanna, viewModel::updateSanna, "我给它贴了什么标签？")
        Field("行 · 反应", state.sankhara, viewModel::updateSankhara, "冲动、倾向或行动？")
        Field("识 · 知道", state.vinnana, viewModel::updateVinnana, "此刻知道什么？")
        Field("回看", state.reflection, viewModel::updateReflection, "看见因缘之后，下一步是什么？", 3)
        SaveButton(state.isSaving, viewModel::save)
    }
}

@Composable
private fun DependentOriginationForm(state: ObservationUiState, viewModel: ObservationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Field("触 · 触发点", state.trigger, viewModel::updateTrigger, "什么被接触到了？")
        Field("受 · 感受", state.dependentFeeling, viewModel::updateDependentFeeling, "苦、乐或不苦不乐？")
        Field("爱 · 想要", state.craving, viewModel::updateCraving, "我想要什么改变？")
        Field("取 · 抓住", state.grasping, viewModel::updateGrasping, "我正在坚持哪个故事？")
        Field("回看", state.dependentReflection, viewModel::updateDependentReflection, "在哪个环节可以停一下？", 3)
        SaveButton(state.isSaving, viewModel::save)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1
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

@Composable
private fun SaveButton(isSaving: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
        Text(if (isSaving) "保存中…" else "保存观察")
    }
}
