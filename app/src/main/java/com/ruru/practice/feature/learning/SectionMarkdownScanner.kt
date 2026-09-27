package com.ruru.practice.feature.learning

/** Markdown heading scanner. Fenced code blocks are ignored and heading levels can be constrained. */
object SectionMarkdownScanner {
    fun scan(articleId: String, markdown: String, levels: IntRange = 1..3): List<LearningSection> {
        var fenced = false
        val headings = mutableListOf<String>()
        markdown.lineSequence().forEach { raw ->
            val line = raw.trimStart()
            if (line.startsWith("```")) { fenced = !fenced; return@forEach }
            if (fenced) return@forEach
            val match = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$").find(line) ?: return@forEach
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            if (level in levels && title.isNotBlank()) headings += title
        }
        return headings.mapIndexed { index, title ->
            LearningSection(id = "${articleId}_section_${index + 1}", title = title, order = index + 1)
        }
    }
}
