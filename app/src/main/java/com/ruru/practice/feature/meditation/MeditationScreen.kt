@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ruru.practice.feature.meditation

import com.ruru.practice.core.ui.CollapsibleNote

import com.ruru.practice.core.util.NotificationPermission
import com.ruru.practice.core.util.ExactAlarmPermission
import com.ruru.practice.core.util.BatteryOptimization

import android.provider.OpenableColumns
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import com.ruru.practice.core.ui.RetainedColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruru.practice.data.entity.MeditationSessionEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Form controls stay composed while scrolling; only the clock observes each second.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    viewModel: MeditationSessionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    // Phase alone — stable during a running session so the page shell
    // is not invalidated every second by elapsedSeconds.
    val phase by remember(viewModel) { viewModel.state.map { it.phase }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.phase)

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showReflectionSheet by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showBatteryOptDialog by remember { mutableStateOf(false) }
    var showCustomMinutes by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }
    var pendingStartMinutes by remember { mutableStateOf<Int?>(null) }
    val reflectionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    LaunchedEffect(phase) {
        if (phase == MeditationPhase.SAVED) showReflectionSheet = false
    }

    Surface(modifier = modifier.fillMaxSize()) {
        RetainedColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("安般念", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "以自然呼吸为所缘，依十六步逐渐展开身、受、心、法四念处。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "steps", contentType = "steps") {
                MeditationStepBlock(viewModel = viewModel, onCustomMinutes = { showCustomMinutes = true })
            }

            item(key = "timer", contentType = "timer") {
                MeditationTimerBlock(
                    viewModel = viewModel,
                    onRequestStart = { minutes ->
                        pendingStartMinutes = minutes
                        when {
                            !NotificationPermission.isGranted(context) ->
                                showNotificationPermissionDialog = true
                            !ExactAlarmPermission.canSchedule(context) ->
                                showExactAlarmDialog = true
                            !BatteryOptimization.isIgnoring(context) ->
                                showBatteryOptDialog = true
                            minutes >= 60 -> showBatteryDialog = true
                            minutes >= 30 -> showVolumeDialog = true
                            else -> viewModel.start()
                        }
                    }
                )
            }

            if (phase == MeditationPhase.COMPLETED || phase == MeditationPhase.SAVED) {
                item(key = "post", contentType = "post") {
                    MeditationPostSessionBlock(
                        viewModel = viewModel,
                        onRecord = { showReflectionSheet = true }
                    )
                }
            }

            item(key = "hist_header", contentType = "hist_header") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("练习历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            if (selectedIds.isNotEmpty()) {
                                TextButton(onClick = { confirmDelete = true }) {
                                    Text("删除(${selectedIds.size})")
                                }
                            }
                        }
                        Text(
                            "仅保留本周记录，周一自动清除；也可勾选一条或多条删除。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (history.isEmpty()) {
                            Text("暂无练习记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(
                items = history,
                key = { it.id },
                contentType = { "hist" }
            ) { session ->
                HistoryCard(
                    session = session,
                    selected = session.id in selectedIds,
                    onSelectionChange = { checked ->
                        selectedIds = if (checked) selectedIds + session.id else selectedIds - session.id
                    }
                )
            }
        }

        if (showReflectionSheet && phase == MeditationPhase.COMPLETED) {
            MeditationReflectionSheet(
                viewModel = viewModel,
                sheetState = reflectionSheetState,
                onDismiss = { showReflectionSheet = false }
            )
        }

        
        // Mandatory long-seat end alarm — cannot be dismissed without acknowledging.
        val endAlarmActive by remember(viewModel) { viewModel.state.map { it.endAlarmActive }.distinctUntilChanged() }
            .collectAsStateWithLifecycle(initialValue = viewModel.state.value.endAlarmActive)
        if (endAlarmActive) {
            AlertDialog(
                onDismissRequest = { /* must confirm */ },
                title = { Text("本座已到时") },
                text = {
                    Text("长时安般念已结束。引磬与震动将持续提醒，请确认自己已安全出定后再停止。")
                },
                confirmButton = {
                    Button(onClick = { viewModel.acknowledgeEndAlarm() }) { Text("已出定，停止提醒") }
                }
            )
        }

        if (showCustomMinutes) {
            AlertDialog(
                onDismissRequest = { showCustomMinutes = false },
                title = { Text("自定义时长（分钟）") },
                text = {
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { v -> customMinutesText = v.filter { it.isDigit() }.take(3) },
                        label = { Text("1–360 分钟") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val m = customMinutesText.toIntOrNull()?.coerceIn(1, 360)
                        if (m != null) {
                            viewModel.selectMinutes(m)
                            showCustomMinutes = false
                            customMinutesText = ""
                        }
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomMinutes = false }) { Text("取消") }
                }
            )
        }


        
        if (showNotificationPermissionDialog) {
            AlertDialog(
                onDismissRequest = { /* 强制授权：不允许点外部关闭后开始计时 */ },
                title = { Text("必须开启通知权限") },
                text = {
                    Text(
                        "未开启通知权限时，无法启动修习计时。\n\n" +
                            "后台保活与到时提示依赖通知权限。请点击下方按钮前往开启，返回后点「我已开启」。"
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            runCatching {
                                (context as? android.app.Activity)?.requestPermissions(
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    1001
                                )
                            }
                        }
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
                    Button(onClick = {
                        if (NotificationPermission.isGranted(context)) {
                            showNotificationPermissionDialog = false
                            val minutes = pendingStartMinutes ?: return@Button
                            when {
                                !ExactAlarmPermission.canSchedule(context) ->
                                    showExactAlarmDialog = true
                                !BatteryOptimization.isIgnoring(context) ->
                                    showBatteryOptDialog = true
                                minutes >= 60 -> showBatteryDialog = true
                                minutes >= 30 -> showVolumeDialog = true
                                else -> viewModel.start()
                            }
                        }
                    }) { Text("我已开启") }
                }
            )
        }

        if (showExactAlarmDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("必须开启闹钟与提醒") },
                text = {
                    Text(
                        "锁屏与后台准时出定依赖系统「闹钟与提醒」权限。\n\n" +
                            "未开启时无法保证到点响铃。请前往开启后，返回点「我已开启」。"
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        runCatching {
                            context.startActivity(ExactAlarmPermission.settingsIntent(context))
                        }
                    }) { Text("去开启闹钟权限") }
                },
                dismissButton = {
                    Button(onClick = {
                        if (ExactAlarmPermission.canSchedule(context)) {
                            showExactAlarmDialog = false
                            val minutes = pendingStartMinutes ?: return@Button
                            when {
                                !BatteryOptimization.isIgnoring(context) ->
                                    showBatteryOptDialog = true
                                minutes >= 60 -> showBatteryDialog = true
                                minutes >= 30 -> showVolumeDialog = true
                                else -> viewModel.start()
                            }
                        }
                    }) { Text("我已开启") }
                }
            )
        }

        if (showBatteryOptDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("必须关闭电池限制") },
                text = {
                    Text(
                        "为在锁屏与后台准时提醒，必须允许本应用「不受电池优化限制」。\n\n" +
                            "这是成熟闹钟类应用的必要设置。请前往开启后，返回点「我已开启」。"
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        runCatching {
                            context.startActivity(BatteryOptimization.requestIgnoreIntent(context))
                        }.onFailure {
                            runCatching {
                                context.startActivity(BatteryOptimization.appBatterySettingsIntent(context))
                            }
                        }
                    }) { Text("去关闭电池限制") }
                },
                dismissButton = {
                    Button(onClick = {
                        if (BatteryOptimization.isIgnoring(context)) {
                            showBatteryOptDialog = false
                            val minutes = pendingStartMinutes ?: return@Button
                            when {
                                minutes >= 60 -> showBatteryDialog = true
                                minutes >= 30 -> showVolumeDialog = true
                                else -> viewModel.start()
                            }
                        }
                    }) { Text("我已开启") }
                }
            )
        }

