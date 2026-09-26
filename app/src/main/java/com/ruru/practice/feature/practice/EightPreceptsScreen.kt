package com.ruru.practice.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EightPreceptsScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: EightPreceptsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val items = listOf(
        "不杀生：不故意伤害有情", "不偷盗：不取未与物", "不淫：一日一夜训练完全梵行",
        "不妄语：保持真实、善巧的言语", "不饮酒及放逸性麻醉物：保持心念清明",
        "不非时食：按所受八戒的具体时间规则持守", "不歌舞观听香华庄严：暂离娱乐与感官装饰",
        "不坐卧高广大床：减少奢华与舒适追逐"
    )
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("八戒／简化日", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("手册建议定期训练一日一夜的完全梵行，作为更深出离的经验。此页用于准备与自检。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(items.indices.toList()) { index ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.checked[index], onCheckedChange = { viewModel.toggle(index, it) })
                        Text(
                            items[index],
                            Modifier.weight(1f).padding(end = 8.dp),
                            softWrap = true
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.note, onValueChange = viewModel::updateNote,
                    modifier = Modifier.fillMaxWidth(), label = { Text("今日自检备注") }, minLines = 3
                )
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.height(12.dp))
                Text("重点是减少欲乐与放逸，让一天生活更适合闻法、静坐、经行与观察。八戒应按你所依止的可靠戒法与实际生活条件执行，不把勾选数量当作证果标准。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (selectedIds.isNotEmpty()) TextButton(onClick = { confirmDelete = true }) { Text("删除(${selectedIds.size})") }
                }
                Text("当天的勾选会自动保存，可选择单条或多条删除。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.history.isEmpty()) {
                item { Text("还没有八戒自检记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.history, key = { it.id }) { history ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = history.id in selectedIds,
                                onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + history.id else selectedIds - history.id }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(history.date, fontWeight = FontWeight.SemiBold)
                                Text("已勾选 ${Integer.bitCount(history.checkedMask)} / 8 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (history.note.isNotBlank()) Text(history.note)
                            }
                        }
                    }
                }
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("删除八戒自检记录？") },
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
