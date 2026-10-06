package com.ruru.practice.feature.meditation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.ruru.practice.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TimerDiagnosticsButton() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("复制计时诊断（${BuildConfig.VERSION_NAME}）") }
    TextButton(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try {
                val report = withContext(Dispatchers.IO) { TimerDiagnostics.snapshot(context) }
                clipboard.setText(AnnotatedString(report))
                status = "诊断已复制，可粘贴反馈"
            } catch (_: Exception) {
                status = "复制失败，请重试"
            } finally {
                busy = false
            }
        }
    }) { Text(status) }
}