if (showBatteryDialog) {
            val battery = viewModel.batteryPercent()
            val volumePercent = viewModel.mediaVolumePercent()
            val lowVol = viewModel.isMediaMutedOrVeryLow()
            AlertDialog(
                onDismissRequest = { showBatteryDialog = false },
                title = { Text("长时修习开始前确认") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            buildString {
                            append("本座时长 ≥ 60 分钟。\n\n")
                            append("闹钟音量约 ${volumePercent}%")
                            if (lowVol) append("（几乎静音，请先调高闹钟音量）")
                            append("。\n")
                            if (battery >= 0) append("当前电量约 ${battery}%。")
                            else append("未能读取电量，请自行确认电量充足。")
                            append("\n\n到时后将循环播放引磬并震动，直到你打开本页点击确认。\n")
                            append("建议允许忽略电池优化，以免系统在后台结束计时。")
                        }
                        )
                        Text(
                            "考虑到您的安全，建议与系统闹钟一起使用，并建议您的闹钟提示音使用不刺激的引磬声音",
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        // 长时座再次强制检测三项权限
                        when {
                            !NotificationPermission.isGranted(context) -> {
                                showBatteryDialog = false
                                showNotificationPermissionDialog = true
                            }
                            !ExactAlarmPermission.canSchedule(context) -> {
                                showBatteryDialog = false
                                showExactAlarmDialog = true
                            }
                            !BatteryOptimization.isIgnoring(context) -> {
                                showBatteryDialog = false
                                showBatteryOptDialog = true
                            }
                            else -> {
                                showBatteryDialog = false
                                viewModel.start()
                            }
                        }
                    }) { Text("已确认，开始计时") }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryDialog = false }) { Text("返回") }
                }
            )
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
                            "当前闹钟音量约 ${volumePercent}%，几乎静音。座时 30 分钟以上结束时的引磬可能听不到，请先调高系统媒体音量后再开始。"
                        } else {
                            "当前闹钟音量约 ${volumePercent}%。座时 30 分钟以上结束时会播放引磬；若音量过低可能听不到，确认无误后再开始。"
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
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
private fun MeditationStepBlock(
    viewModel: MeditationSessionViewModel,
    onCustomMinutes: () -> Unit
) {
    val practiceStep by remember(viewModel) { viewModel.state.map { it.practiceStep }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.practiceStep)
    val phase by remember(viewModel) { viewModel.state.map { it.phase }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.phase)
    val selectedMinutes by remember(viewModel) { viewModel.state.map { it.selectedMinutes }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.selectedMinutes)

    val stepsEnabled = phase == MeditationPhase.READY ||
        phase == MeditationPhase.COMPLETED ||
        phase == MeditationPhase.SAVED
    val currentStep = remember(practiceStep) {
        AnapanStepCatalog.steps.first { it.number == practiceStep }
    }
    val stepScroll = rememberScrollState()
    val durationScroll = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("安般念十六步", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "先练熟前四步，再逐渐进入受、心、法三组。十六步是完整修习过程，不机械等同于四禅。",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(stepScroll)
        ) {
            AnapanStepCatalog.steps.forEach { step ->
                AssistChip(
                    onClick = { viewModel.selectStep(step.number) },
                    label = { Text("${step.number}") },
                    enabled = stepsEnabled
                )
            }
        }
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
            modifier = Modifier.padding(top = 4.dp).horizontalScroll(durationScroll)
        ) {
            val presets = listOf(1, 15, 30, 45, 60, 90, 120, 180)
            presets.forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { viewModel.selectMinutes(minutes) },
                    enabled = stepsEnabled,
                    label = { Text("${minutes}分钟") }
                )
            }
            FilterChip(
                selected = selectedMinutes !in presets,
                onClick = onCustomMinutes,
                enabled = stepsEnabled,
                label = {
                    Text(
                        if (selectedMinutes !in presets) "自定义 ${selectedMinutes}分"
                        else "自定义"
                    )
                }
            )
        }
    }
}

