@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ruru.practice.feature.meditation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import com.ruru.practice.data.entity.MeditationSessionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    viewModel: MeditationSessionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showReflectionSheet by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var endVibrate by remember { mutableStateOf(viewModel.endVibrationEnabled) }
    val reflectionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var bellName by remember { mutableStateOf(viewModel.bellSoundName()) }
    var bellError by remember { mutableStateOf<String?>(null) }
    val bellPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = runCatching { context.contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull()
            viewModel.setBellSound(it, name).fold(
                onSuccess = {
                    val previewed = viewModel.previewBell()
                    bellName = viewModel.bellSoundName()
                    bellError = if (previewed) {
                        null
                    } else {
                        "音频已保存，但试听失败，请检查系统音量或重新选择。"
                    }
                },
                onFailure = { error ->
                    bellError = error.message ?: "本地音频保存失败，请重试。"
                }
            )
        }
    }
    val totalSeconds = state.selectedMinutes * 60
    val progress = if (totalSeconds == 0) 0f else state.elapsedSeconds.toFloat() / totalSeconds
    LaunchedEffect(state.phase) {
        if (state.phase == MeditationPhase.SAVED) showReflectionSheet = false
    }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("安般念", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("以自然呼吸为所缘，依十六步逐渐展开身、受、心、法四念处。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                Text("安般念十六步", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("先练熟前四步，再逐渐进入受、心、法三组。十六步是完整修习过程，不机械等同于四禅。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    AnapanStepCatalog.steps.forEach { step ->
                        AssistChip(onClick = { viewModel.selectStep(step.number) }, label = { Text("${step.number}") }, enabled = state.phase == MeditationPhase.READY || state.phase == MeditationPhase.COMPLETED || state.phase == MeditationPhase.SAVED)
                    }
                }
                val currentStep = AnapanStepCatalog.steps.first { it.number == state.practiceStep }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("第${currentStep.number}步 · ${currentStep.group}", fontWeight = FontWeight.SemiBold)
                        Text(currentStep.title, style = MaterialTheme.typography.titleLarge)
                        Text(currentStep.guidance, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("选择时长", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 5, 10, 15, 20, 30, 60, 120, 180).forEach { minutes ->
                        FilterChip(
                            selected = state.selectedMinutes == minutes,
                            onClick = { viewModel.selectMinutes(minutes) },
                            label = { Text("${minutes}分") },
                            enabled = state.phase == MeditationPhase.READY || state.phase == MeditationPhase.COMPLETED || state.phase == MeditationPhase.SAVED
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("练习中遇到问题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("昏沉：加强念、择法、精进、喜；必要时起身经行。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("掉举：加强轻安、定、舍，减少继续追逐刺激。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("分析太多：停止脑内讲解，回到自然呼吸与直接经验。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("出现特殊体验：知道它即可，不追逐，也不据此判断证果。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(timerLabel(state), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.phase != MeditationPhase.READY) {
                            Text(
                                "已练习 ${formatDuration(state.elapsedSeconds)} · ${if (state.phase == MeditationPhase.COMPLETED || state.phase == MeditationPhase.SAVED) "本次已结束" else "剩余 ${formatDuration(state.remainingSeconds)}"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("第${state.practiceStep}步 · ${AnapanStepCatalog.title(state.practiceStep)}", fontWeight = FontWeight.SemiBold)
                        Text(phaseLabel(state.phase), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TimerActions(state, viewModel, onStartRequest = {
                            if (state.selectedMinutes >= 30) showVolumeDialog = true else viewModel.start()
                        })
                        Text("计时与当前页面无关：切换页面或暂时离开后仍会继续；进程被系统回收后也会按已练习时长恢复。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("自然结束时播放三次引磬；暂停、手动完成和离开页面不会误响。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("提示音：$bellName（默认 432Hz 禅寺磬音）", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("结束时震动提醒", fontWeight = FontWeight.Medium)
                                Text("嗡～静～嗡～静～嗡，与短信等短震不同", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = endVibrate,
                                onCheckedChange = {
                                    endVibrate = it
                                    viewModel.endVibrationEnabled = it
                                }
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val previewed = viewModel.previewBell()
                                bellName = viewModel.bellSoundName()
                                bellError = if (previewed) {
                                    null
                                } else {
                                    "当前提示音无法播放，请重新选择音频或恢复默认。"
                                }
                            }) { Text("试听") }
                            OutlinedButton(onClick = { bellPicker.launch(arrayOf("audio/*")) }) { Text("选择本地音频") }
                            if (bellName != "软件内置引磬") TextButton(onClick = {
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
                        bellError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            if (state.phase == MeditationPhase.COMPLETED || state.phase == MeditationPhase.SAVED) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("本次练习已自动记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("次数和时长已经计入；写下这一刻只补充心得，不会重复计数。未写笔记时历史里暂不显示空白卡片，下次进入或刷新后仍会保留本次练习。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.phase == MeditationPhase.COMPLETED) {
                                Button(
                                    onClick = { showReflectionSheet = true },
                                    enabled = !state.saveState.isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (state.saveState.isSaving) "正在记入练习…" else "记录这一刻") }
                                state.saveState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                            else Text("这一刻已记录", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("练习历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            if (selectedIds.isNotEmpty()) {
                                TextButton(onClick = { confirmDelete = true }) { Text("删除(${selectedIds.size})") }
                            }
                        }
                        Text("仅保留本周记录，周一自动清除；也可勾选一条或多条删除。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (history.isEmpty()) {
                            Text("暂无练习记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                items(history, key = { it.id }) { session ->
                    HistoryCard(
                        session = session,
                        selected = session.id in selectedIds,
                        onSelectionChange = { checked ->
                            selectedIds = if (checked) selectedIds + session.id else selectedIds - session.id
                        }
                    )
                }
            }
        }
        if (showReflectionSheet && state.phase == MeditationPhase.COMPLETED) {
            ModalBottomSheet(
                onDismissRequest = { showReflectionSheet = false },
                sheetState = reflectionSheetState
            ) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("记录这一刻", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("这只是补充本次练习的观察，不会再次增加练习次数。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = state.observation, onValueChange = viewModel::updateObservation, label = { Text("本步练习中的观察（可选）") }, placeholder = { Text("例如：知道散乱后回到自然呼吸") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 6)
                    OutlinedTextField(value = state.afterState, onValueChange = viewModel::updateAfterState, label = { Text("练习后的身心状态（可选）") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 6)
                    state.saveState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showReflectionSheet = false }, modifier = Modifier.weight(1f)) { Text("取消") }
                        Button(onClick = viewModel::save, enabled = !state.saveState.isSaving, modifier = Modifier.weight(1f)) { Text(if (state.saveState.isSaving) "记录中…" else "保存") }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (showVolumeDialog) {
            val volumePercent = viewModel.mediaVolumePercent()
            val low = viewModel.isMediaMutedOrVeryLow()
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text("开始前请确认媒体音量") },
                text = {
                    Text(
                        if (low) {
                            "当前媒体音量约 ${volumePercent}%，几乎静音。座时 30 分钟以上结束时的引磬可能听不到，请先调高系统媒体音量后再开始。"
                        } else {
                            "当前媒体音量约 ${volumePercent}%。座时 30 分钟以上结束时会播放引磬；若音量过低可能听不到，确认无误后再开始。"
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showVolumeDialog = false
                        viewModel.start()
                    }) { Text("已确认，开始计时") }
                },
                dismissButton = {
                    TextButton(onClick = { showVolumeDialog = false }) { Text("返回调节") }
                }
            )
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("删除练习记录？") },
                text = { Text("将删除已选择的 ${selectedIds.size} 条记录，首页统计也会同步更新。删除后无法恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSessions(selectedIds)
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
private fun TimerActions(
    state: MeditationTimerState,
    viewModel: MeditationSessionViewModel,
    onStartRequest: () -> Unit = { viewModel.start() }
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.phase) {
            MeditationPhase.READY -> Button(onClick = onStartRequest) { Text("开始") }
            MeditationPhase.RUNNING -> {
                OutlinedButton(onClick = viewModel::pause) { Text("暂停") }
                Button(onClick = viewModel::finish) { Text("完成") }
            }
            MeditationPhase.PAUSED -> {
                Button(onClick = viewModel::resume) { Text("继续") }
                OutlinedButton(onClick = viewModel::finish) { Text("完成") }
            }
            MeditationPhase.COMPLETED -> {
                OutlinedButton(onClick = viewModel::reset) { Text("重新计时") }
            }
            MeditationPhase.SAVED -> {
                Button(onClick = viewModel::newSession) { Text("再练一次") }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    session: MeditationSessionEntity,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val observation = session.observation.trim()
    val afterState = session.afterState.trim()
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(checked = selected, onCheckedChange = onSelectionChange)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${session.date} · ${formatDuration(session.durationSeconds)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "第${session.practiceStep}步 · ${session.stepTitle}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (observation.isNotBlank()) {
                    Text("本步练习中的观察：\n$observation")
                }
                if (afterState.isNotBlank()) {
                    Text(
                        "练习后的身心状态：\n$afterState",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (observation.isBlank() && afterState.isBlank()) {
                    Text(
                        "仅记录练习完成；未写当下感受",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun timerLabel(state: MeditationTimerState): String {
    val seconds = when (state.phase) {
        MeditationPhase.READY -> state.selectedMinutes * 60
        MeditationPhase.RUNNING, MeditationPhase.PAUSED -> state.remainingSeconds
        MeditationPhase.COMPLETED, MeditationPhase.SAVED -> state.elapsedSeconds
    }
    val minutes = seconds / 60
    val rest = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${rest.toString().padStart(2, '0')}"
}

private fun phaseLabel(phase: MeditationPhase): String = when (phase) {
    MeditationPhase.READY -> "准备好后开始，保持自然呼吸"
    MeditationPhase.RUNNING -> "正在练习 · 轻轻知道呼吸"
    MeditationPhase.PAUSED -> "已暂停 · 需要时继续"
    MeditationPhase.COMPLETED -> "练习完成 · 留一点时间观察"
    MeditationPhase.SAVED -> "这一刻已记录"
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes > 0) "${minutes} 分 ${rest.toString().padStart(2, '0')} 秒" else "$rest 秒"
}
