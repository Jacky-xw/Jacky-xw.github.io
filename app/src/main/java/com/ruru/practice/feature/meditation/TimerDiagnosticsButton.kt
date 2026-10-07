package com.ruru.practice.feature.meditation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.ruru.practice.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TimerDiagnosticsButton() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Cannot open destination")
                        output.use { TimerDiagnostics.export(context.applicationContext, it) }
                    }
                    status = "诊断 ZIP 已保存，请上传此文件；无需粘贴长日志"
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { status = "导出失败，文件可能不完整，请重试" }
                finally { busy = false }
            }
        }
    }
    Column {
        TextButton(enabled = !busy, onClick = {
            save.launch("禅修计时诊断-${BuildConfig.VERSION_NAME}-${System.currentTimeMillis()}.zip")
        }) { Text("导出计时诊断 ZIP（${BuildConfig.VERSION_NAME}）") }
        TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    val report = withContext(Dispatchers.IO) { TimerDiagnostics.snapshot(context.applicationContext) }
                    clipboard.setText(AnnotatedString(report))
                    status = "已复制最新一座的末尾日志；完整反馈请导出 ZIP"
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { status = "复制失败，请重试" }
                finally { busy = false }
            }
        }) { Text("复制最近计时摘要") }
        Text("仅导出本地技术日志，不含修习笔记或音频。可能含设备型号、系统状态和事件时间，分享前请审阅。")
        if (status.isNotEmpty()) Text(status)
    }
}