@Composable
private fun MeditationTimerBlock(
    viewModel: MeditationSessionViewModel,
    onRequestStart: (selectedMinutes: Int) -> Unit
) {
    val controls = remember(viewModel) {
        viewModel.state.map { it.copy(elapsedSeconds = 0, remainingSeconds = 0) }.distinctUntilChanged()
    }
    val state by controls.collectAsStateWithLifecycle(initialValue = viewModel.state.value)
    val context = LocalContext.current
    var bellName by remember { mutableStateOf(viewModel.bellSoundName()) }
    var bellError by remember { mutableStateOf<String?>(null) }
    var endVibrate by remember { mutableStateOf(viewModel.endVibrationEnabled) }
    val bellPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = runCatching {
                context.contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull()
            viewModel.setBellSound(it, name).fold(
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
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeditationClock(viewModel)
            Text(
                "第${state.practiceStep}步 · ${AnapanStepCatalog.title(state.practiceStep)}",
                fontWeight = FontWeight.SemiBold
            )
            Text(phaseLabel(state.phase), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimerActions(
                state = state,
                viewModel = viewModel,
                onStartRequest = { onRequestStart(state.selectedMinutes) }
            )
            Text(
                "计时与当前页面无关：切换页面或暂时离开后仍会继续；进程被系统回收后也会按已练习时长恢复。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "自然结束时播放三次引磬；暂停、手动完成和离开页面不会误响。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "提示音：$bellName（默认 432Hz 禅寺磬音）",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("结束时震动提醒", fontWeight = FontWeight.Medium)
                    Text(
                        if (state.selectedMinutes >= 60)
                            "一小时及以上：到时强制循环引磬+震动，需确认出定后才停止"
                        else
                            "嗡～静～嗡～静～嗡，与短信等短震不同",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
            if (state.selectedMinutes >= 60) {
                Text(
                    "考虑到您的安全，建议与系统闹钟一起使用，并建议您的闹钟提示音使用不刺激的引磬声音",
                    color = Color(0xFFC62828),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
                }
                Switch(
                    checked = if (state.selectedMinutes >= 60) true else endVibrate,
                    onCheckedChange = { checked ->
                        if (state.selectedMinutes < 60) {
                            endVibrate = checked
                            viewModel.endVibrationEnabled = checked
                        }
                    },
                    enabled = state.selectedMinutes < 60
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            TimerDiagnosticsButton()
        }
    }
}

@Composable
private fun MeditationPostSessionBlock(
    viewModel: MeditationSessionViewModel,
    onRecord: () -> Unit
) {
    val phase by remember(viewModel) { viewModel.state.map { it.phase }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.phase)
    val saveState by remember(viewModel) { viewModel.state.map { it.saveState }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = viewModel.state.value.saveState)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("本次练习已自动记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "次数和时长已经计入；写下这一刻只补充心得，不会重复计数。未写笔记时历史里暂不显示空白卡片，下次进入或刷新后仍会保留本次练习。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (phase == MeditationPhase.COMPLETED) {
                Button(
                    onClick = onRecord,
                    enabled = !saveState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (saveState.isSaving) "正在记入练习…" else "记录这一刻") }
                saveState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } else {
                Text("这一刻已记录", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeditationReflectionSheet(
    viewModel: MeditationSessionViewModel,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("记录这一刻", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "这只是补充本次练习的观察，不会再次增加练习次数。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.observation,
                onValueChange = viewModel::updateObservation,
                label = { Text("本步练习中的观察（可选）") },
                placeholder = { Text("例如：知道散乱后回到自然呼吸") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 6
            )
            OutlinedTextField(
                value = state.afterState,
                onValueChange = viewModel::updateAfterState,
                label = { Text("练习后的身心状态（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 6
            )
            state.saveState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saveState.isSaving,
                    modifier = Modifier.weight(1f)
                ) { Text(if (state.saveState.isSaving) "记录中…" else "保存") }
            }
            Spacer(Modifier.height(8.dp))
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
                    CollapsibleNote(label = "本步练习中的观察", value = observation, collapsedMaxLines = 3)
                }
                if (afterState.isNotBlank()) {
                    CollapsibleNote(label = "练习后的身心状态", value = afterState, secondary = true, collapsedMaxLines = 3)
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
    MeditationPhase.RUNNING -> "进行中"
    MeditationPhase.PAUSED -> "已暂停"
    MeditationPhase.COMPLETED -> "本座已完成"
    MeditationPhase.SAVED -> "本座已记录"
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m} 分 ${s.toString().padStart(2, '0')} 秒"
}

@Composable
private fun MeditationClock(viewModel: MeditationSessionViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val totalSeconds = state.selectedMinutes * 60
    val progress = if (totalSeconds == 0) 0f else state.elapsedSeconds.toFloat() / totalSeconds

            Text(timerLabel(state), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            // Lightweight bar: Material LinearProgressIndicator is heavier under frequent updates.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

}
