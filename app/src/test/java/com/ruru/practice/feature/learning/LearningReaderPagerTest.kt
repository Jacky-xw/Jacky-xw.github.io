package com.ruru.practice.feature.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningReaderPagerTest {
    @Test fun paginationPreservesTextAndPrefersBoundaries() {
        val text = "第一段内容。第二句继续。\n\n第二段内容。第三句继续。"
        val pages = paginateText(text, 14)
        assertTrue(pages.size >= 2)
        assertEquals(text.replace("\n\n", "").replace(" ", ""), pages.joinToString("").replace("\n\n", "").replace(" ", ""))
    }

    @Test fun sectionPageIndexesAreGlobalAndStable() {
        val input = listOf(ReaderPage(0, "s1", "一。二。三。"), ReaderPage(0, "s2", "四。五。六。"))
        val pages = paginateSections(input, 4)
        assertEquals(pages.indices.toList(), pages.map { it.index })
        assertTrue(pages.any { it.sectionId == "s2" })
    }
}
