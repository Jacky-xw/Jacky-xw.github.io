package com.ruru.practice.feature.learning

/**
 * Stable local article/section models used by the Markdown scanner and future
 * content sources. They keep article identity separate from display labels.
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
