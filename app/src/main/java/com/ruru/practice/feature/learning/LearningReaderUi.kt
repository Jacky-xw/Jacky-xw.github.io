package com.ruru.practice.feature.learning

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReaderSettingsPanel(vm: LearningSettingsViewModel, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("阅读设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SettingSlider("字体", vm.fontSize.toFloat(), 14f..26f, { vm.setFontSize(it.toInt()) }, "${vm.fontSize}sp")
            SettingSlider("行距", vm.lineHeight.toFloat(), 20f..42f, { vm.setLineHeight(it.toInt()) }, "${vm.lineHeight}sp")
            SettingSlider("页面宽度", vm.pageWidth.toFloat(), 70f..100f, { vm.setPageWidth(it.toInt()) }, "${vm.pageWidth}%")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("夜间阅读", Modifier.weight(1f)); Switch(vm.nightMode, vm::setNightMode)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("分页阅读", Modifier.weight(1f)); Switch(vm.pagedMode, vm::setPagedMode)
            }
            Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
                Text(
                    "觉知呼吸，观察身心变化。设置会即时预览并永久保存。",
                    modifier = Modifier.padding(14.dp).fillMaxWidth(vm.pageWidth / 100f),
                    fontSize = vm.fontSize.sp,
                    lineHeight = vm.lineHeight.sp
                )
            }
        }
    }
}

@Composable private fun SettingSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float)->Unit, valueText: String) {
    Column {
        Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text(valueText, color = MaterialTheme.colorScheme.primary) }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun MonthlyLearningInsights(prefs: SharedPreferences, modifier: Modifier = Modifier) {
    val revision = rememberPreferenceRevision(prefs)
    val calendar = remember { Calendar.getInstance() }
    val year = calendar.get(Calendar.YEAR); val month = calendar.get(Calendar.MONTH)
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val first = Calendar.getInstance().apply { set(year, month, 1) }
    val leading = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val counts = remember(prefs, year, month, revision) {
        (1..maxDay).associateWith { day ->
            val c = Calendar.getInstance().apply { set(year, month, day) }
            prefs.getInt("daily_${fmt.format(c.time)}", 0)
        }
    }
    val activeDays = counts.values.count { it > 0 }
    val total = counts.values.sum()
    val streak = remember(prefs, revision) { calculateLearningStreak(prefs) }
    val maxCount = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("本月学习", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("学习 $activeDays 天"); Text("连续 $streak 天"); Text("本月完成 $total 节")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { listOf("一","二","三","四","五","六","日").forEach { Text(it) } }
            val cells = List(leading) { null } + (1..maxDay).map { it }
            LazyVerticalGrid(columns = GridCells.Fixed(7), userScrollEnabled = false, modifier = Modifier.height((((cells.size + 6) / 7) * 38).dp)) {
                items(cells) { day ->
                    if (day == null) Spacer(Modifier.size(34.dp)) else {
                        val count = counts[day] ?: 0
                        val alpha = if (count == 0) 0.06f else 0.18f + 0.62f * count / maxCount
                        Box(Modifier.padding(2.dp).size(34.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) { Text(day.toString(), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            Text("详细趋势", fontWeight = FontWeight.Medium)
            Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                val values = (1..maxDay).map { counts[it] ?: 0 }
                if (values.size > 1) {
                    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
                    val step = size.width / (values.size - 1)
                    values.zipWithNext().forEachIndexed { i, pair ->
                        drawLine(
                            color = Color.Gray,
                            start = Offset(i * step, size.height - pair.first * size.height / max),
                            end = Offset((i + 1) * step, size.height - pair.second * size.height / max),
                            strokeWidth = 3f
                        )
                    }
                }
            }
        }
    }
}

fun calculateLearningStreak(prefs: SharedPreferences): Int {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    var count = 0
    while (prefs.getInt("daily_${fmt.format(cal.time)}", 0) > 0) { count++; cal.add(Calendar.DATE, -1) }
    return count
}
