package com.ruru.practice.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val items = listOf(
        "不杀生：不故意伤害有情", "不偷盗：不取未与物", "不淫：一日一夜训练完全梵行",
        "不妄语：保持真实、善巧的言语", "不饮酒及放逸性麻醉物：保持心念清明",
        "不非时食：按所受八戒的具体时间规则持守", "不歌舞观听香华庄严：暂离娱乐与感官装饰",
        "不坐卧高广大床：减少奢华与舒适追逐"
    )
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("八戒／简化日", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("手册建议定期训练一日一夜的完全梵行，作为更深出离的经验。此页用于准备与自检。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(items.indices.toList()) { index ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.checked[index], onCheckedChange = { viewModel.toggle(index, it) })
                        Text(items[index], Modifier.padding(end = 8.dp))
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
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
