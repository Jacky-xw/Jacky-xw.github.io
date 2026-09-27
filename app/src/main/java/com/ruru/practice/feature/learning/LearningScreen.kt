package com.ruru.practice.feature.learning

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ruru.practice.R
import com.ruru.practice.feature.navigation.BottomNavItem
import kotlin.math.roundToInt

private val LocalLearningSettingsVm = compositionLocalOf<LearningSettingsViewModel> { error("LearningSettingsViewModel not provided") }

private val learningTabs = listOf(
    "古阴符经-华夏复兴必看", "菩萨道", "修行主线", "在家八正道", "安般念", "五盖调节",
    "经行", "护根", "五蕴观察", "缘起观察", "长期检验", "术语", "安全边界"
)

private const val LEARNING_TABS_VERSION = 2

private data class ReaderScrollPosition(val index: Int, val offset: Int)

private data class DirectoryTarget(val title: String, val itemIndex: Int)

private fun directoryTargets(tab: Int, anapanasatiSection: Int): List<DirectoryTarget> {
    val titles = when (tab) {
        0 -> listOf("古阴符经-华夏复兴必看", "《古阴符经》的真相·上篇", "《古阴符经》的真相·下篇")
        1 -> listOf("菩萨道", "菩萨道概括", "佛像")
        2 -> listOf("第一部分 · 先把佛法听明白", "四圣谛", "佛、法、僧与归依", "信、业与轮回", "业果、后有与当下观察", "涅槃与善不善", "中道与善法欲", "戒、定、慧", "八正道", "入流四分", "五蕴", "集、味、患、离", "身见与二十种身见", "六入、触、受、爱、取", "无常、苦、无我", "入流方向", "入流以后")
        3 -> listOf("在家八正道", "正见", "正思惟", "正语", "正业", "正命 · 先守住谋生边界", "正精进", "正念", "正定", "生活条件")
        4 -> {
            val inner = when (anapanasatiSection) {
                0 -> listOf("零基础 · 先学会怎么坐", "环境", "坐姿", "眼睛与身体", "呼吸放在哪里", "一开始只做这一件事", "需要数息怎么办", "一次练多久", "修习前 · 五项助缘", "1 · 戒与威仪", "2 · 少欲、少事、少务", "3 · 饮食知量", "4 · 睡眠与精勤", "5 · 清静处", "先认识五盖", "从日常到坐禅")
                1 -> listOf("十六步 · 身念处", "第 1 步 · 知入息出息", "第 2 步 · 知长短", "第 3 步 · 觉知一切身", "第 4 步 · 身行止息", "十六步 · 受念处", "第 5 步 · 觉知喜", "第 6 步 · 觉知乐", "第 7 步 · 觉知心行", "第 8 步 · 令心行止息", "十六步 · 心念处", "第 9 步 · 觉知心", "第 10 步 · 令心欣悦", "第 11 步 · 令心安定", "第 12 步 · 令心解脱", "十六步 · 法念处", "第 13 步 · 观察无常", "第 14 步 · 观察断", "第 15 步 · 观察无欲", "第 16 步 · 观察灭", "十六步与四念处", "十六步怎么实际推进", "前四步练到什么程度再往后", "喜与乐不是必须制造", "心行怎么理解", "令心欣悦、安定、解脱怎么做", "最后四步不是思考哲学", "什么时候退回前一步")
                2 -> listOf("道品关系 · 从安般念到三十七菩提分法", "三十七菩提分法 · 七组共三十七支", "经中明确的安般念次第", "四念处与七觉分", "七觉分不是七个勋章", "安那般那 → 四念处 → 七觉分 → 明与解脱", "与四正勤相应", "与五根、五力相应", "与四如意足相应", "与八正道相应", "安般念与四禅")
                3 -> listOf("经教层次 · 为什么修与如何定位", "佛陀为什么反复教安那般那", "阿梨瑟咤经教 · 从不染著到更胜妙", "学住/如来住 · 同修安般而断惑深浅不同", "安般念的直接作用", "更胜妙的安般念", "安般念与定 · 从明显到微细", "安般念与禅定及圣果", "诸行渐次止息", "身行、口行、意行", "熟练后的稳定性")
                else -> listOf("呼吸越来越细时", "找不到呼吸时怎么办", "妄念很多时怎么办", "昏沉时怎么办", "紧张、焦虑时怎么办", "疼痛时怎么办", "出现喜、光、震动、身体消失感", "修习成果不要只看坐上", "完整次第", "零基础的一次完整练习")
            }
            listOf("第二部分 · 安那般那念") + inner
        }
        5 -> listOf("五盖现前时", "贪欲", "瞋恚", "昏沉睡眠", "掉举后悔", "疑", "七觉分调节", "五根与五力", "护根")
        6 -> listOf("经行", "准备", "基本方法", "所缘选择", "标记与步速", "停与转", "结束与坐禅衔接", "困难调整", "观察无常", "常见偏差", "精要")
        7 -> listOf("护根 · 白天的延伸", "从六根接触开始", "辨认三种受", "受之后看爱", "爱之后看取", "手机、网络与娱乐", "冲突中的护根", "与坐禅衔接")
        8 -> listOf("五蕴观察", "色", "受", "想", "行", "识", "色与四大", "集、味、患、离", "强烈我感时怎么观", "二十种身见")
        9 -> listOf("缘起观察", "触", "受", "爱", "取", "有与后续行为", "完整缘起的方向", "概念太多就返回呼吸")
        10 -> listOf("长期修习", "标准长期训练", "先看行为", "再看心", "再看智慧", "三个月大复盘", "方向性判据", "选择善知识 · 边界先于权威")
        11 -> listOf("常见术语", "四圣谛", "佛、法、僧与归依", "信", "阿含、尼柯耶", "业、果报与轮回", "后有", "涅槃", "善、不善与善法欲", "中道", "戒、定、慧", "八正道", "五戒与正命", "五盖", "四念处", "七觉支", "五根与五力", "五蕴 / 五取蕴", "六入、触", "受、爱、取", "缘起", "无常、苦、无我", "正念、正知、定、慧", "厌离与离欲", "三结与四不坏净", "集、味、患、离", "居士与优婆塞", "优婆塞六具足", "优婆塞事", "法行与正行", "预流 / 入流", "一来、不还、阿罗汉", "入流四分")
        12 -> listOf("安全边界", "特殊身心现象", "不强迫呼吸", "异常情况先降低强度", "身体健康优先", "居士责任", "概念过多时回到安般", "最后的主线")
        else -> emptyList()
    }
    return when (tab) {
        0 -> listOf(
            DirectoryTarget("古阴符经-华夏复兴必看", 3),
            DirectoryTarget("《古阴符经》的真相·上篇", 3),
            DirectoryTarget("《古阴符经》的真相·下篇", 4)
        )
        4 -> {
            val inner = titles.drop(1)
            listOf(DirectoryTarget(titles.first(), 3)) + inner.mapIndexed { index, title -> DirectoryTarget(title, 5 + index) }
        }
        else -> titles.mapIndexed { index, title -> DirectoryTarget(title, 3 + index) }
    }
}

