package com.ruru.practice.feature.learning

/** A semantic reader page. Pagination prefers paragraph/sentence boundaries over raw character cuts. */
data class ReaderPage(val index: Int, val sectionId: String, val content: String)

fun paginateText(content: String, pageSize: Int = 1200): List<String> {
    require(pageSize > 0)
    val paragraphs = content.replace("\r\n", "\n").split(Regex("\\n\\s*\\n"))
        .map { it.trim() }.filter { it.isNotEmpty() }
    if (paragraphs.isEmpty()) return emptyList()

    val units = paragraphs.flatMap { paragraph ->
        if (paragraph.length <= pageSize) listOf(paragraph)
        else paragraph.split(Regex("(?<=[。！？.!?])\\s*"))
            .filter { it.isNotBlank() }
            .flatMap { sentence ->
                if (sentence.length <= pageSize) listOf(sentence.trim())
                else sentence.chunked(pageSize)
            }
    }
    val pages = mutableListOf<String>()
    val current = StringBuilder()
    for (unit in units) {
        val separator = if (current.isEmpty()) "" else "\n\n"
        if (current.isNotEmpty() && current.length + separator.length + unit.length > pageSize) {
            pages += current.toString()
            current.clear()
        }
        current.append(if (current.isEmpty()) "" else "\n\n").append(unit)
    }
    if (current.isNotEmpty()) pages += current.toString()
    return pages
}

fun paginateSections(sections: List<ReaderPage>, pageSize: Int = 1200): List<ReaderPage> {
    var globalIndex = 0
    return sections.flatMap { section ->
        paginateText(section.content, pageSize).map { text ->
            ReaderPage(index = globalIndex++, sectionId = section.sectionId, content = text)
        }
    }
}
