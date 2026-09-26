package com.ruru.practice.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FirstRunScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("先把轨道铺稳", "实修日以四圣谛为方向，以戒、安般念、四念处和日常护根为主要训练线。第一次使用不需要学完全部理论。", "今天只要开始一座安般念。"),
        Triple("安般念先从前四步", "知道入息、出息；知道长短；觉知息身全过程；让身行渐渐止息。自然呼吸即可，不追求特殊体验。", "初学阶段先把前四步练熟。"),
        Triple("白天把修行带进生活", "遇到明显接触时，可以观察触 → 受 → 爱 → 取，再决定怎样回应。坐禅稳定后，也可以观察身、受、心、法的生灭。", "护根是日间修行的延伸。"),
        Triple("遇到问题就调整", "昏沉时可以加强念、择法、精进、喜，也可以起身经行；掉举时可加强轻安、定、舍。分析越来越多、直接经验越来越模糊时，回到自然呼吸。", "不追逐体验，也不把记录当作证果标准。")
    )
    val current = pages[page]
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Spacer(Modifier.height(24.dp))
                Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("第一次使用", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${page + 1} / ${pages.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(current.first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(current.second, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Text(current.third, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (page > 0) {
                    OutlinedButton(onClick = { page-- }, modifier = Modifier.weight(1f)) { Text("上一步") }
                }
                Button(
                    onClick = { if (page == pages.lastIndex) onFinish() else page++ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (page == pages.lastIndex) "进入今日修行" else "继续")
                    if (page != pages.lastIndex) Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
