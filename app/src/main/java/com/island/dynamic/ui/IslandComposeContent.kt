package com.island.dynamic.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    val targetWidth = when {
        hidden -> 6.dp
        mode == IslandMode.IDLE -> if (isMusic) 180.dp else 120.dp
        mode == IslandMode.MUSIC_CONTROL -> 360.dp
        else -> 340.dp
    }
    val targetHeight = when {
        hidden -> 6.dp
        mode == IslandMode.IDLE -> 36.dp
        mode == IslandMode.MUSIC_CONTROL -> 96.dp
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
        val style = StyleHolder.current
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
                        val newDrag = (dragOffset + dragAmount.x)
                            .coerceIn(-maxOffsetPx - offsetX.value, maxOffsetPx - offsetX.value)
                        dragOffset = newDrag
                    })
            }
            .pointerInput(hidden) {
                var accY = 0f
                detectDragGestures(
                    onDragStart = { accY = 0f },
                    onDragEnd = {
                        if (accY < -80f) {
                            hidden = true
                            scope.launch { delay(600L); offsetX.snapTo(0f); snappedLeft = false }
                        }
                        accY = 0f
                    },
                    onDrag = { change, drag -> change.consume(); accY += drag.y })
            }
            .pointerInput(hidden, mode) {
                detectTapGestures(onTap = {
                    if (hidden) hidden = false
                    else if (IslandStateHolder.appMode == AppMode.MUSIC)
                        IslandStateHolder.mode =
                            if (mode == IslandMode.MUSIC_CONTROL) IslandMode.IDLE
                            else IslandMode.MUSIC_CONTROL
                })
            }) {
            when (style) {
                UiStyle.GLASS -> {
                    Box(Modifier.matchParentSize().blur(28.dp).clip(RoundedCornerShape(radius))
                        .background(Tokens.NeonGreen.copy(alpha = 0.22f)))
                    Box(Modifier.matchParentSize()
                        .shadow(12.dp, RoundedCornerShape(radius),
                            ambientColor = Tokens.NeonGreen.copy(alpha = 0.4f),
                            spotColor = Tokens.NeonGreen.copy(alpha = 0.6f))
                        .clip(RoundedCornerShape(radius))
                        .background(Brush.linearGradient(
                            0f to Color(0xFF0F172A).copy(alpha = 0.92f),
                            1f to Color(0xFF000000).copy(alpha = 0.88f)))
                        .border(1.dp, Brush.linearGradient(
                            0f to Tokens.GlassBorderStrong,
                            0.5f to Tokens.GlassBorder, 1f to Color.Transparent),
                            RoundedCornerShape(radius)))
                    Box(Modifier.matchParentSize().clip(RoundedCornerShape(radius))
                        .background(Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.12f), 0.3f to Color.Transparent)))
                }
                UiStyle.IPHONE -> Box(Modifier.matchParentSize()
                    .shadow(8.dp, RoundedCornerShape(radius))
                    .clip(RoundedCornerShape(radius)).background(Color.Black))
                UiStyle.CARD -> Box(Modifier.matchParentSize()
                    .shadow(16.dp, RoundedCornerShape(radius))
                    .clip(RoundedCornerShape(radius)).background(Tokens.Ink600))
            }
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(radius))
                .graphicsLayer { alpha = contentAlpha }) {
                AnimatedContent(targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(Tokens.DurBase)) + scaleIn(initialScale = 0.9f))
                            .togetherWith(fadeOut(tween(Tokens.DurFast)))
                    }, label = "content") { m ->
                    when (m) {
                        IslandMode.IDLE -> if (isMusic) MusicIdleContent() else LifeIdleContent()
                        IslandMode.CHARGING -> ChargingContent(IslandStateHolder.batteryLevel)
                        IslandMode.NOTIFICATION -> NotifContent(
                            IslandStateHolder.title, IslandStateHolder.subtitle)
                        IslandMode.RECORDING -> RecordContent()
                        IslandMode.WEATHER -> WeatherContent()
                        IslandMode.CALENDAR -> CalendarContent()
                        IslandMode.MUSIC_CONTROL -> MusicControlContent()
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
private fun LifeIdleContent() {
    val time by remember { derivedStateOf { IslandStateHolder.currentTime } }
    val battery by remember { derivedStateOf { IslandStateHolder.batteryLevel } }
    val charging by remember { derivedStateOf { IslandStateHolder.isCharging } }
    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(time, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        BatteryIcon(battery, charging, 18.dp)
    }
}

@Composable
private fun MusicIdleContent() {
    val playing by remember { derivedStateOf { MusicStateHolder.isPlaying } }
    val song by remember { derivedStateOf { MusicStateHolder.currentSong } }
    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        MusicWaveBar(playing)
        Spacer(Modifier.width(8.dp))
        Text(song?.name ?: "音乐模式", color = Color.White, fontSize = 11.sp,
            maxLines = 1, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MusicWaveBar(playing: Boolean) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val h1 by infinite.animateFloat(if (playing) 6f else 3f, if (playing) 16f else 3f,
        infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "h1")
    val h2 by infinite.animateFloat(if (playing) 14f else 3f, if (playing) 6f else 3f,
        infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "h2")
    val h3 by infinite.animateFloat(if (playing) 8f else 3f, if (playing) 14f else 3f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "h3")
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(h1, h2, h3).forEach { h ->
            Box(Modifier.width(3.dp).height(h.dp)
                .clip(RoundedCornerShape(1.5.dp)).background(Tokens.NeonGreen))
        }
    }
}

@Composable
private fun ChargingContent(level: Int) {
    val infinite = rememberInfiniteTransition(label = "charge")
    val pulse by infinite.animateFloat(0.85f, 1.12f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse")
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("⚡ 正在充电", color = Tokens.NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("$level%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.size(48.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator(level / 100f, color = Tokens.NeonGreen,
                trackColor = Tokens.Ink500, strokeWidth = 4.dp)
            Text("$level", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NotifContent(title: String, sub: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center) {
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(sub, color = Tokens.Ink300, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun RecordContent() {
    val infinite = rememberInfiniteTransition(label = "rec")
    val alpha by infinite.animateFloat(0.3f, 1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "blink")
    val time by remember { derivedStateOf { IslandStateHolder.currentTime } }
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
            .background(Color.Red.copy(alpha = alpha)))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("屏幕录制中", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(time, color = Tokens.Ink300, fontSize = 11.sp)
        }
    }
}

@Composable
private fun WeatherContent() {
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(IslandStateHolder.weatherCity, color = Tokens.NeonBlue, fontSize = 11.sp)
            Text("${IslandStateHolder.weatherTemp} ${IslandStateHolder.weatherDesc}",
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text("☁️", fontSize = 28.sp)
    }
}

@Composable
private fun CalendarContent() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center) {
        Text("📅 ${IslandStateHolder.calendarDay}", color = Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(IslandStateHolder.currentTime, color = Tokens.Ink300, fontSize = 11.sp)
    }
}

@Composable
private fun MusicControlContent() {
    val song by remember { derivedStateOf { MusicStateHolder.currentSong } }
    val playing by remember { derivedStateOf { MusicStateHolder.isPlaying } }
    val mode by remember { derivedStateOf { MusicStateHolder.playMode } }
    val pos by remember { derivedStateOf { MusicStateHolder.currentPosition } }
    val dur by remember { derivedStateOf { MusicStateHolder.duration } }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(song?.name ?: "未播放", color = Color.White, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(song?.artist ?: "", color = Tokens.Ink300, fontSize = 10.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = if (dur > 0) pos.toFloat() / dur else 0f,
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Tokens.NeonGreen, trackColor = Tokens.Ink500)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            val modeIcon = when (mode) {
                PlayMode.SEQUENTIAL -> "→"; PlayMode.LIST_LOOP -> "🔁"
                PlayMode.SINGLE_LOOP -> "🔂"; PlayMode.SHUFFLE -> "🔀"
            }
            Text(modeIcon, fontSize = 14.sp, modifier = Modifier.tap { MusicController.cyclePlayMode() })
            Text("⏮", fontSize = 16.sp, color = Color.White, modifier = Modifier.tap { MusicController.playPrev() })
            Text(if (playing) "⏸" else "▶️", fontSize = 18.sp, modifier = Modifier.tap { MusicController.togglePlayPause() })
            Text("⏭", fontSize = 16.sp, color = Color.White, modifier = Modifier.tap { MusicController.playNext() })
        }
    }
}

fun Modifier.tap(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }

@Composable
fun BatteryIcon(level: Int, charging: Boolean, size: androidx.compose.ui.unit.Dp) {
    val fillColor = when {
        charging -> Tokens.NeonGreen; level > 60 -> Tokens.NeonGreen
        level > 20 -> Tokens.NeonAmber; else -> Color(0xFFF87171)
    }
    Box(Modifier.size(size, size * 0.5f).clip(RoundedCornerShape(3.dp)).background(Tokens.Ink500)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(level / 100f).background(fillColor))
        if (charging) Text("⚡", fontSize = 8.sp, modifier = Modifier.align(Alignment.Center))
    }
}
