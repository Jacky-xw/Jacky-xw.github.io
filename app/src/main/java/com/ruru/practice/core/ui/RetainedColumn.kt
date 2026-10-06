package com.ruru.practice.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Form pages keep their controls composed while scrolling. Not for unbounded feeds. */
@Composable
fun RetainedColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable RetainedColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = verticalArrangement
    ) { RetainedColumnScope.content() }
}

@Suppress("UNUSED_PARAMETER")
object RetainedColumnScope {
    @Composable
    fun item(key: Any? = null, contentType: Any? = null, content: @Composable () -> Unit) {
        androidx.compose.runtime.key(key) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }

    @Composable
    fun <T> items(
        items: List<T>,
        key: ((T) -> Any)? = null,
        contentType: (T) -> Any? = { null },
        itemContent: @Composable (T) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            androidx.compose.runtime.key(key?.invoke(item) ?: index) { itemContent(item) }
        }
    }
}
