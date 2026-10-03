package com.island.dynamic.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.model.*
import com.island.dynamic.music.MusicController
import com.island.dynamic.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IslandComposeContent() {
    val scope = rememberCoroutineScope()
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var hidden by remember { mutableStateOf(false) }
    var snappedLeft by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(snappedLeft) {
        if (snappedLeft) {
            delay(5000L)
            scope.launch {
                offsetX.animateTo(0f, spring(Tokens.SpringDamping, Tokens.SpringStiffness))
                snappedLeft = false
            }
        }
    }

    val appMode = IslandStateHolder.appMode
    val mode = IslandStateHolder.mode
    val isMusic = appMode == AppMode.MUSIC
    val isDownload = appMode == AppMode.DOWNLOAD

    val targetWidth = when {
        hidden -> 6.dp
        mode == IslandMode.IDLE -> if (isMusic || isDownload) 180.dp else 120.dp
        else -> 340.dp
    }
    val targetHeight = when {
        hidden -> 6.dp
        mode == IslandMode.IDLE -> 36.dp
        else -> 80.dp
    }
    val targetRadius = when {
        hidden -> 3.dp
        mode == IslandMode.IDLE -> 18.dp
        else -> 28.dp
    }
    val springSpec = spring<Dp>(Tokens.SpringDamping, Tokens.SpringStiffness, 0.5.dp)
    val width by animateDpAsState(targetWidth, springSpec, label = "w")
    val height by animateDpAsState(targetHeight, springSpec, label = "h")
    val radius by animateDpAsState(targetRadius, springSpec, label = "r")
    val contentAlpha by animateFloatAsState(
        if (hidden) 0f else 1f, tween(Tokens.DurFast), label = "alpha")
    val maxOffsetPx = with(density) {
        (screenWidthPx / 2f - width.toPx() / 2f - 12.dp.toPx()).coerceAtLeast(0f)
    }

    Box(Modifier.fillMaxWidth().padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter) {
        Box(Modifier
            .offset { IntOffset((offsetX.value + dragOffset).roundToInt(), 0) }
            .width(width).height(height)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val finalX = offsetX.value + dragOffset
                        dragOffset = 0f
                        scope.launch {
                            val threshold = maxOffsetPx * 0.35f
                            when {
                                finalX < -threshold -> {
                                    offsetX.animateTo(-maxOffsetPx,
                                        spring(Tokens.SpringDamping, Tokens.SpringStiffness))
                                    snappedLeft = true
                                }
                                finalX > threshold -> {
                                    offsetX.animateTo(maxOffsetPx,
                                        spring(Tokens.SpringDamping, Tokens.SpringStiffness))
                                    snappedLeft = false
                                }
                                else -> {
                                    offsetX.animateTo(0f,
                                        spring(Tokens.SpringDampingSoft, Tokens.SpringStiffnessSoft))
                                    snappedLeft = false
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount.x)
                            .coerceIn(-maxOffsetPx - offsetX.value,
                                      maxOffsetPx - offsetX.value)
                    })
            }
            .pointerInput(hidden) {
                var accY = 0f
                detectDragGestures(
                    onDragStart = { accY = 0f },
                    onDragEnd = {
                        if (accY < -80f) {
                            hidden = true
                            scope.launch {
                                delay(600L)
                                offsetX.snapTo(0f)
                                snappedLeft = false
                            }
                        }
                        accY = 0f
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        accY += drag.y
                    })
            }
            .pointerInput(hidden, mode) {
                detectTapGestures(onTap = {
                    if (hidden) hidden = false
                    else if (IslandStateHolder.appMode == AppMode.MUSIC)
                        IslandStateHolder.mode =
                            if (mode == IslandMode.MUSIC_CONTROL) IslandMode.IDLE
                            else IslandMode.MUSIC_CONTROL
                })
            }
        ) {
            // 背景
            Box(Modifier.matchParentSize().blur(24.dp).clip(RoundedCornerShape(radius))
                .background(Tokens.NeonGreen.copy(alpha = 0.22f)))
            Box(Modifier.matchParentSize()
                .clip(RoundedCornerShape(radius))
                .background(Brush.linearGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF000000))))
                .border(1.dp, Tokens.GlassBorder, RoundedCornerShape(radius)))

            // 内容
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(radius))
                .graphicsLayer { alpha = contentAlpha }) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(240)) + scaleIn(initialScale = 0.9f))
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "content"
                ) { m ->
                    when (m) {
                        IslandMode.IDLE ->
                            if (isMusic) MusicIdle()
                            else if (isDownload) DownloadIdle()
                            else LifeIdle()
                        IslandMode.CHARGING -> ChargingContent()
                        IslandMode.NOTIFICATION -> NotifContent()
                        IslandMode.RECORDING -> RecordContent()
                        IslandMode.WEATHER -> WeatherContent()
                        IslandMode.CALENDAR -> CalendarContent()
                        IslandMode.MUSIC_CONTROL -> MusicControl()
                        IslandMode.DOWNLOADING -> DownloadingContent()
                    }
                }
            }
        }
        if (hidden) {
            Box(Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(18.dp).clip(RoundedCornerShape(9.dp))
                .background(Tokens.GlassWhite15)
                .border(1.dp, Tokens.GlassBorder, RoundedCornerShape(9.dp))
                .pointerInput(Unit) { detectTapGestures { hidden = false } })
        }
    }
}

