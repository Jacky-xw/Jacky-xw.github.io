package com.ruru.practice.feature.practice

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruru.practice.core.ui.CollapsibleNote
import com.ruru.practice.core.util.NotificationPermission
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Performance: page shell does not collect [WalkingUiState.elapsed] every second.
 * Only [WalkingTimerCard] subscribes to tick fields — same isolation as 安般念.
 */
@Composable
fun WalkingScreen(
    viewModel: WalkingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val method by viewModel.state
        .map { it.method }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.method)
    val phase by viewModel.state
        .map { it.phase }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.phase)
    val note by viewModel.state
        .map { it.note }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.note)
    val saving by viewModel.state
        .map { it.saving }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.saving)
    val message by viewModel.state
        .map { it.message }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.message)
    val history by viewModel.state
        .map { it.history }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.history)

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var bellName by remember { mutableStateOf(viewModel.bellSoundName()) }
    var bellError by remember { mutableStateOf<String?>(null) }
    val bellPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        viewModel.setBellSound(uri, name).fold(
            onSuccess = {
                val previewed = viewModel.previewBell()
                bellName = viewModel.bellSoundName()
                bellError = if (previewed) null else "音频已保存，但试听失败，请检查系统音量或重新选择。"
            },
            onFailure = { error ->
                bellError = error.message ?: "本地音频保存失败，请重试。"
            }
        )
    }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("经行", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "身体行走时，知道身体行走。经行本身就是正式修习。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item(key = "method", contentType = "method") {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("选择方法", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("全身行走", "脚步所缘").forEach { m ->
                                FilterChip(
                                    selected = method == m,
                                    onClick = { viewModel.method(m) },
                                    label = { Text(m) }
                                )
                            }
                        }
                    }
                }
            }
            item(key = "timer", contentType = "timer") {
                WalkingTimerCard(
                    viewModel = viewModel,
                    onRequestStart = {
                        if (!NotificationPermission.isGranted(context)) {
                            showNotificationPermissionDialog = true
                        } else {
                            viewModel.start()
                        }
                    }
                )
            }
            item(key = "tips", contentType = "tips") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard("站、走、停、转都不离正念", "到尽头先站稳，再以几个自然小步转身；走神后重新知道身体正在走。")
                    InfoCard("速度以清楚稳定为准", "昏沉可适当加快并挺直；焦躁时稳定脚掌落地感；不要把动作做得僵硬。")
                    InfoCard("与安般念衔接", "经行以身体行走为主，坐下后重新回到自然的入息与出息，不强迫脚步配合呼吸。")
                }
            }
            item(key = "note_bell", contentType = "note") {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("结束震动与安般念共用开关（默认开启，模式：嗡～静～嗡～静～嗡）。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("提示音与安般念同步：$bellName", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val previewed = viewModel.previewBell()
                                bellName = viewModel.bellSoundName()
                                bellError = if (previewed) null else "当前提示音无法播放，请重新选择音频或恢复默认。"
                            }) { Text("试听") }
                            OutlinedButton(onClick = { bellPicker.launch(arrayOf("audio/*")) }) { Text("选择本地音频") }
                            if (bellName != "软件内置引磬") {
                                TextButton(onClick = {
                                    viewModel.useDefaultBellSound().fold(
                                        onSuccess = {
                                            bellName = viewModel.bellSoundName()
                                            bellError = null
                                        },
                                        onFailure = { error ->
                                            bellError = error.message ?: "恢复默认提示音失败，请重试。"
                                        }
                                    )
                                }) { Text("恢复默认") }
                            }
                        }
                        bellError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (phase == WalkingTimerPhase.FINISHED || phase == WalkingTimerPhase.PAUSED) {
                            OutlinedTextField(
                                value = note,
                                onValueChange = viewModel::note,
                                label = { Text("经行后的观察") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { viewModel.save() },
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (saving) "保存中…" else "保存经行记录")
                            }
                        }
                        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item(key = "hist_header", contentType = "hist_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("经行历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("删除(${selectedIds.size})")
                        }
                    }
                }
                Text(
                    "仅保留本周记录，周一自动清除；也可勾选删除。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (history.isEmpty()) {
                item(key = "hist_empty") {
                    Text("还没有经行记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(history, key = { it.id }, contentType = { "walk_hist" }) { h ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = h.id in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + h.id else selectedIds - h.id
                                }
                            )
                            Column(modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${h.date} · ${h.method}", fontWeight = FontWeight.SemiBold)
                                Text("${h.durationSeconds / 60}分 ${h.durationSeconds % 60}秒")
                                if (h.observation.isNotBlank()) {
                                    CollapsibleNote(
                                        label = "经行后的观察",
                                        value = h.observation,
                                        secondary = true,
                                        collapsedMaxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = { Text("需要通知权限") },
            text = {
                Text("未开启通知权限时，无法启动经行计时。\n\n后台到时提醒需要通知权限（用于计时保活与到时提示）。引磬本身走闹钟音量。请先允许通知权限后，再开始计时。")
            },
            confirmButton = {
                Button(onClick = {
                    showNotificationPermissionDialog = false
                    runCatching {
                        context.startActivity(NotificationPermission.appNotificationSettingsIntent(context))
                    }.onFailure {
                        runCatching {
                            context.startActivity(NotificationPermission.appDetailsSettingsIntent(context))
                        }
                    }
                }) { Text("去开启通知") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionDialog = false }) { Text("取消") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除经行记录？") },
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

@Composable
private fun WalkingTimerCard(
    viewModel: WalkingViewModel,
    onRequestStart: () -> Unit
) {
    val phase by viewModel.state
        .map { it.phase }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.phase)
    val elapsed by viewModel.state
        .map { it.elapsed }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.elapsed)
    val targetSeconds by viewModel.state
        .map { it.targetSeconds }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.targetSeconds)

    val remaining = (targetSeconds - elapsed).coerceAtLeast(0)
    val mm = remaining / 60
    val ss = remaining % 60

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "%02d:%02d".format(mm, ss),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                when (phase) {
                    WalkingTimerPhase.READY -> "准备好后开始经行"
                    WalkingTimerPhase.RUNNING -> "经行进行中"
                    WalkingTimerPhase.PAUSED -> "已暂停"
                    WalkingTimerPhase.FINISHED -> "本段经行已结束"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (phase) {
                    WalkingTimerPhase.READY, WalkingTimerPhase.FINISHED -> {
                        Button(onClick = onRequestStart) { Text("开始") }
                    }
                    WalkingTimerPhase.RUNNING -> {
                        OutlinedButton(onClick = { viewModel.pause() }) { Text("暂停") }
                        Button(onClick = { viewModel.finish() }) { Text("结束") }
                    }
                    WalkingTimerPhase.PAUSED -> {
                        Button(onClick = { viewModel.resume() }) { Text("继续") }
                        OutlinedButton(onClick = { viewModel.finish() }) { Text("结束") }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
