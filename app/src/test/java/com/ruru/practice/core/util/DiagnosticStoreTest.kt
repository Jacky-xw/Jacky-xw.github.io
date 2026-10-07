package com.ruru.practice.core.util

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class DiagnosticStoreTest {
    private fun inTemp(test: (File) -> Unit) {
        val root = Files.createTempDirectory("diagnostic-test").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }
    @Test fun isolatesSessionsAndPreservesEvents() = inTemp { root ->
        val store = DiagnosticStore(root)
        store.append("100_meditation_a", "begin\nend\n", emptySet())
        store.append("101_walking_b", "walking\n", emptySet())
        assertEquals("begin\nend\n", store.recent("100_meditation_a"))
        assertEquals("walking\n", store.recent("101_walking_b"))
    }
    @Test fun rotationMarksActualLoss() = inTemp { root ->
        val store = DiagnosticStore(root, 5)
        store.append("100_meditation", "12345", emptySet())
        store.append("100_meditation", "67890", emptySet())
        assertEquals("1234567890", store.recent("100_meditation"))
        store.append("100_meditation", "abcde", emptySet())
        assertEquals("67890abcde", store.recent("100_meditation"))
        assertTrue(File(root, "100_meditation/TRUNCATED.txt").isFile)
    }
    @Test fun keepsTenInactivePlusProtectedSessions() = inTemp { root ->
        val store = DiagnosticStore(root)
        for (i in 100..114) store.append("${i}_meditation", "entry", setOf("100_meditation"))
        assertEquals(11, store.sessions().size)
        assertTrue(File(root, "100_meditation").exists())
        assertFalse(File(root, "101_meditation").exists())
    }
    @Test fun clipboardTailIsExplicitlyIncomplete() = inTemp { root ->
        val store = DiagnosticStore(root)
        store.append("100_meditation", "0123456789", emptySet())
        val tail = store.recent("100_meditation", 3)
        assertTrue(tail.contains("ZIP"))
        assertTrue(tail.endsWith("789"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsTraversal() = inTemp { root ->
        DiagnosticStore(root).append("../outside", "bad", emptySet())
    }
}
