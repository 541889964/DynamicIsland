package com.island.dynamic.ui
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.download.MultiPartDownloader
import com.island.dynamic.model.*
import com.island.dynamic.settings.SettingsStore
import com.island.dynamic.theme.*
import kotlinx.coroutines.CoroutineScope
import java.util.UUID
@Composable
fun DownloadScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("") }
    var threadCount by remember { mutableStateOf(SettingsStore.downloadThreads) }
    val tasks by remember { derivedStateOf { DownloadStateHolder.tasks.toList() } }
    val totalSpeed by remember { derivedStateOf { DownloadStateHolder.totalSpeed } }
    val activeCount by remember { derivedStateOf { tasks.count { it.status == DownloadStatus.DOWNLOADING } } }
    Column(modifier.fillMaxSize()) {
        GlassSurface(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM), Tokens.RadiusL, Tokens.NeonBlue) {
            Row(Modifier.fillMaxWidth().padding(Tokens.SpaceM), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("总速度", color = Tokens.Ink300, fontSize = 11.sp)
                    Text(formatSpeed(totalSpeed), color = Tokens.NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) {
                    Text("活动任务", color = Tokens.Ink300, fontSize = 11.sp)
                    Text("$activeCount", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) } } }
        Spacer(Modifier.height(Tokens.SpaceM))
        Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
            GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusXL, Tokens.NeonGreen) {
                OutlinedTextField(value = urlInput, onValueChange = { urlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("粘贴下载链接 (http/https)", color = Tokens.Ink300) },
                    leadingIcon = { Icon(Icons.Default.Link, null, tint = Tokens.Ink300) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Tokens.NeonGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (urlInput.isNotBlank()) { startDownload(context, urlInput, threadCount, scope); urlInput = "" } })) } }
        Spacer(Modifier.height(Tokens.SpaceS))
        Column(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("下载线程数", color = Tokens.Ink300, fontSize = 12.sp); Spacer(Modifier.weight(1f))
                Text("$threadCount 进程", color = Tokens.NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Slider(value = threadCount.toFloat(), onValueChange = { threadCount = it.toInt() }, valueRange = 1f..32f, steps = 30,
                modifier = Modifier.fillMaxWidth().height(28.dp), colors = SliderDefaults.colors(thumbColor = Tokens.NeonBlue, activeTrackColor = Tokens.NeonBlue))
            SettingsStore.setDownloadThreads(context, threadCount) }
        Spacer(Modifier.height(Tokens.SpaceS))
        Row(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM), horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceS)) {
            QuickDownloadBtn("测试大文件", "https://speed.cloudflare.com/__down?bytes=104857600", context, threadCount, scope)
            QuickDownloadBtn("Android SDK", "https://dl.google.com/android/repository/platform-tools-latest-linux.zip", context, threadCount, scope) }
        Spacer(Modifier.height(Tokens.SpaceM))
        if (tasks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, null, tint = Tokens.Ink300, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(Tokens.SpaceM))
                    Text("粘贴链接开始下载", color = Tokens.Ink300, fontSize = 14.sp)
                    Text("支持 1-32 进程并行分块", color = Tokens.Ink300, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) } }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = Tokens.SpaceM)) {
                items(tasks, key = { it.id }) { task -> DownloadTaskItem(task); Spacer(Modifier.height(Tokens.SpaceS)) } } } }
}
@Composable
private fun QuickDownloadBtn(label: String, url: String, context: Context, threads: Int, scope: CoroutineScope) {
    Box(Modifier.clip(RoundedCornerShape(Tokens.RadiusPill)).background(Tokens.GlassWhite10)
        .clickable { startDownload(context, url, threads, scope) }
        .padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS)) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium) } }
@Composable
private fun DownloadTaskItem(task: DownloadTask) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusM,
        when (task.status) { DownloadStatus.COMPLETED -> Tokens.NeonGreen; DownloadStatus.FAILED -> Tokens.NeonRed
            DownloadStatus.DOWNLOADING -> Tokens.NeonBlue; else -> Tokens.Ink300 }) {
        Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(when (task.status) { DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                    DownloadStatus.FAILED -> Icons.Default.Warning; DownloadStatus.PAUSED -> Icons.Default.Pause
                    DownloadStatus.DOWNLOADING -> Icons.Default.Download; else -> Icons.Default.Schedule }, null,
                    tint = when (task.status) { DownloadStatus.COMPLETED -> Tokens.NeonGreen; DownloadStatus.FAILED -> Tokens.NeonRed
                        DownloadStatus.DOWNLOADING -> Tokens.NeonBlue; else -> Tokens.Ink300 }, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Tokens.SpaceS))
                Column(Modifier.weight(1f)) {
                    Text(task.fileName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${task.threads}进程 · ", color = Tokens.Ink300, fontSize = 10.sp)
                        when (task.status) {
                            DownloadStatus.DOWNLOADING -> Text("↓ ${formatSpeed(task.speed)}", color = Tokens.NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            DownloadStatus.COMPLETED -> Text("已完成 · ${formatBytes(task.totalSize)}", color = Tokens.NeonGreen, fontSize = 10.sp)
                            DownloadStatus.FAILED -> Text(task.errorMessage.ifBlank { "下载失败" }, color = Tokens.NeonRed, fontSize = 10.sp)
                            DownloadStatus.PAUSED -> Text("已暂停", color = Tokens.NeonAmber, fontSize = 10.sp)
                            else -> Text("等待中", color = Tokens.Ink300, fontSize = 10.sp) } } }
                when (task.status) {
                    DownloadStatus.DOWNLOADING -> IconButton(onClick = { MultiPartDownloader.pause(task.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Pause, "暂停", tint = Color.White, modifier = Modifier.size(18.dp)) }
                    DownloadStatus.PAUSED, DownloadStatus.FAILED -> IconButton(onClick = {
                        DownloadStateHolder.updateTask(task.id) { it.copy(status = DownloadStatus.PENDING, downloadedSize = 0, errorMessage = "") }
                        MultiPartDownloader.download(context, task, scope, onProgress = { }, onComplete = { _, _ -> })
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, "继续", tint = Tokens.NeonGreen, modifier = Modifier.size(18.dp)) }
                    else -> {} }
                IconButton(onClick = { DownloadStateHolder.removeTask(task.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "移除", tint = Tokens.Ink300, modifier = Modifier.size(16.dp)) } }
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
                Spacer(Modifier.height(Tokens.SpaceS))
                LinearProgressIndicator(progress = task.progress, modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = Tokens.NeonBlue, trackColor = Tokens.Ink500)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatBytes(task.downloadedSize) + " / " + formatBytes(task.totalSize), color = Tokens.Ink300, fontSize = 10.sp)
                    Text("${(task.progress * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } } } }
private fun startDownload(context: Context, url: String, threads: Int, scope: CoroutineScope) {
    val fileName = url.substringAfterLast('/').substringBefore('?').ifBlank { "download_${System.currentTimeMillis()}" }
    val task = DownloadTask(id = UUID.randomUUID().toString(), url = url, fileName = fileName, threads = threads)
    DownloadStateHolder.addTask(task); DownloadStateHolder.currentTaskId = task.id
    IslandStateHolder.mode = IslandMode.DOWNLOADING
    MultiPartDownloader.download(context, task, scope, onProgress = { }, onComplete = { _, _ -> IslandStateHolder.mode = IslandMode.IDLE }) }
fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format("%.2f GB", bytes / 1024f / 1024f / 1024f)
    bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
    else -> "$bytes B" }