@Composable
fun LearningScreen(
    currentTopLevelRoute: String? = null,
    onNavigateTopLevel: ((BottomNavItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember(context) { learningPrefs(context) }
    remember(prefs) { migrateLearningTabState(prefs) }
    val tabs = learningTabs
    var tab by rememberSaveable(LEARNING_TABS_VERSION) {
        mutableIntStateOf(
            prefs.getInt("reader_tab", prefs.getInt("last_tab", 0)).coerceIn(0, tabs.lastIndex)
        )
    }
    var anapanasatiSection by rememberSaveable {
        mutableIntStateOf(prefs.getInt("anapanasati_section", 0).coerceIn(0, 4))
    }
    var pendingAnapanasatiJump by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val settingsVm: LearningSettingsViewModel = viewModel()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val lifecycleOwner = LocalLifecycleOwner.current
    val directoryTargets = remember(tab, anapanasatiSection) { directoryTargets(tab, anapanasatiSection) }
    val currentDirectoryIndex by remember(directoryTargets) {
        derivedStateOf {
            directoryTargets.indexOfLast { it.itemIndex <= listState.firstVisibleItemIndex }.coerceAtLeast(0)
        }
    }
    val readingProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 4) 0f else {
                (listState.firstVisibleItemIndex.toFloat() / (totalItems - 1).toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    val scrollPositions = remember { mutableMapOf<Int, ReaderScrollPosition>() }
    val pendingScrollPositions = remember { mutableMapOf<Int, ReaderScrollPosition>() }
    val learningTextCache = remember { mutableMapOf<Int, String>() }
    val bodhisattvaTexts = remember {
        listOf(
            R.raw.bodhisattva_overview,
            R.raw.buddha_image
        )
    }
    val ancientYinfuTexts = remember {
        listOf(R.raw.guyinfujing_upper, R.raw.guyinfujing_lower)
    }

    fun currentScrollPosition(): ReaderScrollPosition = ReaderScrollPosition(
        index = listState.firstVisibleItemIndex,
        offset = listState.firstVisibleItemScrollOffset
    )

    fun selectTab(nextTab: Int) {
        val normalized = nextTab.coerceIn(0, tabs.lastIndex)
        if (normalized == tab) return

        val current = currentScrollPosition()
        scrollPositions[tab] = current
        saveScrollPosition(prefs, tab, current)

        // A tab without its own saved position inherits the current viewport. This
        // prevents a category click from unexpectedly jumping to item zero.
        pendingScrollPositions[normalized] =
            scrollPositions[normalized] ?: readScrollPosition(prefs, normalized, current)
        pendingAnapanasatiJump = false
        tab = normalized
    }

    fun resumeLastStudy() {
        val lastTab = prefs.getInt("reader_tab", tab).coerceIn(0, tabs.lastIndex)
        if (lastTab != tab) {
            selectTab(lastTab)
        } else {
            val position = scrollPositions[lastTab] ?: readScrollPosition(prefs, lastTab)
            scope.launch {
                val maxIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                runCatching { listState.animateScrollToItem(position.index.coerceIn(0, maxIndex), position.offset) }
            }
        }
    }

    LaunchedEffect(tab) {
        val target = pendingScrollPositions.remove(tab)
            ?: scrollPositions[tab]
            ?: readScrollPosition(prefs, tab)
        scrollPositions[tab] = target
        // Wait for the selected tab's items to be measured before restoring its
        // viewport. The previous implementation compared one global item key and
        // reset to zero whenever the category changed.
        withFrameNanos { }
        runCatching {
            val maxIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.scrollToItem(target.index.coerceIn(0, maxIndex), target.offset.coerceAtLeast(0))
        }
    }

    LaunchedEffect(anapanasatiSection) {
        if (pendingAnapanasatiJump && tab == 4) {
            withFrameNanos { }
            runCatching { listState.animateScrollToItem(5) }
            pendingAnapanasatiJump = false
        }
    }

    DisposableEffect(lifecycleOwner, prefs, tab, listState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                saveScrollPosition(prefs, tab, currentScrollPosition())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(listState, tab) {
        snapshotFlow {
            ReaderScrollPosition(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }.distinctUntilChanged().collectLatest { position ->
            delay(250)
            scrollPositions[tab] = position
            saveScrollPosition(prefs, tab, position)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onNavigateTopLevel != null) {
                        item { Text("主导航", fontWeight = FontWeight.Bold) }
                        BottomNavItem.items.forEach { navItem ->
                            item(key = "top-level-${navItem.route}") {
                                Text(
                                    text = navItem.title,
                                    fontWeight = if (currentTopLevelRoute == navItem.route) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTopLevelRoute == navItem.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                drawerState.close()
                                                onNavigateTopLevel(navItem)
                                            }
                                        }
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                    }
                    item { Text("章节目录", fontWeight = FontWeight.Bold) }
                    tabs.forEachIndexed { index, title ->
                        item(key = "directory-tab-$index") {
                            Text(
                                title,
                                fontWeight = if (index == tab) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        if (index == tab) {
                                            listState.animateScrollToItem(0)
                                        } else {
                                            selectTab(index)
                                        }
                                        drawerState.close()
                                    }
                                }
                            )
                        }
                        if (index == tab) {
                            directoryTargets.forEachIndexed { targetIndex, target ->
                                item(key = "directory-target-$index-${target.itemIndex}") {
                                    Text(
                                        text = "  ${target.title}",
                                        fontWeight = if (targetIndex == currentDirectoryIndex) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (targetIndex == currentDirectoryIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    listState.animateScrollToItem(target.itemIndex)
                                                    drawerState.close()
                                                }
                                            }
                                            .padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        MaterialTheme(colorScheme = if (settingsVm.nightMode) darkColorScheme() else MaterialTheme.colorScheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalLearningSettingsVm provides settingsVm) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            Text("学习中心", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { scope.launch { drawerState.open() } }) { Text("目录") }
                                TextButton(onClick = { showSettings = !showSettings }) { Text("设置") }
                            }
                            if (showSettings) ReaderSettingsPanel(settingsVm)
                            Text(
                                "在原有学习内容上补充《在家居士入流修行手册》的理论地图与实修次第。先明白方向，再把教法放回经验中检验。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = readingProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "当前分类阅读进度：${(readingProgress * 100).roundToInt()}% · ${directoryTargets.getOrNull(currentDirectoryIndex)?.title.orEmpty()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        item { LearningResumeCard(prefs, tabs, tab, ::resumeLastStudy) }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    FilterChip(selected = tab == index, onClick = { selectTab(index) }, label = { Text(title) })
                                }
                            }
                        }
                        when (tab) {
                            0 -> ancientYinfuPath(getAncientYinfuTexts())
                            1 -> bodhisattvaPath(getBodhisattvaTexts())
                            2 -> mainPath()
                            3 -> householdEightfoldPath()
                            4 -> anapanasati(
                                selectedSection = anapanasatiSection,
                                onSectionSelected = { section ->
                                    val normalizedSection = section.coerceIn(0, 4)
                                    prefs.edit().putInt("anapanasati_section", normalizedSection).apply()
                                    if (normalizedSection == anapanasatiSection) {
                                        scope.launch { runCatching { listState.animateScrollToItem(5) } }
                                    } else {
                                        anapanasatiSection = normalizedSection
                                        pendingAnapanasatiJump = true
                                    }
                                }
                            )
                            5 -> hindrances()
                            6 -> walkingMeditation()
                            7 -> senseRestraint()
                            8 -> aggregatesPractice()
                            9 -> dependentOriginationPractice()
                            10 -> longTerm()
                            11 -> glossary()
                            12 -> safety()
                            else -> safety()
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

private fun learningPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

private fun migrateLearningTabState(prefs: SharedPreferences) {
    if (prefs.getInt("learning_tabs_version", 0) >= LEARNING_TABS_VERSION) return
    val editor = prefs.edit()
    prefs.all.keys
        .filter { key ->
            key == "reader_tab" ||
                key == "last_tab" ||
                key == "reader_item_index" ||
                key == "reader_item_offset" ||
                key.startsWith("reader_item_index_") ||
                key.startsWith("reader_item_offset_")
        }
        .forEach(editor::remove)
    editor.putInt("learning_tabs_version", LEARNING_TABS_VERSION).apply()
}

private fun readScrollPosition(
    prefs: SharedPreferences,
    tab: Int,
    fallback: ReaderScrollPosition? = null
): ReaderScrollPosition {
    val indexKey = "reader_item_index_$tab"
    return if (prefs.contains(indexKey)) {
        ReaderScrollPosition(
            index = prefs.getInt(indexKey, 0).coerceAtLeast(0),
            offset = prefs.getInt("reader_item_offset_$tab", 0).coerceAtLeast(0)
        )
    } else if (tab == prefs.getInt("reader_tab", tab) && prefs.contains("reader_item_index")) {
        ReaderScrollPosition(
            index = prefs.getInt("reader_item_index", 0).coerceAtLeast(0),
            offset = prefs.getInt("reader_item_offset", 0).coerceAtLeast(0)
        )
    } else {
        fallback ?: ReaderScrollPosition(index = 3, offset = 0)
    }
}

private fun saveScrollPosition(
    prefs: SharedPreferences,
    tab: Int,
    position: ReaderScrollPosition
) {
    prefs.edit()
        .putInt("reader_tab", tab)
        .putInt("reader_item_index_$tab", position.index.coerceAtLeast(0))
        .putInt("reader_item_offset_$tab", position.offset.coerceAtLeast(0))
        .apply()
}

private fun sectionId(title: String): String = "section_" + title.hashCode().toUInt().toString(16)

private fun readLearningText(context: Context, resourceId: Int): String =
    LearningContentPreloader.get(context, resourceId)
        ?: "学习内容缓存未完成，请重新启动应用。"

private fun LazyListScope.bodhisattvaPath(contents: List<String>) {
    section("菩萨道", "以下内容按指定顺序呈现：菩萨道概括、佛像。")
    lesson("菩萨道概括", contents.getOrElse(0) { "" })
    lesson("佛像", contents.getOrElse(1) { "" })
}

private data class AncientYinfuImageMarker(val text: String, val resourceId: Int)

private sealed interface AncientYinfuSegment {
    data class Text(val value: String) : AncientYinfuSegment
    data class Image(val resourceId: Int) : AncientYinfuSegment
}

private val ancientYinfuImageMarkers = listOf(
    AncientYinfuImageMarker("（在这个位置加入上传的图片图一）", R.drawable.guyinfujing_image_1),
    AncientYinfuImageMarker("（这个位置放上传的图二）", R.drawable.guyinfujing_image_2),
    AncientYinfuImageMarker("（这个位置加入我上传的图三）", R.drawable.guyinfujing_image_3)
)

private fun ancientYinfuSegments(content: String): List<AncientYinfuSegment> {
    val segments = mutableListOf<AncientYinfuSegment>()
    val text = StringBuilder()

    fun flushText() {
        if (text.isNotEmpty()) {
            segments += AncientYinfuSegment.Text(text.toString())
            text.clear()
        }
    }

    content.split('\n', ignoreCase = false, limit = Int.MAX_VALUE).forEach { line ->
        val marker = ancientYinfuImageMarkers.firstOrNull { line.contains(it.text) }
        if (marker != null) {
            flushText()
            segments += AncientYinfuSegment.Image(marker.resourceId)
        } else {
            if (text.isNotEmpty()) text.append('\n')
            text.append(line)
        }
    }
    flushText()
    return segments
}

private fun LazyListScope.ancientYinfuPath(contents: List<String>) {
    repeat(2) { index ->
        item(key = "ancient-yinfu-text-box-$index") {
            AncientYinfuTextCard(contents.getOrElse(index) { "" })
        }
    }
}

@Composable
private fun AncientYinfuTextCard(content: String) {
    val settingsVm = LocalLearningSettingsVm.current
    val segments = remember(content) { ancientYinfuSegments(content) }
    Card(modifier = Modifier.fillMaxWidth(settingsVm.pageWidth / 100f)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            segments.forEach { segment ->
                when (segment) {
                    is AncientYinfuSegment.Text -> Text(
                        segment.value,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = settingsVm.fontSize.sp,
                        lineHeight = (settingsVm.fontSize * settingsVm.lineSpacing / 100f).sp
                    )
                    is AncientYinfuSegment.Image -> Image(
                        painter = painterResource(segment.resourceId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun LazyListScope.mainPath() {
    section("第一部分 · 先把佛法听明白", "手册先建立地图，再进入训练。修行方向以苦的止息为核心，不以特殊体验或打卡数量代替见法。")
    lesson("四圣谛", "苦谛：有为经验会变化，真正加重苦的是对身体、关系、身份和观点的抓取；集谛：渴爱与抓取使苦集起；灭谛：推动抓取的渴爱止息，相应地，苦也止息。道谛：以八正道为实践路径，逐步断除苦的根源。")
    lesson("佛、法、僧与归依", "佛是觉悟者，说明觉悟与解脱是可能的；法是佛所教的道路和真实规律，包括四圣谛、八正道、十二因缘与最终解脱；僧是持法修行者。归依并不意味着盲从，而是以清晰的信任与实践开始。")
    lesson("信、业与轮回", "信从对道路的信任开始，不强迫自己盲信；它应随着持戒后追悔减少、正念后冲动更容易被看见、定力提高后心更清楚而增强。业是身、口、意的有意造作，轮回是善恶因果不断继续的过程。")
    lesson("业果、后有与当下观察", "故意伤害、偷盗、邪淫、欺骗和恶意羞辱会形成不善条件，布施、持戒、忍辱、精进、禅定和智慧会形成善的条件。观察当下的冲动、语言和行为，可以更容易看见因果的连续性。")
    lesson("涅槃与善不善", "涅槃可先从止息理解：贪、瞋、痴、渴爱和抓取不再推动苦；这不是永恒灵魂搬到某处，也不是麻木或昏沉。善不只是社会认可，而是使心更清楚、少害、少执著。")
    lesson("中道与善法欲", "中道不是凡事一半一半，而是避开沉迷感官欲乐和以折磨身心换取解脱两个极端。沉迷欲乐的渴爱要逐渐减弱，但闻法、持戒、修定、资助他人等善法欲是应当培养的。")
    lesson("戒、定、慧", "戒在八正道中是正语、正业、正命；定是正精进、正念、正定，使心清楚、稳定、统一；慧是正见、正思惟，依四圣谛、缘起和不执著的观察而增长。")
    lesson("八正道", "八正道是正见、正思惟、正语、正业、正命、正精进、正念、正定。正见理解业果、四圣谛、缘起与解脱；正思惟趋向出离、不瞋、不害。")
    lesson("入流四分", "流就是八圣道，入流分有四：亲近善士、听闻正法、内正思惟、法次法向。善知识重视戒、定、慧并允许合理查证；听法围绕根本宣说、受持、反复观察。")
    lesson("五蕴", "五蕴是色、受、想、行、识。色是身体和物质的四大及其所造色；受是苦受、乐受、不苦不乐受；想是辨认、取相和定义；行包括思考、计划、冲动和身口意造作；识是认识与觉知。")
    lesson("集、味、患、离", "经教要求对色、受、想、行、识观察集、味、患、离：五取蕴以欲为根、以欲为集、以欲而生；味是对象带来的吸引力；患是维持和失去时的痛苦；离是看见不再依赖执著。")
    lesson("身见与二十种身见", "身见是围绕五取蕴建立真实、固定的自体。对每一蕴都可能把它当成我、认为我拥有它、认为它在我里面、认为我在它里面。实修重点不是死背二十条，而是看见‘我’被怎么构造出来。")
    lesson("六入、触、受、爱、取", "六入是眼、耳、鼻、舌、身、意及相应活动领域；根、境、识和合形成触，触后有受。乐受容易想继续，苦受容易排斥，不苦不乐时容易寻找刺激；护根从看清这条链就开始。")
    lesson("无常、苦、无我", "无常不是背诵‘什么都会变’，而是直接看情绪、身体、感受、想法和意识依条件生起、变化、减弱、消失。不能保持不变的东西，不该被当作“我”或“我的”。")
    lesson("入流方向", "入流是第一次不可逆地进入圣道，与三结断、四不坏净和圣戒密切相关。三结是身见、疑、戒禁取；四不坏净是对佛、法、僧以及圣者所赞之戒的不可动摇净信。")
    lesson("入流以后", "预流不是阿罗汉，也不是建立新身份的终点。欲贪和瞋恚尚未彻底断，仍要继续观察五受蕴、无常、苦、空、非我，继续落实戒、定、慧。")
}

private fun LazyListScope.householdEightfoldPath() {
    section("在家八正道", "手册第二部分把实修直接定位为八正道的全面实践。安般念不是脱离八正道的单独技巧；先把身、口、意与生活条件安顿在清楚、稳定、不伤害的方向上。")
    lesson("正见", "正见是理解业果、四圣谛、缘起和解脱的大方向，也就是如实知苦、断集、证灭、修道。正念、定与观察应导向贪、瞋、痴减弱、心更清明。")
    lesson("正思惟", "正思惟是让意向趋向出离、不瞋、不害。事情发生后观察它依什么条件建立、是否恒常、能否一直保持以及持续抓住是否成为苦和执著。")
    lesson("正语", "正语是不两舌、不恶口、不妄语、不说增长烦恼的无意义绮语；真话也尽量用不伤害的方式表达。没有证得的禅定、神通和果位，不能以‘我觉悟了’为名伤人。")
    lesson("正业", "正业使身、口、意远离杀生、偷盗、邪淫等恶业。在家居士以五戒为基本行为边界：不杀生、不偷盗、不邪淫、不妄语、不饮酒；实践越稳，修行越有基础。")
    lesson("正命 · 先守住谋生边界", "手册把戒与正命放在整条实修主线的前面：先尽量不依靠杀害、欺骗、剥削、诱导放逸或伤害众生的方式维持生计。生计清净，才能修心不被焦虑和内疚拖着走。")
    lesson("正精进", "正精进即四正勤：已生恶不善法令减弱、断除；未生恶不善法防护不令生；未生善法令生起；已生善法令增长、稳定。")
    lesson("正念", "正念是不忘失当前所缘和正确方向，也就是觉察观照当下发生的一切。眼见、耳闻、身体接触或念头牵动时，知道接触与随后的苦乐、追求与排斥，不急着跟它走。")
    lesson("正定", "正定是心离开粗重的欲和不善法，逐渐统一、安住、不散乱。坐下修安般念时反复回到所缘，不为了境界强迫呼吸或制造体验；经行时也应保持同样的稳定和清楚。")
    lesson("生活条件", "饮食知量、适当睡眠、减少不必要的欲乐刺激和事务。工作时专心工作，开车时专心道路，与人交谈时认真听；手机、网络、娱乐和情绪刺激要重新建立边界。")
}

private fun LazyListScope.senseRestraint() {
    section("护根 · 白天的延伸", "手册结语把护根定位为安般念在白天的延伸。重点不是逃避六根对象，而是在接触发生后尽早看见受、爱、取，不让它们继续推动行动和言语。")
    lesson("从六根接触开始", "眼、耳、鼻、舌、身、意接触对象时，先知道接触已经发生。根、境、识和合有触；触后有受。先停在可直接知道的经验上，不急于评价。")
    lesson("辨认三种受", "知道此刻是乐受、苦受还是不苦不乐受。乐受后容易追求延续，苦受后容易排斥，不苦不乐时容易寻找刺激；护根从看清这三种受开始。")
    lesson("受之后看爱", "问自己：是否正在想要更多、想立刻摆脱，或因为平淡而寻找刺激？先知道这股倾向，不立即跟随。能在受与爱之间看见空隙，后续判断才会更稳。")
    lesson("爱之后看取", "留意‘非要不可’‘绝不能这样’‘这就是我/我的’等抓住、认同与执持。不是压抑感受，而是停止继续喂养，让取不必自我强化。")
    lesson("手机、网络与娱乐", "若信息流、情色、争论、购物或娱乐明显成为贪欲、瞋恚、散乱的燃料，主动减少入口与重复刺激。护根不是拒绝生涯与社会，而是减弱自动反应的拉力。")
    lesson("冲突中的护根", "被批评或冒犯时，先知道声音或文字的接触、身体的苦受、瞋与反击冲动。强烈时先不发送消息、不恶口、不作重大决定，先把心收回并观察。")
    lesson("与坐禅衔接", "白天护根能减少坐下后的追悔和粗重扰动；坐禅培养的正念与定，又帮助白天更早看见触、受、爱、取。两者是同一条训练主线。")
}

private fun LazyListScope.aggregatesPractice() {
    section("五蕴观察", "五蕴不是背名词，而是把原来一团‘我’的经验拆开，如实观察色、受、想、行、识怎样依条件出现、变化与止息。")
    lesson("色", "观察身体和物质条件：食物、睡眠、年龄、疾病、温度与环境都会改变身体状态。照顾身体，但也看见它不能完全听命于‘我’。")
    lesson("受", "直接辨认苦受、乐受、不苦不乐受，并看它们不断变化。不要把感受自动等同于一个固定的我。")
    lesson("想", "观察辨认、取相与定义怎样受记忆、立场和情绪影响。同一句话可被理解成玩笑或羞辱，说明‘想’依条件建立并会改变。")
    lesson("行", "观察思考、意图、冲动、计划和身口意造作。生气时想做的事可能隔天完全不想做；思考出现后也会消失。")
    lesson("识", "观察眼、耳、鼻、舌、身、意六识依相应根境条件而起，不把识或所谓纯粹观察者重新抓成常住的我。识与名色相依互为条件，不能被当成万物之源。")
    lesson("色与四大", "色包括身体与物质条件，可从地、水、火、风四大的坚硬、流动、温热、推动等特征来观察。身体受食物、睡眠、年龄、疾病等条件持续影响。")
    lesson("集、味、患、离", "看五取蕴怎样集起、有什么吸引力、维持与失去有什么过患，以及如何在看得更完整后逐渐离开贪著。离欲不是咬牙压抑，而是看清事情的实际结构。")
    lesson("强烈我感时怎么观", "被批评、成功、失败或欲望强烈时，问：这个‘我’落在身体、感受、标签与记忆、意志冲动，还是意识与观察者上？先知道它正在出现，再决定是否继续喂养。")
    lesson("二十种身见", "对每一蕴都可能出现四种抓取：把它当成我、认为我拥有它、认为它在我里面、认为我在它里面。实修重点不是死背二十条，而是看见抓取正在发生。")
}

private fun LazyListScope.dependentOriginationPractice() {
    section("缘起观察", "日常最直接的入口是根境识和合生触、触生受、受后爱与取怎样继续发展。手册同时提醒：触—受—爱—取只是实用观察入口，完整缘起应回到整个因果链。")
    lesson("触", "先辨认接触：看见、听见、身体接触或一个明显念头出现。知道对象、相应识与接触已经发生，不急着评价。")
    lesson("受", "接触后辨认乐、苦、不苦不乐。受本身先被知道，避免直接跳到‘他让我生气’‘我必须得到它’等故事。")
    lesson("爱", "观察乐受后的追求、苦受后的排斥，以及平淡时寻找刺激的倾向。爱出现时只要如实知道，就已经把自动反应变成可观察的过程。")
    lesson("取", "观察爱怎样加重成抓住、认同、立场和‘非要不可’。尤其留意身份、观点、关系和欲乐如何被抓成‘我’与‘我的’。")
    lesson("有与后续行为", "取继续被喂养时，会推动新的身、口、意行为与后续条件。实修中重点看自己此刻如何继续造作，而不是用缘起理论武断站队。")
    lesson("完整缘起的方向", "手册保留无明、行、识、名色、六入处、触、受、爱、取、有、生、老病死忧悲苦恼的完整框架，并以各支止息说明苦的断除。")
    lesson("概念太多就返回呼吸", "若观察缘起变成脑内推理、直接经验越来越模糊，就停止分析，回到自然呼吸与身体，让心重新稳定；清楚后再从更小的因果一步步展开。")
}

private val anapanasatiSections = listOf(
    "开始修习",
    "十六步",
    "道品关系",
    "经教层次",
    "应对与次第"
)

private fun LazyListScope.anapanasati(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit
) {
    section(
        "第二部分 · 安那般那念",
        "以自然入息、出息为根本所缘，从身、受、心、法展开完整止观。完整修习不是单纯盯住呼吸，而是由呼吸建立正念与定，再如实观察身、受、心和法的变化。"
    )
    item {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "安那般那内部导航",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "按学习目的分成五个入口。切换入口会回到该部分顶部，十六步、道品关系和经教辨析各自独立，查阅时不必在一条长列表里反复寻找。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                anapanasatiSections.forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedSection == index,
                        onClick = { onSectionSelected(index) },
                        label = { Text(title) }
                    )
                }
            }
        }
    }
    when (selectedSection) {
        0 -> anapanasatiFoundations()
        1 -> anapanasatiSixteenSteps()
        2 -> anapanasatiPathFactors()
        3 -> anapanasatiScripturalLayers()
        else -> anapanasatiPracticeSupport()
    }
}

private fun LazyListScope.anapanasatiFoundations() {
    section("零基础 · 先学会怎么坐", "先把方法做简单、稳定、可重复。最初目标不是追求禅相或特殊感觉，而是能持续知道自然呼吸，走神后能清楚回来。")
    lesson("环境", "优先选择安静、通风、温度适宜且安全的地方。手机静音，提前处理饮水、如厕等需要。经中常说林中、闲房、树下、空露地，核心不是地理位置而是安静和能放松。")
    lesson("坐姿", "椅坐、盘坐都可以。让身体稳定而不过度僵硬：脊柱自然直立，肩颈和腹部放松，双手安放稳定。端身正坐的重点是清醒、稳定、舒服，而非僵硬或刻意摆姿势。")
    lesson("眼睛与身体", "可以轻闭眼，也可微睁眼自然垂视。昏沉明显时睁眼、坐直或改经行；紧张时先放松额头、下颌、肩膀、腹部。不要用憋气或屏住呼吸去制造定力。")
    lesson("呼吸放在哪里", "选择最容易清楚知道呼吸的区域即可，例如鼻端、鼻孔周围、胸腹起伏或整个呼吸过程。选定后先稳定使用，不必频繁寻找更“高级”的所缘。")
    lesson("一开始只做这一件事", "吸气时知道“正在吸气”，呼气时知道“正在呼气”。不知道时就是不知道；一发现走神，就知道“刚才走神了”，并回到下一次呼吸。")
    lesson("需要数息怎么办", "若完全无法连续知道几次呼吸，可暂时用简单计数辅助，例如每次呼气默数一到十，再从一开始；一旦呼吸已经较清楚，最好逐渐放弃计数。")
    lesson("一次练多久", "零基础可以从能稳定坚持的短时段开始，再逐渐延长。时间长短不是证量；宁可每天规律练习，也不要为了完成时长而硬撑。")
    section("修习前 · 五项助缘", "《杂阿含经》明确列出五类对安那般那很有帮助的条件。把坐禅前的生活整理好，往往比坐下后硬压妄念更重要。")
    lesson("1 · 戒与威仪", "守住戒与行为边界，对细小过失保持警觉。对在家人而言，至少以五戒、正语、正业、正命作为稳定基础。追悔少，心较容易安住。")
    lesson("2 · 少欲、少事、少务", "减少不必要的欲望、事务和信息刺激，不让生活长期处在过载状态。不是逃避责任，而是减少会不断占据心的多余东西。")
    lesson("3 · 饮食知量", "饮食取适量，不因贪味不断追求。过饱容易昏沉，过度饥饿也会扰乱身心；以支持清醒、健康和修习为原则。")
    lesson("4 · 睡眠与精勤", "经文强调初夜、后夜不贪著睡眠、保持精勤；在家实践不应理解为强行剥夺必要睡眠。先保证基本健康与清醒，再减少懒散。")
    lesson("5 · 清静处", "尽量选择少愦闹、少干扰的地方。清静不是目的本身，而是帮助心减少外缘，便于如实看见呼吸与身心活动。")
    lesson("先认识五盖", "贪欲、瞋恚、昏沉睡眠、掉举后悔、疑会扰乱内心、削弱智慧。经中把远离五盖放在端身正坐、系念面前之后、正式展开十六步之前。")
    lesson("从日常到坐禅", "经文描述的完整背景包括守护身体、守诸根门、善系心住，完成日常事务后进入清静处，端身正坐，系念面前，再远离五盖、安住呼吸。")
}

private fun LazyListScope.anapanasatiSixteenSteps() {
    section("十六步 · 身念处", "先把前四步练熟，再逐渐扩展。")
    lesson("第 1 步 · 知入息出息", "吸气时清楚知道正在吸气，呼气时清楚知道正在呼气；不向外攀缘，保持正知、正念。")
    lesson("第 2 步 · 知长短", "呼吸长时知道长，呼吸短时知道短，如实知道，不刻意制造长短。")
    lesson("第 3 步 · 觉知一切身", "随吸气、呼气学习觉知整个息身过程，让觉知从单一点扩展到完整的呼吸经验。")
    lesson("第 4 步 · 身行止息", "身体放松、心逐渐安定时，呼吸会自然趋于平缓、柔和、细微。只清楚觉知，不屏息、不故意压细呼吸。")
    section("十六步 · 受念处", "第五至第八步把喜、乐与心行纳入觉知。")
    lesson("第 5 步 · 觉知喜", "随入息、出息觉知喜的生起、变化与减弱，不追逐喜，也不把喜当成成就证明。")
    lesson("第 6 步 · 觉知乐", "随入息、出息觉知身心的乐受；知道乐受本身会变化，不为了延长它而控制呼吸。")
    lesson("第 7 步 · 觉知心行", "随入息、出息觉知心行的状态，知道想、思以及情绪活动如何影响当下经验。")
    lesson("第 8 步 · 令心行止息", "随入息、出息学习让粗重心行渐渐止息；以放松、清楚和不继续喂养为方向，不强迫心念停止。")
    section("十六步 · 心念处", "第九至第十二步直接觉知并调伏心。")
    lesson("第 9 步 · 觉知心", "随入息、出息直接知道心是散乱、昏沉、瞋恚、安定或清明，不急着评判。")
    lesson("第 10 步 · 令心欣悦", "在正念和善法支持下令心欣悦、明亮，不靠刺激或追逐特殊体验制造兴奋。")
    lesson("第 11 步 · 令心安定", "随入息、出息让心逐渐统一、安住、得定；散乱时温和回来，不强行压制。")
    lesson("第 12 步 · 令心解脱", "随入息、出息学习令心从当下的盖、执著和扰动中得到空间，并清楚知道心的解脱状态。")
    section("十六步 · 法念处", "第十三至第十六步进入观法。")
    lesson("第 13 步 · 观察无常", "随入息、出息观察呼吸、身心状态和觉知如何生起、变化、减弱与消失。")
    lesson("第 14 步 · 观察断", "随入息、出息观察不善推动与执著如何被放下、断除，不把断理解成毁灭身体或压抑经验。")
    lesson("第 15 步 · 观察无欲", "随入息、出息观察对对象的贪著逐渐减弱，清楚知道离欲不是厌恶或强迫。")
    lesson("第 16 步 · 观察灭", "随入息、出息观察推动苦的条件止息。十六步都应反复学习、反复安住，而不是偶尔做一次概念分析。")
    lesson("十六步与四念处", "前四步满足身念处，第五至八步对应受念处，第九至十二步对应心念处，第十三至十六步对应法念处。完整安般念由此展开。")
    section("十六步怎么实际推进", "十六步是完整训练方向，不要求初学者第一天就同时完成十六项。先让前面的训练稳定，再让后面的训练自然进入。")
    lesson("前四步练到什么程度再往后", "能够较连续知道入息、出息；长短变化能如实辨认；能觉知较完整的呼吸过程；身体和呼吸在不控制的情况下趋于稳定。")
    lesson("喜与乐不是必须制造", "第五、第六步是觉知已经出现的喜与乐，不是命令自己产生喜乐。若当下没有明显喜乐，就继续稳定前四步，不需强行制造。")
    lesson("心行怎么理解", "修习中可直接观察与感受相伴、影响心的活动怎样粗重或柔和。重点不是在坐中做复杂名相分析，而是知道心受什么推动和改变。")
    lesson("令心欣悦、安定、解脱怎么做", "不是用意志制造兴奋、空白或无念。心低沉时忆念善法、看见修习本身的清净方向，使心明亮；心散乱时回到呼吸，温和回来。")
    lesson("最后四步不是思考哲学", "观察无常、断、无欲、灭，要以正在发生的经验为基础：一口呼吸会生灭，感受与心态会变化；不继续抓取时，心会逐渐更清楚、更轻松。")
    lesson("什么时候退回前一步", "若后面的观察让心变得更散乱、更紧张、更概念化，就退回清楚知道自然呼吸，甚至只做第一步。能稳定地退回基础，说明修行有回到真实的能力。")
}

private fun LazyListScope.anapanasatiPathFactors() {
    section(
        "道品关系 · 从安般念到三十七菩提分法",
        "安般念是完整圣道中的一个重要修法。经中明确给出的次第是：安般念多修习，四念处满足；四念处满足，七觉分满足；七觉分满足，明与解脱渐次增长。"
    )
    lesson(
        "三十七菩提分法 · 七组共三十七支",
        "三十七菩提分法合计 4＋4＋4＋5＋5＋7＋8＝37：四念处、四正勤、四如意足、五根、五力、七觉支、八圣道。它是修行的系统地图。"
    )
    lesson(
        "经中明确的安般念次第",
        "《杂阿含经》在安那般那的相关经教中明确说明：安那般那念多修习，能令四念处满足；四念处满足，令七觉分满足；七觉分满足，令明、解脱等得以增长。"
    )
    lesson("四念处与七觉分", "正念安住后，依次培养念、择法、精进、喜、轻安、定、舍。七觉分以远离、无欲、灭为依，趋向于舍，并导向明与解脱。")
    lesson("七觉分不是七个勋章", "念使所缘不忘失；择法如实辨别善不善与身心状态；精进令善法增长；喜使心对善法有兴趣；轻安令身心柔和；定使心安住；舍让心不被执著牵引。")
    lesson("安那般那 → 四念处 → 七觉分 → 明与解脱", "《杂阿含经》直接给出这条次第：安那般那多修习令四念处满足，四念处满足令七觉分满足，七觉分满足则让明与解脱能继续展开。")
    lesson("与四正勤相应", "修习时未生恶不善法防护不令生，已生恶不善法令减弱、断除；未生善法令生，已生善法令增长稳定。发现五盖并停止喂养，是四正勤的直接体现。")
    lesson("与五根、五力相应", "信让人愿意依正法持续实践；精进使修习不中断；念使呼吸与身心不被忘失；定使心统一；慧如实知无常、离欲与止息。")
    lesson("与四如意足相应", "欲、精进、心、观（思惟）所成的定与勤行，是四如意足的基本结构。放在安那般那中，可理解为：对善法有清净意愿，持续精进，心能安住，观察不偏离正确方向。")
    lesson("与八正道相应", "安那般那必须放在正见、正思惟、正语、正业、正命、正精进、正念、正定中修。坐上的正念正定若与生活中的戒、正见和正语相应，修行就会稳住。")
    lesson("安般念与四禅", "初禅离五盖，具觉、观、喜、乐、一心；第二禅无觉、无观，具喜、乐、一心；第三禅离喜，具乐、一心；第四禅离乐，具一心和平静。安般念是进入四禅的基础训练。")
}

private fun LazyListScope.anapanasatiScripturalLayers() {
    section(
        "经教层次 · 为什么修与如何定位",
        "这一部分把经中对安那般那的作用、层次和边界放在一起阅读。经教描述的是修法的方向与成熟差别，不是让初学者凭一次短暂体验给自己贴标签。"
    )
    lesson("佛陀为什么反复教安那般那", "《杂阿含经》称安那般那多修习有‘大果大福利’，能断诸觉想、令心不动摇，并以‘甘露、究竟甘露’等名词描述其深远作用。")
    lesson(
        "阿梨瑟咤经教 · 从不染著到更胜妙",
        "阿梨瑟咤说，他对过去诸行不顾念、不追忆，对未来诸行不生欣乐，对现在诸行不生染著，并把内外对碍想善正除灭。佛先认可：这是在修安那般那。"
    )
    lesson(
        "学住/如来住 · 同修安般而断惑深浅不同",
        "《杂阿含经》回答摩诃男说：学住异如来住异。学住者，断五盖多住，仍是在反复远离、断除五盖的修习中安住；如来住者，五盖已断、已知、已证，心更稳、更彻底。"
    )
    lesson("安般念的直接作用", "反复修习安般念，身能止息、心能止息，内心趋向寂灭与专一，明慧的修习逐渐满足；散乱的觉想减弱，心较不动摇。")
    lesson("更胜妙的安般念", "不追忆过去、不欣乐未来、不染著现在并除去障碍之想已有训练价值；更完整的方向，是依十六步从呼吸开始，经过身、受、心、法逐步展开。")
    lesson("安般念与定 · 从明显到微细", "完整修习安般念能令身心安住、不倾不动，三昧熟练后，需要时较容易进入。修习可先安住于较明显的呼吸，其后再逐渐让心从粗到细地定住。")
    lesson("安般念与禅定及圣果", "手册把安般念与初禅、第二禅、第三禅、第四禅相联系，也提到慈、悲、喜、舍以及空无边处、识无边处、无所有处等更深层次的定。")
    lesson("诸行渐次止息", "手册列出更深定境中诸行渐次止息：初禅言语止息；第二禅觉、观止息；第三禅喜止息；第四禅出入息止息；空无边处色想止息等。")
    lesson("身行、口行、意行", "出息与入息依身体而有，称身行；觉与观能引发语言，称口行；想与思依心而转，称意行。手册以此说明更深止息的层次。")
    lesson("熟练后的稳定性", "《杂阿含经》描述安那般那三昧熟练时，身心安住、不倾不动，能住胜妙住；重点是长期多修习形成稳定能力，而不是追求一时刺激。")
}

private fun LazyListScope.anapanasatiPracticeSupport() {
    lesson("呼吸越来越细时", "呼吸微细不必故意加深，也不要紧张。能知道多少就如实知道多少。第四禅中出入息止息是深定结果的描述，不能倒过来硬强求。")
    lesson("找不到呼吸时怎么办", "先检查是否因为紧张而在寻找“某种特别的呼吸”。放松身体，不主动深呼吸，等待下一次自然入息或出息自己显现。")
    lesson("妄念很多时怎么办", "不要逐条消灭念头。先知道“正在想”，不继续内容，回到下一次呼吸。若同一件现实问题反复出现，可坐后处理；坐中顺着呼吸会更稳。")
    lesson("昏沉时怎么办", "先端正身体、睁眼、增加光线，呼吸觉知稍微清楚有力；仍然昏沉就起身经行。若真正缺乏睡眠，应恢复必要睡眠，不把疲劳误认为禅定寂静。")
    lesson("紧张、焦虑时怎么办", "先停止追求“必须马上定下来”。确认自己没有在控制呼吸，放松肩、下颌和腹部，只知道一两次自然呼吸；必要时起身站立、经行后再回到坐姿。")
    lesson("疼痛时怎么办", "先辨认疼痛本身、紧张和排斥是不是不同层次。轻度不适可观察变化；明显或持续加重的疼痛应调整姿势，不以伤害身体为代价追求定力。")
    lesson("出现喜、光、震动、身体消失感", "先把它们当作会变化的身心现象：知道出现，知道变化，不追、不怕，也不据此判断禅那、神通或果位。")
    lesson("修习成果不要只看坐上", "经中说修习可令身不疲倦、随顺观住乐而不染著乐，也把它与离欲、禅定、断结和解脱联系起来。因此真正的检验是生活能力与心的清明程度。")
    lesson("完整次第", "戒与生活安顿 → 少欲少务、饮食知量、适当睡眠、清静处 → 端身正坐、系念面前 → 认识并远离五盖 → 稳定知道自然入息出息 → 逐步展开十六步与四念处 → 白天护根与日常正行。")
    lesson("零基础的一次完整练习", "①安顿环境与坐姿；②放松但保持清醒；③知道自然入息、出息；④走神就知道并回来；⑤如实知长短；⑥逐渐把觉知扩展到整个呼吸过程；⑦结束时回到日常身体状态。")
}

private fun LazyListScope.hindrances() {
    section("五盖现前时", "先知道‘五盖正在出现’，不要立刻把它当成失败，也不要继续增加燃料；处理后回到自然入息与出息。")
    lesson("贪欲", "停止继续喂养刺激，知道心正在追逐什么，也知道身体的急迫、躁动或紧张。暂时不跟随对象，回到自然呼吸；减弱后继续安住。")
    lesson("瞋恚", "强烈时先不说伤人的话、不发送冲动消息、不作重大决定。知道苦受、身体紧绷、发热、冲动与瞋心；身体能放松的地方就放松，回到呼吸。")
    lesson("昏沉睡眠", "坐直身体、睁眼、增加光线，让觉知清楚；仍昏沉可起身经行。若真正缺乏睡眠，应恢复必要睡眠，不把疲劳误认为禅定寂静。")
    lesson("掉举后悔", "不跟每个念头跑，只清楚知道几次自然呼吸，把范围收回来。若后悔来自确实做错的事，坐后承认、补救、改正，不用反复自责。")
    lesson("疑", "分清是当下犹疑还是需要查证的理论问题。理论问题记下，坐后查证；坐中先依已经确定的无害方法练习，避免分析把心完全带离所缘。")
    lesson("七觉分调节", "心昏沉时可加强念、择法、精进、喜；心掉举时可加强轻安、定、舍。念贯穿全程，调节的目的都是让心重新清楚、稳定。")
    lesson("五根与五力", "信、精进、念、定、慧称五根，成熟有力时称五力。信给方向，精进给动力，念让训练不断线，定让心稳定，慧让心如实见苦与止息。")
    lesson("护根", "日间观察接触→受→爱→取→回应。重点是更早发现受之后的贪爱或排斥倾向，停止继续喂养，为回应留下空间。")
}

private fun LazyListScope.walkingMeditation() {
    section("经行", "经行是在行走中保持正念，是正式修习的一部分。共同原则是：身体在走，心清楚知道；不散乱，也不把动作做得僵硬造作。")
    lesson("准备", "选择安全、平直、少干扰的一小段路。站稳，眼睛睁开，视线自然落在前方地面；肩、颈、下颌放松。先知道站立与‘要走’的意向。")
    lesson("基本方法", "走时清楚知道正在走：移动知道移动，左右脚交替知道交替，触地知道触地，重心转换知道转换。走神后发现即可回来，不责备自己。")
    lesson("所缘选择", "可觉知全身行走的总体感觉，也可把范围缩小到双脚与小腿。心较散时，脚离地、向前、触地、重量落下等较明确的感觉可帮助心重新安住。")
    lesson("标记与步速", "可轻轻标记‘走、走’‘左、右’或‘抬起、推前、放下’，但标记只是辅助。步速以让心更清楚、更稳定为准；昏沉可稍快一点，散乱时稍慢一点。")
    lesson("停与转", "到尽头不要急转。先停下知道站立，知道准备转身的意向，用自然小步转身；转好再站稳后继续。站、走、停、转都属于修习。")
    lesson("结束与坐禅衔接", "最后一步不要突然退出。停下知道站立并感受全身。经行以身体行走为主，坐禅以呼吸为主；不强迫一步配一息，经行后再回到呼吸即可。")
    lesson("困难调整", "妄想：重新感觉脚步或身体；念头很强时可先停下，知道站立、念头与情绪，等心有空间再走；昏沉：挺直、睁眼、适当加快步速；紧张：放松肩和下颌，再回到脚步。")
    lesson("观察无常", "正念稳定后，直接看脚抬起又落下、触感出现又消失、重量不断转移、念头来了又走。不需要一边走一边用语言推理‘无常’。")
    lesson("常见偏差", "不要把经行变成聊天或看手机；不是越慢越好；不要过分低头盯脚；不要强迫脚步配呼吸；不要让标记变成机械口令；不要追求某种特别的“轻安感”。")
    lesson("精要", "身体行走时知道身体行走；心散乱时知道散乱并重新回来。让坐禅中的觉知延伸到活动中，使正念保持相续。")
}

private fun LazyListScope.longTerm() {
    section("长期修习", "长期检验看的是行为、心与智慧的方向，而不是只看累计坐禅小时数。")
    lesson("标准长期训练", "每天保持一到两座安般；白天持续护根；每周固定闻法与复盘；每周至少一段较长安静时段；定期八戒或简化日；持续布施与从生活中对照修行。")
    lesson("先看行为", "五戒是否更稳；说话是否更诚实；正命是否更清净；悭吝与占有是否慢慢减轻。")
    lesson("再看心", "安般念是否更容易持续；五盖是否更早被发现；贪起来是否更少立即跟随；瞋起来是否更少变成伤人的语言行动；平淡时是否不再靠刺激维持心情。")
    lesson("再看智慧", "受与爱、爱与取是否越来越容易分开看见；是否更清楚身、受、心、法依条件出现并变化；是否更少把身体、感受、想法、意志当成固定的我。")
    lesson("三个月大复盘", "检查五戒故犯、谎言、情色依赖、酒醉、购物冲动、网络瞋、恢复正念速度、五盖强度、定的可重复性、四念处清晰度，以及与善知识和法门的关系。")
    lesson("方向性判据", "若生活与心行长期朝善、离欲、清明、少执著变化，继续稳定；若长期相反，回头检查老师、方法、生活方式与自己的动机和边界。")
    lesson("选择善知识 · 边界先于权威", "手册给出的判断重点不是个人魅力，而是是否把人带回法。若有人要求破五戒、以所谓特殊法门合理化性关系、暴力、羞辱或控制，先退出，不以“尊重师长”为由放弃基本边界。")
}

private fun LazyListScope.glossary() {
    section("常见术语", "以下定义以上传的《在家居士入流修行手册》和经教整理稿为准；白话说明用于帮助进入经验，但不能脱离完整上下文。")
    lesson("四圣谛", "苦谛是有为经验会变化，抓取使苦加重；集谛是渴爱与抓取使苦集起；灭谛是推动抓取的渴爱止息，相应的苦止息；道谛是以八正道为实践路径。")
    lesson("佛、法、僧与归依", "佛是觉悟者，说明觉悟与解脱是可能的；法是佛所教的道路和真实规律，包括四圣谛、八正道、十二因缘与最终解脱；僧是持法修行者。归依并不等于盲信，而是以信、持戒和实践为基础。")
    lesson("信", "信从信任开始，但不要求强迫自己盲信。信会随着持戒后追悔减少、正念后冲动更容易被看见、定力提高后心更清楚而增长。")
    lesson("阿含、尼柯耶", "保存早期佛教经教的不同传承集合，是本手册整理四圣谛、缘起、五蕴和入流道路的重要经教依据。")
    lesson("业、果报与轮回", "业以有意图的身、口、意行为为核心。故意伤害、偷盗、邪淫、欺骗和恶意羞辱会形成不善之因，布施、持戒、忍辱、精进、禅定和智慧会形成善之因。")
    lesson("后有", "后有是与渴爱、取、有及生死流转相关的后续存在。四圣谛里的集会导致后有的渴爱，无明爱取是轮回的根本条件。")
    lesson("涅槃", "涅槃是贪、瞋、痴、渴爱和取的究竟止息。它不是永恒灵魂搬到某处，不是麻木或昏沉，也不是把死后什么都没有当作涅槃。")
    lesson("善、不善与善法欲", "令贪、瞋、痴减少、心更清楚、更不害、更能放下的行为，称作善；令贪、瞋、痴增加、伤害和迷乱加重的行为，称作不善。善法欲是愿意追求更清净、更少执著的方向。")
    lesson("中道", "中道不是凡事取一半，而是避开沉迷感官欲乐的执着，也避开认为折磨身心会导向解脱的执着。身体是修道的条件，应正常吃饭、睡眠和工作。")
    lesson("戒、定、慧", "戒在八正道中是正语、正业、正命；定是正精进、正念、正定，使心清楚、稳定、统一；慧是正见、正思惟，依四圣谛、缘起和不执著的观察而增长。")
    lesson("八正道", "八正道包括正见、正思惟、正语、正业、正命、正精进、正念、正定。正见理解业果、四圣谛、缘起与解脱；正思惟趋向出离、不瞋、不害。")
    lesson("五戒与正命", "在家居士最基础的戒是不杀生、不偷盗、不邪淫、不妄语、不饮酒。正命是以合法且不损害他人的方式维持生命，不贩卖众生的痛苦来谋生。")
    lesson("五盖", "五盖是贪欲、瞋恚、昏沉睡眠、掉举恶作、疑，是使心不清楚、不安住、不能如实观察的主要障碍。")
    lesson("四念处", "四念处是身念处、受念处、心念处、法念处；以正念直接观察身、受、心、法的生起、变化和止息，是安般念展开为完整止观的基础。")
    lesson("七觉支", "七觉支是念、择法、精进、喜、轻安、定、舍。昏沉时可加强念、择法、精进、喜，掉举时可加强轻安、定、舍；念贯穿全程。")
    lesson("五根与五力", "信、精进、念、定、慧称五根；当这些善法成熟、有力量、能抵抗相反烦恼时称五力。信给方向，精进给动力，念让训练不忘失，定让心稳定，慧让心如实见法。")
    lesson("五蕴 / 五取蕴", "五蕴是色、受、想、行、识：色是身体和物质，受是苦乐与不苦不乐，想是辨认取相，行是思考意图等造作，识是依根境生起的认识。五取蕴会让人以为有‘我’固定存在。")
    lesson("六入、触", "六入是眼、耳、鼻、舌、身、意及相应活动领域；根、境、识三者和合形成触。触不是孤立的对象，而是接触成立后，受以及随后的爱和取才能生成。")
    lesson("受、爱、取", "受有乐受、苦受和不苦不乐受；爱是想要、排斥以及对存在或消失的渴求；取是爱继续发展后的抓住、认同和执持。生活中很多冲突都能追溯到这三个环节。")
    lesson("缘起", "缘起是诸法依条件集起，也依条件止息，完整结构包括无明、行、识、名色、六入处、触、受、爱、取、有、生、老病死忧悲苦恼等。")
    lesson("无常、苦、无我", "无常是依条件生起的法不能永远保持不变；苦是被抓取的有为经验不能提供永久、完全可靠的满足；无我是对五蕴、六入、心、法的执著被知道以后逐渐显露的方向。")
    lesson("正念、正知、定、慧", "正念是不忘失当前所缘和正确方向，觉察观照当下发生的一切；正知是清楚知道自己正在做什么、状态是否适合；定使心安住，慧使心如实了解。")
    lesson("厌离与离欲", "厌离是看清过患以后逐渐失去迷醉；离欲是对欲染不再贪著。它不是压抑感受，而是看得更完整以后，对对象的估值真实改变。")
    lesson("三结与四不坏净", "三结是身见、疑、戒禁取。四不坏净是对佛、法、僧不可动摇的净信，以及成就圣者所赞之戒；它们与经教中的入流判定密切相关。")
    lesson("集、味、患、离", "集是看清一个对象怎样集起，味是看见它有什么吸引力，患是看见它无常、变化、维持与失去的过患，离是看清完整以后不继续被牵引。")
    lesson("居士与优婆塞", "优婆塞是在家过清净生活、发愿尽此一生归依佛、归依法、归依比丘僧的人。本手册特别关注居士怎样在工作、家庭和社交中保持清净。")
    lesson("优婆塞六具足", "信具足、戒具足、闻具足、舍具足、慧具足和定具足，是在家居士持续修道的基础。")
    lesson("优婆塞事", "优婆塞的实践次第是信、戒、施、在适当时候亲近沙门、专心听法、受持所闻、观察甚深义理，并随顺所知实行法次法向。")
    lesson("法行与正行", "法行、正行是远离杀生乃至邪见，修十善业迹；以清净持戒、离欲和正行作为基础，可以随顺趋向善趣、禅定以及断三结。")
    lesson("预流 / 入流", "预流或入流是第一次不可逆地进入圣道，与三结断、四不坏净和圣戒密切相关；不能用见光、身体消失感、一次无念、梦境等来替代真实判定。")
    lesson("一来、不还、阿罗汉", "一来即斯陀含，是预流以后继续减弱欲贪和瞋恚的圣道阶段；不还即阿那含，欲界贪瞋进一步断除；阿罗汉是漏尽者。")
    lesson("入流四分", "入流四分是亲近善士、听闻正法、内正思惟、法次法向；‘流’指八圣道。重点是亲近重视戒定慧且允许查证的善知识，围绕可靠的法来修。")
}

private fun LazyListScope.safety() {
    section("安全边界", "修行应使人越来越清楚、稳定、少贪瞋与取著，而不是越来越失去现实功能。")
    lesson("特殊身心现象", "光影、震动、明显喜乐、轻安、身体边界变淡、呼吸变细等出现时，知道即可。不追逐、不恐惧、不急着解释，也不凭一次异常感觉就认定自己已经证果。")
    lesson("不强迫呼吸", "呼吸变细时保持自然，不过度干预。不要屏息、压细呼吸或为了重复某次体验而刻意控制呼吸、姿势和心境。")
    lesson("异常情况先降低强度", "若练习持续造成明显失眠、强烈恐惧、现实感异常、严重绝望、自伤冲动，或已经无法维持基本生活，应停止强化训练，降低强度并寻求专业帮助。")
    lesson("身体健康优先", "任何修行都不能成为自伤、自杀、拒绝必要医疗、长期剥夺睡眠、严重营养不足或逃避现实责任的理由。疼痛、眩晕或明显不适都需要优先照顾身体。")
    lesson("居士责任", "无我不能用来逃避责任。出离也不等于突然抛弃父母、子女、债务、工作或照护责任；修行应与诚实、守戒、正命和基本生活责任相容。")
    lesson("概念过多时回到安般", "观缘起、五蕴或无我时，如果脑内分析越来越多、直接经验越来越模糊，就停止分析，回到自然呼吸，让心重新稳定；清楚后再从更小的层次重新观察。")
    lesson("最后的主线", "戒和正命先守住；白天护根；坐下来修安般念；五盖在主线中认识和处理；心稳定后让四念处展开，再从直接经验观察受如实、爱与取的链条。")
}

private fun LazyListScope.section(title: String, content: String) {
    item {
        val settingsVm = LocalLearningSettingsVm.current
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = settingsVm.fontSize.sp,
                lineHeight = (settingsVm.fontSize * settingsVm.lineSpacing / 100f).sp
            )
        }
    }
}

private fun LazyListScope.lesson(title: String, content: String) {
    item(key = sectionId(title)) { LessonCard(title, content) }
}

@Composable
private fun LearningResumeCard(
    prefs: SharedPreferences,
    tabs: List<String>,
    currentTab: Int,
    onResume: () -> Unit
) {
    val lastTab = prefs.getInt("reader_tab", currentTab).coerceIn(0, tabs.lastIndex)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("继续学习", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("最近阅读：${tabs[lastTab]}")
            TextButton(onClick = onResume) { Text("回到上次位置") }
        }
    }
}

@Composable
private fun LessonCard(title: String, content: String) {
    val settingsVm = LocalLearningSettingsVm.current
    Card(modifier = Modifier.fillMaxWidth(settingsVm.pageWidth / 100f)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = settingsVm.fontSize.sp,
                lineHeight = (settingsVm.fontSize * settingsVm.lineSpacing / 100f).sp
            )
        }
    }
}
