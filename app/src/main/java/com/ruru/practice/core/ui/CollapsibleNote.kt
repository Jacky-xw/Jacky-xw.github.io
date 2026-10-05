package com.ruru.practice.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Long note display for practice history lists.
 * Default collapsed to [collapsedMaxLines]; expand/collapse without string truncation.
 */
@Composable
fun CollapsibleNote(
    value: String,
    label: String? = null,
    secondary: Boolean = false,
    collapsedMaxLines: Int = 3,
    expandKey: String = value.take(32)
) {
    val body = value.trim()
    if (body.isEmpty()) return
    var expanded by rememberSaveable(expandKey) { mutableStateOf(false) }
    // Heuristic: more than ~3 short lines worth of text can expand.
    val canExpand = body.length > 90 || body.count { it == '\n' } >= collapsedMaxLines

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = if (label.isNullOrBlank()) body else "$label：\n$body",
            color = if (secondary) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        if (canExpand) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (expanded) "收起" else "展开")
            }
        }
    }
}
