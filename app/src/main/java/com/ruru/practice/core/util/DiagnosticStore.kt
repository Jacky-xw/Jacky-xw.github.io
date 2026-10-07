package com.ruru.practice.core.util

import java.io.File

/** Only called from the diagnostic writer. No dependency on Android or timer state. */
class DiagnosticStore(private val root: File, private val segmentBytes: Long = 512 * 1024L) {
    fun append(id: String, line: String, protectedIds: Set<String>) {
        require(id.matches(Regex("[A-Za-z0-9_-]+")))
        val dir = File(root, id).apply { mkdirs() }
        val current = File(dir, "events.log")
        if (current.length() + line.toByteArray(Charsets.UTF_8).size > segmentBytes) {
            val previous = File(dir, "events.previous.log")
            if (previous.exists()) File(dir, "TRUNCATED.txt").writeText(
                "Older events exceeded retention and were removed. This session is incomplete.\n"
            )
            previous.delete()
            check(!current.exists() || current.renameTo(previous)) { "Diagnostic rotation failed" }
        }
        current.appendText(line)
        val expired = root.listFiles()?.filter { it.isDirectory && it.name !in protectedIds }
            ?.sortedByDescending { it.name }?.drop(10).orEmpty()
        expired.forEach { it.deleteRecursively() }
    }

    fun sessions(): List<File> = root.listFiles()?.filter { it.isDirectory }
        ?.sortedBy { it.name }.orEmpty()

    fun recent(id: String, maxChars: Int = 18_000): String {
        require(id.matches(Regex("[A-Za-z0-9_-]+")))
        val dir = File(root, id)
        val content = listOf("events.previous.log", "events.log").joinToString("") {
            File(dir, it).takeIf { f -> f.isFile }?.readText().orEmpty()
        }
        return if (content.length > maxChars) "[仅显示末尾片段；请导出 ZIP 查看保留的完整事件]\n" + content.takeLast(maxChars) else content
    }
}
