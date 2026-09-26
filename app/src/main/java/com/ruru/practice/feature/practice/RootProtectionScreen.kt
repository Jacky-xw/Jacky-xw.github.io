package com.ruru.practice.feature.practice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RootProtectionScreen(
    viewModel: RootProtectionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("护根", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("白天遇到色、声、香、味、触、法时，知道接触与随后发展的受、爱、取。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("眼", "耳", "鼻", "舌", "身", "意").forEach { sense ->
                        FilterChip(selected = state.sense == sense, onClick = { viewModel.sense(sense) }, label = { Text(sense) })
                    }
                }
            }
            item { Field("接触", state.contact, viewModel::contact, "发生了什么接触？") }
            item { Field("受", state.feeling, viewModel::feeling, "苦、乐或不苦不乐？") }
            item { Field("爱", state.craving, viewModel::craving, "心想继续得到、排斥或改变什么？") }
            item { Field("取", state.grasping, viewModel::grasping, "正在抓住哪个故事、立场或对象？") }
            item { Field("回应", state.response, viewModel::response, "在哪一步停一下？准备怎样回应？", 3) }
            item {
                Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "保存中…" else "保存护根观察")
                }
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
            if (state.history.isEmpty()) {
                item { Text("还没有护根记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.history, key = { it.id }) { history ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(
                                checked = history.id in selectedIds,
                                onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + history.id else selectedIds - history.id }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${history.date} · ${history.sense}", fontWeight = FontWeight.SemiBold)
                                Text("触：${history.contact}")
                                Text("受：${history.feeling} · 爱：${history.craving}")
                                Text("取：${history.grasping}")
                                if (history.response.isNotBlank()) Text("回应：${history.response}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("删除护根记录？") },
                text = { Text("将删除已选择的 ${selectedIds.size} 条记录，删除后无法恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSelected(selectedIds)
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
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 2
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = { Text(placeholder) },
    minLines = minLines,
    modifier = Modifier.fillMaxWidth()
)
