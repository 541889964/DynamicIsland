package com.island.dynamic.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.download.MultiPartDownloader
import com.island.dynamic.model.*
import com.island.dynamic.theme.*
import java.util.UUID

@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("") }
    var threadCount by remember { mutableStateOf(16) }
    val tasks by remember { derivedStateOf { DownloadStateHolder.tasks.toList() } }
    val totalSpeed by remember { derivedStateOf { DownloadStateHolder.totalSpeed } }

    Column(Modifier.fillMaxSize()) {
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM),
            Tokens.RadiusL, Tokens.NeonBlue
        ) {
            Row(
                Modifier.fillMaxWidth().padding(Tokens.SpaceM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("总速度", color = Tokens.Ink300, fontSize = 11.sp)
                    Text(formatSpeed(totalSpeed), color = Tokens.NeonGreen,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text("${tasks.count { it.status == DownloadStatus.DOWNLOADING }}",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(Tokens.SpaceM))

        OutlinedTextField(
            value = urlInput, onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM),
            placeholder = { Text("粘贴下载链接", color = Tokens.Ink300) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White)
        )

        Spacer(Modifier.height(Tokens.SpaceS))

        Column(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
            Text("线程: $threadCount", color = Tokens.Ink300, fontSize = 12.sp)
            Slider(
                value = threadCount.toFloat(),
                onValueChange = { threadCount = it.toInt() },
                valueRange = 1f..32f, steps = 30)
        }

        Spacer(Modifier.height(Tokens.SpaceS))

        Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
            GlassButton("开始下载", Modifier.fillMaxWidth(),
                accent = Tokens.NeonBlue) {
                if (urlInput.isNotBlank()) {
                    startDownload(context, urlInput, threadCount, scope)
                    urlInput = ""
                }
            }
        }

        Spacer(Modifier.height(Tokens.SpaceM))

        if (tasks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("粘贴链接开始下载", color = Tokens.Ink300, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Tokens.SpaceM)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(task)
                    Spacer(Modifier.height(Tokens.SpaceS))
                }
            }
        }
    }
}

@Composable
private fun TaskItem(task: DownloadTask) {
    GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusM,
        when (task.status) {
            DownloadStatus.COMPLETED -> Tokens.NeonGreen
            DownloadStatus.FAILED -> Tokens.NeonRed
            DownloadStatus.DOWNLOADING -> Tokens.NeonBlue
            else -> Tokens.Ink300
        }) {
        Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
            Text(task.fileName, color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            when (task.status) {
                DownloadStatus.DOWNLOADING -> Text("↓ ${formatSpeed(task.speed)}",
                    color = Tokens.NeonGreen, fontSize = 11.sp)
                DownloadStatus.COMPLETED -> Text("已完成 · ${formatBytes(task.totalSize)}",
                    color = Tokens.NeonGreen, fontSize = 11.sp)
                DownloadStatus.FAILED -> Text(task.errorMessage.ifBlank { "失败" },
                    color = Tokens.NeonRed, fontSize = 11.sp)
                else -> Text("等待中", color = Tokens.Ink300, fontSize = 11.sp)
            }
            if (task.status == DownloadStatus.DOWNLOADING) {
                Spacer(Modifier.height(Tokens.SpaceS))
                LinearProgressIndicator(
                    progress = task.progress,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Tokens.NeonBlue, trackColor = Tokens.Ink500)
            }
        }
    }
}

private fun startDownload(context: Context, url: String, threads: Int,
                          scope: kotlinx.coroutines.CoroutineScope) {
    val fileName = url.substringAfterLast('/').substringBefore('?')
        .ifBlank { "download_${System.currentTimeMillis()}" }
    val task = DownloadTask(
        id = UUID.randomUUID().toString(),
        url = url, fileName = fileName, threads = threads)
    DownloadStateHolder.addTask(task)
    MultiPartDownloader.download(context, task, scope,
        onProgress = { }, onComplete = { _, _ -> })
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format("%.2f GB", bytes / 1024f / 1024f / 1024f)
    bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}
