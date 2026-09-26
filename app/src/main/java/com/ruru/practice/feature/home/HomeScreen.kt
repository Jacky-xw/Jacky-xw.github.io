package com.ruru.practice.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    viewModel: HomeDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 当用户从其他栏目返回首页时重新读取本地数据，
    // 避免练习完成后首页仍显示旧数据。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("实修日", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("让每一次觉知，都回到当下", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("今日方向", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("安般念", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("从呼吸开始，知道吸气，也知道呼气。", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Button(onClick = onStartPractice) {
                            Icon(Icons.Default.SelfImprovement, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("开始练习")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("今日修行反馈", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("记录只用于看清训练是否持续，不作为证果、境界或能力评分。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FeedbackRow("安般念", state.todayMeditationCount > 0, "${state.todayMeditationCount} 次")
                        FeedbackRow("经行", state.todayWalkingCount > 0, "${state.todayWalkingCount} 次")
                        FeedbackRow("护根", state.todayRootCount > 0, "${state.todayRootCount} 次")
                        FeedbackRow("观察", state.todayObservationCount > 0, "${state.todayObservationCount} 次")
                        FeedbackRow("五盖记录", state.todayHindranceCount > 0, "${state.todayHindranceCount} 次")
                        FeedbackRow("戒行复盘", state.todayPreceptCount > 0, "${state.todayPreceptCount} 次")
                        if (state.strongestHindrance != null) {
                            Text("近七日较常出现：${state.strongestHindrance}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.hindranceGuidance, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("今日修行顺序", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("按手册的工作日训练逻辑安排。这里看的是方向，不是打卡分数。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PlanRow(1, "安般念", state.todayMeditationCount > 0, "固定一座，以自然呼吸为基地；初学者先熟悉前四步。")
                        PlanRow(2, "白天护根", state.todayRootCount > 0, "遇到明显接触时看触 → 受 → 爱 → 取，给回应留下空间。")
                        PlanRow(3, "四念处观察", state.todayObservationCount > 0, "选择一个清楚的受、心或法，观察生起、变化、灭去。")
                        PlanRow(4, "经行", state.todayWalkingCount > 0, "昏沉或久坐后可经行；稳定时自然放慢，再回到安般。")
                        PlanRow(5, "戒行复盘", state.todayPreceptCount > 0, "睡前回看五戒与当天一个具体事件，再以无常与不放逸收束。")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("今天下一步", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(state.nextAction, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("近七日：安般 ${state.sevenDayMeditationCount} 次 · 经行 ${state.sevenDayWalkingCount} 次", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("练习次数", state.meditationCount.toString(), Modifier.weight(1f))
                    StatCard("累计分钟", state.totalMinutes.toString(), Modifier.weight(1f))
                }
                Text("练习次数统计安般念、经行与护根；八戒自检单独保留在练习页。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            item {
                Text("最近练习", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            if (state.isLoading) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 28.dp))
                }
            } else if (state.errorMessage != null) {
                item {
                    Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = viewModel::refresh) { Text("重试") }
                }
            } else if (state.recentPractices.isEmpty()) {
                item {
                    Text("还没有练习记录，今天就从 5 分钟开始。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.recentPractices, key = { it.key }) { practice ->
                    SessionSummaryCard(practice)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PlanRow(number: Int, title: String, completed: Boolean, guidance: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (completed) "✓" else number.toString(), fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(guidance, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeedbackRow(label: String, completed: Boolean, detail: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (completed) "✓ $label" else "○ $label")
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionSummaryCard(practice: com.ruru.practice.domain.usecase.RecentPracticeSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(practice.date, style = MaterialTheme.typography.labelLarge)
                practice.durationSeconds?.let { Text(formatDuration(it), fontWeight = FontWeight.SemiBold) }
            }
            Text(practice.title, fontWeight = FontWeight.SemiBold)
            if (practice.detail.isNotBlank()) Text(practice.detail, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes > 0) "${minutes} 分 ${rest.toString().padStart(2, '0')} 秒" else "$rest 秒"
}
