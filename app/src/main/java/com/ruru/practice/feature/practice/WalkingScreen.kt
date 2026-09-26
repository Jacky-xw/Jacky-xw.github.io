package com.ruru.practice.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WalkingScreen(viewModel: WalkingViewModel = hiltViewModel(), modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(20.dp)); Text("经行", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("身体行走时，知道身体行走。经行本身就是正式修习。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Card { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("选择方法", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("全身行走", "脚步所缘").forEach { m -> FilterChip(state.method == m, { viewModel.method(m) }, label = { Text(m) }) }
            }
            Text("计时：${state.elapsed / 60}分 ${state.elapsed % 60}秒")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::start, enabled = !state.running) { Text("开始") }
                OutlinedButton(onClick = viewModel::finish, enabled = state.running) { Text("结束") }
            }
            OutlinedTextField(state.note, viewModel::note, label = { Text("经行后的观察") }, placeholder = { Text("散乱、昏沉、焦躁、身体感受、无常变化……") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(onClick = viewModel::save, enabled = state.elapsed > 0 && !state.saving, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "保存中…" else "保存经行") }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        } } }
        item { Text("练习要点", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item { InfoCard("站、走、停、转都不离正念", "到尽头先站稳，再以几个自然小步转身；走神后重新知道身体正在走。") }
        item { InfoCard("速度以清楚稳定为准", "昏沉可适当加快并挺直；焦躁时稳定脚掌落地感；不要把动作做得僵硬。") }
        item { InfoCard("与安般念衔接", "经行以身体行走为主，坐下后重新回到自然的入息与出息，不强迫脚步配合呼吸。") }
        item { Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(state.history, key = { it.id }) { h -> Card { Column(Modifier.padding(16.dp)) { Text("${h.date} · ${h.method}", fontWeight = FontWeight.SemiBold); Text("${h.durationSeconds / 60}分 ${h.durationSeconds % 60}秒"); if (h.observation.isNotBlank()) Text(h.observation) } } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
@Composable private fun InfoCard(title: String, text: String) { Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
