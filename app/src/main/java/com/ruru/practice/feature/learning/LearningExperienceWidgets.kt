package com.ruru.practice.feature.learning
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * V1.26.2 学习中心体验组件
 * - Compose Canvas 趋势折线图
 * - Compose Grid 月历热力图
 * - Section 自动索引生成
 * - 阅读设置 UI 状态入口
 */

data class LearningDay(
    val day: Int,
    val count: Int
)

data class LearningTrendPoint(
    val day: Int,
    val value: Int
)

fun generateSectionIndex(article: LearningArticle): Map<String, Int> =
    article.sections.associate { it.id to it.order }

@Composable
fun LearningCalendarHeatmap(
    days: List<LearningDay>,
    modifier: Modifier = Modifier
) {
    val max = (days.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("学习月历")
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(220.dp)
            ) {
                items(days) { item ->
                    val level = item.count.toFloat() / max
                    Card(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(32.dp)
                    ) {
                        Text(
                            "${item.day}",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LearningTrendChart(
    points: List<LearningTrendPoint>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("月度学习趋势")
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (points.size > 1) {
                    val max = points.maxOf { it.value }.coerceAtLeast(1)
                    val step = size.width / (points.size - 1)

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color.Black,
                            start = Offset(
                                i * step,
                                size.height - points[i].value * size.height / max
                            ),
                            end = Offset(
                                (i + 1) * step,
                                size.height - points[i + 1].value * size.height / max
                            ),
                            strokeWidth = 3f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LearningSettingsPanel(
    fontSize: Int,
    lineHeight: Int,
    pageWidth: Int,
    nightMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("阅读设置")
            Text("字体大小: $fontSize")
            Text("行距: $lineHeight")
            Text("页面宽度: $pageWidth")
            Text("夜间模式: ${if (nightMode) "开启" else "关闭"}")
        }
    }
}
