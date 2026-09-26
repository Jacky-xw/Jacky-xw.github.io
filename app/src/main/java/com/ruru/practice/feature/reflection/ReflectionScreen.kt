package com.ruru.practice.feature.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun ReflectionScreen(viewModel: ReflectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("复盘中心", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("用几句话把今天看清楚，变化会留下轨迹。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("关联最近练习（可选）", style = MaterialTheme.typography.labelLarge)
                        if (state.recentPracticeLinks.isEmpty()) {
                            Text("完成一次安般念、经行或护根后，这里可以把复盘和当次练习连起来。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            androidx.compose.foundation.layout.Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.linkedPractice == null,
                                    onClick = { viewModel.selectLinkedPractice(null) },
                                    label = { Text("不关联") }
                                )
                                state.recentPracticeLinks.forEach { link ->
                                    FilterChip(
                                        selected = state.linkedPractice == link,
                                        onClick = { viewModel.selectLinkedPractice(link) },
                                        label = { Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = state.content,
                            onValueChange = viewModel::updateContent,
                            label = { Text("今天想记下什么？") },
                            placeholder = { Text("发生了什么？我学到了什么？下一次想怎么做？") },
                            minLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.isSaving) "保存中…" else "保存复盘")
                        }
                        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("历史复盘", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (selectedIds.isNotEmpty()) TextButton(onClick = { confirmDelete = true }) { Text("删除(${selectedIds.size})") }
                }
                Text("可勾选单条或多条复盘后删除。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.reflections.isEmpty()) {
                item { Text("还没有复盘记录。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.reflections, key = { it.id }) { reflection ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(
                                checked = reflection.id in selectedIds,
                                onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + reflection.id else selectedIds - reflection.id }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(formatDate(reflection.createdAt), style = MaterialTheme.typography.labelLarge)
                                reflection.linkedPractice?.let { Text("关联练习：$it", color = MaterialTheme.colorScheme.primary) }
                                Text(reflection.content)
                            }
                        }
                    }
                }
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("删除复盘？") },
                text = { Text("将删除已选择的 ${selectedIds.size} 条复盘，删除后无法恢复。") },
                confirmButton = { TextButton(onClick = { viewModel.deleteSelected(selectedIds); selectedIds = emptySet(); confirmDelete = false }) { Text("删除") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(millis))
    return date
}
