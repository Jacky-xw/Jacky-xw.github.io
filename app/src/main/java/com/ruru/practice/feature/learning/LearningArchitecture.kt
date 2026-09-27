package com.ruru.practice.feature.learning

/**
 * 学习中心结构模型：
 * ArticleId / SectionId 用于长期扩展目录定位、阅读恢复和统计。
 * 数据保存在本地，不依赖网络。
 */
data class LearningArticle(
    val id: String,
    val title: String,
    val category: String,
    val sections: List<LearningSection>
)

data class LearningSection(
    val id: String,
    val title: String,
    val order: Int
)

object LearningAchievement {
    const val FIRST_STEP = "完成第一篇学习"
    const val WEEK_STREAK = "连续学习7天"
    const val MONTH_PROGRESS = "完成一个月学习记录"
}
