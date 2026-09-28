package com.ruru.practice.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ReaderSettingsPanel(vm: LearningSettingsViewModel, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("阅读设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SettingSlider("字体", vm.fontSize.toFloat(), 14f..26f, { vm.updateFontSize(it.toInt()) }, "${vm.fontSize}sp")
            SettingSlider("页面宽度", vm.pageWidth.toFloat(), 70f..100f, { vm.updatePageWidth(it.toInt()) }, "${vm.pageWidth}%")
            SettingSlider(
                label = "行距",
                value = vm.lineSpacing / 100f,
                range = 1.2f..2.4f,
                steps = 11,
                onChange = { vm.updateLineSpacing((it * 100).roundToInt()) },
                valueText = "${vm.lineSpacing / 100f} 倍"
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("夜间阅读", Modifier.weight(1f)); Switch(vm.nightMode, vm::updateNightMode)
            }
            Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
                Text(
                    "觉知呼吸，观察身心变化。设置会即时预览并永久保存。",
                    modifier = Modifier.padding(14.dp).fillMaxWidth(vm.pageWidth / 100f),
                    fontSize = vm.fontSize.sp,
                    lineHeight = (vm.fontSize * vm.lineSpacing / 100f).sp
                )
            }
        }
    }
}

@Composable private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    valueText: String,
    steps: Int = 0
) {
    Column {
        Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text(valueText, color = MaterialTheme.colorScheme.primary) }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}
