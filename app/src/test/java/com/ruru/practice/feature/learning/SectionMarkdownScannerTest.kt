package com.ruru.practice.feature.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionMarkdownScannerTest {
    @Test fun scansHeadingsButIgnoresFencedCodeAndDeepHeadings() {
        val md = "# 第一章\n正文\n## 第一节\n```\n# 代码标题\n```\n#### 忽略层级\n## 第二节"
        val result = SectionMarkdownScanner.scan("a", md)
        assertEquals(listOf("第一章", "第一节", "第二节"), result.map { it.title })
        assertEquals(listOf("a_section_1", "a_section_2", "a_section_3"), result.map { it.id })
    }
}
