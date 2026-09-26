package com.ruru.practice.feature.learning

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LearningScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("修行主线", "调节工具", "四周起步", "长期检验", "安全边界")
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("学习中心", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("把教法放回经验中理解，再用修习检验。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(title) })
                    }
                }
            }
            when (tab) {
                0 -> mainPath()
                1 -> adjustmentTools()
                2 -> starterMonth()
                3 -> longTerm()
                else -> safety()
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.mainPath() {
    item { LessonCard("四圣谛", "从真实的苦入手：知苦、知集、知灭、修道。所有观察最终都应回到苦的集起与止息，而不是只积累概念。") }
    item { LessonCard("八正道", "正见、正思惟、正语、正业、正命、正精进、正念、正定。戒、定、慧互相支持，不设置机械的‘全部完成才能进入下一项’门槛。") }
    item { LessonCard("安般念十六步", "以自然呼吸为基地。前四步建立身念处与稳定；中间两组逐渐把受与心纳入；后四步观察无常、断、无欲、灭。十六步不机械等同于四禅。") }
    item { LessonCard("四念处", "身、受、心、法都做四件事：知道当前对象，知道生起，知道变化与灭去，不因喜欢而取、不因不喜欢而排斥。坐禅稳定后再把明显受、心、烦恼纳入观察。") }
    item { LessonCard("缘起与五蕴", "日常最容易直接看到触→受→爱→取；条件成熟时再观察更完整的缘起关系。五蕴用于检查‘我、我的、我的自体’，不把理论分析变成脑内讲课。") }
    item { LessonCard("入流方向", "入流的核心是见法与三结的断除：身见、疑、戒禁取。累计坐禅时长或特殊体验本身不是判定标准。") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.adjustmentTools() {
    item { LessonCard("七觉支：调节心的工具", "念贯穿全程；择法辨别当下条件；精进推动善法；喜使心明朗；轻安使身心柔和；定使心统一；舍使心不偏取。") }
    item { LessonCard("心昏沉时", "可加强念、择法、精进、喜；起身经行也是手册明确建议的办法。不要靠硬撑维持坐姿。") }
    item { LessonCard("心掉举时", "可加强轻安、定、舍，减少继续追逐刺激。仍以当前所缘为基地，直到心重新稳定。") }
    item { LessonCard("五根与五力", "信、精进、念、定、慧称为五根；成熟有力时称五力。信给方向，精进给动力，念让训练不断线，定让心稳定，慧让心如实见法。") }
    item { LessonCard("护根", "接触→受→爱→取→回应。重点是更早看见受与爱之间的倾向，给回应留下空间；护根是日间修行的延伸。") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.starterMonth() {
    item { LessonCard("第一周：先把轨道铺稳", "每天固定时段坐二十分钟；睡前检查五戒；每天观察一次明显乐受或苦受；减少一个最大的信息刺激入口；听闻一次以四圣谛为中心的正法。") }
    item { LessonCard("第二周：认识五盖", "坐禅提高到二十至三十分钟；学习辨认五盖；白天开始做触—受—爱检查；重点守护一个最常犯的正语问题；做一次小额布施并观察悭吝与舍。") }
    item { LessonCard("第三周：开始看生灭", "保持前两周内容；坐禅稳定后观察一个清楚现象的生灭；每天一次把强烈‘我’的事件放回五蕴检查；安排一到三小时安静独处，期间不娱乐。") }
    item { LessonCard("第四周：完整复盘", "检查戒是否更稳、刺激是否下降、瞋后恢复是否更快、呼吸是否更容易持续、能否分清受与爱、是否开始理解五蕴并非固定自我。根据结果调整，不单纯增加坐禅时长。") }
    item { LessonCard("重要提醒", "四周课表是建立轨道的训练安排，不是四周证入流计划。果位不能由日历保证。") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.longTerm() {
    item { LessonCard("标准长期训练", "每天保持一到两座安般；白天持续护根；每周固定闻法与复盘；每周至少一段较长安静时段；定期八戒或简化日；持续布施；定期向可靠善知识校正。") }
    item { LessonCard("三个月大复盘", "不只看累计坐了多少小时。检查五戒故犯、谎言、情色依赖、酒醉、购物冲动、网络瞋、恢复正念速度、五盖强度、定的可重复性、四念处清晰度，以及触、受、爱、取和自我见的可见度。") }
    item { LessonCard("方向性判据", "若生活与心行长期朝善、离欲、清明、少执著的方向变化，继续稳定；若长期相反，回头检查老师、方法、生活方式与自己的动机。") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.safety() {
    item { LessonCard("不追求特殊体验", "光明、震动、喜悦、身体变化或其他特殊现象出现时，只需知道它们。不要据此认定自己证果，也不要为了重现体验而加力。") }
    item { LessonCard("异常情况先停下来", "若练习持续造成明显失眠、强烈恐惧、现实感明显异常、工作与家庭责任持续受损，应降低强度并寻求可靠的现实支持或专业帮助。") }
    item { LessonCard("回到安般", "观缘起、五蕴或无我时若概念越来越多、直接经验越来越模糊，就停止分析，回到自然呼吸，让心重新稳定。") }
    item { LessonCard("居士责任", "出离首先是心从欲染中退开，不等于突然抛弃父母、子女、债务、工作或照护责任。修行应当与诚实、守戒、正命和基本生活稳定相协调。") }
}

@Composable
private fun LessonCard(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(content, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