@Composable
private fun LifeIdle() {
    val time by remember { derivedStateOf { IslandStateHolder.currentTime } }
    val battery by remember { derivedStateOf { IslandStateHolder.batteryLevel } }
    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(time, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text("$battery%", color = Tokens.NeonGreen, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MusicIdle() {
    val song by remember { derivedStateOf { MusicStateHolder.currentSong } }
    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("♪", color = Tokens.NeonGreen, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(song?.name ?: "音乐模式", color = Color.White, fontSize = 11.sp,
            maxLines = 1, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DownloadIdle() {
    val speed by remember { derivedStateOf { DownloadStateHolder.totalSpeed } }
    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("↓", color = Tokens.NeonBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(formatSpeed(speed), color = Color.White, fontSize = 11.sp,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DownloadingContent() {
    val task = DownloadStateHolder.tasks.firstOrNull {
        it.status == DownloadStatus.DOWNLOADING
    }
    if (task == null) { DownloadIdle(); return }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(task.fileName, color = Color.White, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = task.progress,
            modifier = Modifier.fillMaxWidth().height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Tokens.NeonBlue, trackColor = Tokens.Ink500)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("↓ ${formatSpeed(task.speed)}", color = Tokens.NeonGreen,
                fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("${(task.progress * 100).toInt()}%", color = Color.White,
                fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String = when {
    bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / 1024f / 1024f)
    bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024f)
    else -> "$bytesPerSec B/s"
}

@Composable
private fun ChargingContent() {
    val level by remember { derivedStateOf { IslandStateHolder.batteryLevel } }
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("⚡ 正在充电", color = Tokens.NeonGreen, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text("$level%", color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold)
        }
        Text("$level%", color = Tokens.NeonGreen, fontSize = 22.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotifContent() {
    val title by remember { derivedStateOf { IslandStateHolder.title } }
    val sub by remember { derivedStateOf { IslandStateHolder.subtitle } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center) {
        Text(title, color = Color.White, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(sub, color = Tokens.Ink300, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun RecordContent() {
    val time by remember { derivedStateOf { IslandStateHolder.currentTime } }
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("●", color = Color.Red, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("屏幕录制中", color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text(time, color = Tokens.Ink300, fontSize = 11.sp)
        }
    }
}

@Composable
private fun WeatherContent() {
    val city by remember { derivedStateOf { IslandStateHolder.weatherCity } }
    val temp by remember { derivedStateOf { IslandStateHolder.weatherTemp } }
    val desc by remember { derivedStateOf { IslandStateHolder.weatherDesc } }
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(city, color = Tokens.NeonBlue, fontSize = 11.sp)
            Text("$temp $desc", color = Color.White, fontSize = 15.sp,
                fontWeight = FontWeight.Bold)
        }
        Text("☁", fontSize = 28.sp, color = Tokens.NeonBlue)
    }
}

@Composable
private fun CalendarContent() {
    val day by remember { derivedStateOf { IslandStateHolder.calendarDay } }
    val time by remember { derivedStateOf { IslandStateHolder.currentTime } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center) {
        Text("📅 $day", color = Color.White, fontSize = 13.sp,
            fontWeight = FontWeight.Medium)
        Text(time, color = Tokens.Ink300, fontSize = 11.sp)
    }
}

@Composable
private fun MusicControl() {
    val song by remember { derivedStateOf { MusicStateHolder.currentSong } }
    val playing by remember { derivedStateOf { MusicStateHolder.isPlaying } }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(song?.name ?: "未播放", color = Color.White, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(song?.artist ?: "", color = Tokens.Ink300, fontSize = 10.sp, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Text("⏮", fontSize = 16.sp, color = Color.White,
                modifier = Modifier.tap { MusicController.playPrev() })
            Text(if (playing) "⏸" else "▶", fontSize = 18.sp, color = Color.White,
                modifier = Modifier.tap { MusicController.togglePlayPause() })
            Text("⏭", fontSize = 16.sp, color = Color.White,
                modifier = Modifier.tap { MusicController.playNext() })
        }
    }
}

fun Modifier.tap(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }
