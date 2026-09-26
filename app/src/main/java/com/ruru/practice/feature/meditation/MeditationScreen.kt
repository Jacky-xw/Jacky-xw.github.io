package com.ruru.practice.feature.meditation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    val reflectionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setScreenActive(true)
                Lifecycle.Event.ON_STOP -> viewModel.setScreenActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.setScreenActive(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setScreenActive(false)
        }
    }
    val totalSeconds = state.selectedMinutes * 60
    val progress = if (totalSeconds == 0) 0f else state.elapsedSeconds.toFloat() / totalSeconds

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
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
                    listOf(1, 5, 10, 15, 20).forEach { minutes ->
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
                        TimerActions(state, viewModel)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("自然结束时轻响一次；暂停、手动完成和离开页面不会误响。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = viewModel::previewBell) { Text("试听引磬") }
                        }
                    }
                }
            }

            if (state.phase == MeditationPhase.COMPLETED || state.phase == MeditationPhase.SAVED) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("本次练习已自动记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("次数和时长已经计入；写下这一刻只补充心得，不会重复计数。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.phase == MeditationPhase.COMPLETED) Button(onClick = { showReflectionSheet = true }, modifier = Modifier.fillMaxWidth()) { Text("记录这一刻") }
                            else Text("这一刻已记录", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("练习历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { confirmDelete = true }) { Text("删除(${selectedIds.size})") }
                    }
                }
                Text("勾选一条或多条记录后可删除。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (history.isEmpty()) {
                item { Text("完成一次练习后，记录会显示在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
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
            item { Spacer(Modifier.height(24.dp)) }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showReflectionSheet = false }, modifier = Modifier.weight(1f)) { Text("取消") }
                        Button(onClick = { viewModel.save(); showReflectionSheet = false }, enabled = !state.saveState.isSaving, modifier = Modifier.weight(1f)) { Text(if (state.saveState.isSaving) "记录中…" else "保存") }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
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
private fun TimerActions(state: MeditationTimerState, viewModel: MeditationSessionViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.phase) {
            MeditationPhase.READY -> Button(onClick = viewModel::start) { Text("开始") }
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
private fun CompletionCard(state: MeditationTimerState, viewModel: MeditationSessionViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (state.phase == MeditationPhase.SAVED) "练习已完成" else "写下这一刻", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.observation,
                onValueChange = viewModel::updateObservation,
                label = { Text("本步练习中的观察（可选）") },
                placeholder = { Text("例如：知道散乱后回到自然呼吸") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 4,
                enabled = state.phase == MeditationPhase.COMPLETED
            )
            OutlinedTextField(
                value = state.afterState,
                onValueChange = viewModel::updateAfterState,
                label = { Text("练习后的身心状态（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 4,
                enabled = state.phase == MeditationPhase.COMPLETED
            )
            if (state.phase == MeditationPhase.COMPLETED) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saveState.isSaving && state.elapsedSeconds > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.saveState.isSaving) "记录中…" else "记录这一刻")
                }
            }
            state.saveState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.saveState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun HistoryCard(
    session: MeditationSessionEntity,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = selected, onCheckedChange = onSelectionChange)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${session.date} · 第${session.practiceStep}步 · ${session.stepTitle}", style = MaterialTheme.typography.labelLarge)
                Text(formatDuration(session.durationSeconds), fontWeight = FontWeight.SemiBold)
            }
            if (session.observation.isNotBlank()) Text(session.observation, maxLines = 2)
            if (session.afterState.isNotBlank()) {
                Text("练习后：${session.afterState}", maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (session.observation.isBlank() && session.afterState.isBlank()) {
                Text("仅记录练习完成；未写当下感受", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
