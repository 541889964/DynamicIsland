package com.island.dynamic

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.island.dynamic.model.*
import com.island.dynamic.music.MusicController
import com.island.dynamic.service.IslandService
import com.island.dynamic.service.MusicPlaybackService
import com.island.dynamic.service.ScreenRecordService
import com.island.dynamic.settings.SettingsStore
import com.island.dynamic.theme.*
import com.island.dynamic.ui.*

class MainActivity : ComponentActivity() {
    private val recordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startForegroundService(Intent(this, ScreenRecordService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data) })
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsStore.init(this)
        intent?.data?.let { uri ->
            if (uri.host == "music") {
                val kw = uri.getQueryParameter("keyword") ?: uri.getQueryParameter("q")
                if (!kw.isNullOrBlank()) {
                    IslandStateHolder.appMode = AppMode.MUSIC
                    startForegroundService(Intent(this, MusicPlaybackService::class.java))
                    MusicController.searchAndPlayFirst(kw, MusicStateHolder.searchLimit)
                }
            }
        }
        setContent {
            MaterialTheme {
                var showSplash by remember { mutableStateOf(true) }
                var showSettings by remember { mutableStateOf(false) }
                var showIntro by remember { mutableStateOf(false) }
                val styleKey = StyleHolder.current

                // 首次启动显示介绍弹窗
                LaunchedEffect(showSplash) {
                    if (!showSplash && !SettingsStore.introShown) {
                        kotlinx.coroutines.delay(400)
                        showIntro = true
                    }
                }

                Box(Modifier.fillMaxSize().background(Tokens.Ink900)) {
                    AnimatedContent(targetState = Triple(showSplash, showSettings, styleKey),
                        transitionSpec = {
                            (fadeIn(tween(500)) + scaleIn(initialScale = 0.98f))
                                .togetherWith(fadeOut(tween(400)) + scaleOut(targetScale = 1.02f))
                        }, label = "root") { (splash, settings, _) ->
                        when {
                            splash -> SplashScreen(onFinish = { showSplash = false })
                            settings -> SettingsScreen(
                                onBack = { showSettings = false },
                                onShowIntro = { showIntro = true })
                            else -> MainScreen(
                                onStartIsland = { startForegroundService(Intent(this, IslandService::class.java)) },
                                onToggleRecord = { toggleRecording() },
                                onOpenOverlayPermission = { openOverlayPermission() },
                                onStartMusicService = { startForegroundService(Intent(this, MusicPlaybackService::class.java)) },
                                onOpenSettings = { showSettings = true })
                        }
                    }

                    // 介绍弹窗浮在最上层
                    if (showIntro) {
                        IntroDialog(onClose = {
                            showIntro = false
                            SettingsStore.markIntroShown(this)
                        })
                    }
                }
            }
        }
    }
    private fun toggleRecording() {
        if (IslandStateHolder.isRecording) {
            startService(Intent(this, ScreenRecordService::class.java).apply { action = "STOP" })
        } else {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            recordLauncher.launch(mgr.createScreenCaptureIntent())
        }
    }
    private fun openOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")))
    }
}

@Composable
private fun MainScreen(
    onStartIsland: () -> Unit, onToggleRecord: () -> Unit,
    onOpenOverlayPermission: () -> Unit, onStartMusicService: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var appMode by remember { mutableStateOf(IslandStateHolder.appMode) }
    LaunchedEffect(IslandStateHolder.appMode) { appMode = IslandStateHolder.appMode }
    Box(Modifier.fillMaxSize().background(Tokens.Ink900)) {
        BackgroundAura()
        Scaffold(containerColor = Color.Transparent,
            topBar = { TopAppBar(title = {
                Text(if (appMode == AppMode.LIFE) "灵动岛 · 生活" else "灵动岛 · 音乐",
                    color = Color.White, fontWeight = FontWeight.Bold)
            }, actions = { IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "设置", tint = Color.White)
            }}, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
            bottomBar = {
                NavigationBar(containerColor = Color.Transparent) {
                    NavigationBarItem(selected = appMode == AppMode.LIFE,
                        onClick = { appMode = AppMode.LIFE; IslandStateHolder.appMode = AppMode.LIFE },
                        icon = { Icon(Icons.Default.Home, "生活") }, label = { Text("生活模式") })
                    NavigationBarItem(selected = appMode == AppMode.MUSIC,
                        onClick = { appMode = AppMode.MUSIC
                            IslandStateHolder.appMode = AppMode.MUSIC
                            onStartMusicService() },
                        icon = { Icon(Icons.Default.PlayArrow, "音乐") }, label = { Text("音乐模式") })
                }
            }) { padding ->
            AnimatedContent(targetState = appMode,
                transitionSpec = {
                    (fadeIn(tween(Tokens.DurSlow)) + slideInHorizontally { it / 3 })
                        .togetherWith(fadeOut(tween(Tokens.DurBase)) + slideOutHorizontally { -it / 3 })
                }, label = "mode") { m ->
                when (m) {
                    AppMode.LIFE -> LifeModeScreen(Modifier.padding(padding),
                        onStartIsland, onToggleRecord, onOpenOverlayPermission)
                    AppMode.MUSIC -> MusicModeScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun LifeModeScreen(modifier: Modifier, onStartIsland: () -> Unit,
    onToggleRecord: () -> Unit, onOpenOverlayPermission: () -> Unit) {
    Column(modifier.fillMaxSize().padding(Tokens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(Tokens.SpaceM)) {
        GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonGreen, enableBreathe = true) {
            Column(Modifier.fillMaxWidth().padding(Tokens.SpaceL)) {
                Text("欢迎使用灵动岛", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("通知 · 天气 · 日历 · 电量 · 录屏", color = Tokens.Ink300, fontSize = 13.sp)
            }
        }
        GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonBlue) {
            Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM),
                verticalArrangement = Arrangement.spacedBy(Tokens.SpaceS)) {
                ActionRow(Icons.Default.Settings, "授予悬浮窗权限", onOpenOverlayPermission)
                ActionRow(Icons.Default.Star, "启动灵动岛", onStartIsland)
                ActionRow(Icons.Default.Videocam,
                    if (IslandStateHolder.isRecording) "停止录屏" else "开始录屏", onToggleRecord)
            }
        }
        Text("显示模式", color = Tokens.Ink300, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceS),
            verticalArrangement = Arrangement.spacedBy(Tokens.SpaceS)) {
            ModeChip("时钟", IslandMode.IDLE); ModeChip("天气", IslandMode.WEATHER)
            ModeChip("日历", IslandMode.CALENDAR); ModeChip("通知", IslandMode.NOTIFICATION)
            ModeChip("充电", IslandMode.CHARGING); ModeChip("录屏", IslandMode.RECORDING)
        }
        Spacer(Modifier.weight(1f))
        GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL,
            if (IslandStateHolder.isCharging) Tokens.NeonGreen else Tokens.NeonBlue) {
            Row(Modifier.fillMaxWidth().padding(Tokens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("电池", color = Tokens.Ink300, fontSize = 12.sp)
                    Text("${IslandStateHolder.batteryLevel}%", color = Color.White,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                BatteryIcon(IslandStateHolder.batteryLevel, IslandStateHolder.isCharging, 48.dp)
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.RadiusM))
        .clickable(onClick = onClick).padding(Tokens.SpaceM),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(Tokens.RadiusS))
            .background(Tokens.GlassWhite10), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Tokens.SpaceM))
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Tokens.Ink300)
    }
}

@Composable
private fun ModeChip(label: String, target: IslandMode) {
    val selected = IslandStateHolder.mode == target
    Box(Modifier.clip(RoundedCornerShape(Tokens.RadiusPill))
        .background(if (selected) Tokens.NeonGreen.copy(alpha = 0.25f) else Tokens.GlassWhite10)
        .clickable { IslandStateHolder.mode = target }
        .padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS)) {
        Text(label, color = if (selected) Tokens.NeonGreen else Color.White,
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun MusicModeScreen(modifier: Modifier) {
    val context = LocalContext.current
    var keyword by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(SearchSource.ONLINE) }
    var limit by remember { mutableStateOf(20f) }
    val results by remember { derivedStateOf { MusicStateHolder.searchResults.toList() } }
    val locals by remember { derivedStateOf { MusicStateHolder.localMusic.toList() } }
    val isSearching by remember { derivedStateOf { MusicStateHolder.isSearching } }
    val isScanning by remember { derivedStateOf { MusicStateHolder.isScanningLocal } }
    val scanProgress by remember { derivedStateOf { MusicStateHolder.scanProgress } }
    val currentSong by remember { derivedStateOf { MusicStateHolder.currentSong } }
    val isPlaying by remember { derivedStateOf { MusicStateHolder.isPlaying } }
    val displayList = if (source == SearchSource.ONLINE) results
                      else locals.filter { keyword.isBlank() ||
                          it.name.contains(keyword, true) || it.artist.contains(keyword, true) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS),
            horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceS)) {
            SourceChip("在线", source == SearchSource.ONLINE) { source = SearchSource.ONLINE }
            SourceChip("本地", source == SearchSource.LOCAL) { source = SearchSource.LOCAL }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
            GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusXL, Tokens.NeonGreen) {
                OutlinedTextField(value = keyword, onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (source == SearchSource.ONLINE) "搜索歌曲、歌手…" else "过滤本地音乐…",
                        color = Tokens.Ink300) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Tokens.Ink300) },
                    trailingIcon = { if (isSearching) CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp, color = Tokens.NeonGreen) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        cursorColor = Tokens.NeonGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (source == SearchSource.ONLINE && keyword.isNotBlank())
                            MusicController.searchAndPlayFirst(keyword, limit.toInt())
                    }))
            }
        }
        if (source == SearchSource.ONLINE) {
            Column(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("搜索数量", color = Tokens.Ink300, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${limit.toInt()} 条", color = Tokens.NeonGreen, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                }
                Slider(value = limit, onValueChange = { limit = it },
                    valueRange = 5f..50f, steps = 8,
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Tokens.NeonGreen, activeTrackColor = Tokens.NeonGreen))
            }
        }
        if (source == SearchSource.LOCAL) {
            Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
                GlassButton(text = if (isScanning) "扫描中 ${(scanProgress * 100).toInt()}%"
                           else "扫描本地音乐 (${locals.size})",
                    modifier = Modifier.fillMaxWidth(), accent = Tokens.NeonBlue,
                    onClick = { if (!isScanning) MusicController.scanLocalMusic(context) })
            }
            if (isScanning) {
                LinearProgressIndicator(progress = scanProgress,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS)
                        .height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = Tokens.NeonBlue, trackColor = Tokens.Ink500)
            }
        }
        Spacer(Modifier.height(Tokens.SpaceS))
        if (displayList.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = Tokens.SpaceM)) {
                items(displayList, key = { "${it.isLocal}_${it.id}" }) { song ->
                    SongItem(song, isActive = currentSong?.id == song.id) {
                        if (song.isLocal) MusicController.playLocalSong(song, displayList)
                        else {
                            MusicStateHolder.playQueue.clear()
                            MusicStateHolder.playQueue.addAll(displayList)
                            MusicStateHolder.currentIndex = displayList.indexOf(song)
                            MusicStateHolder.currentSong = song
                            MusicController.playSong(song)
                        }
                    }
                    Spacer(Modifier.height(Tokens.SpaceS))
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (source == SearchSource.LOCAL && locals.isEmpty())
                        "点击上方按钮扫描本地音乐" else "搜索你想听的音乐",
                    color = Tokens.Ink300, fontSize = 14.sp)
            }
        }
        currentSong?.let { MiniPlayerBar(it, isPlaying) }
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(Tokens.RadiusPill))
        .background(if (selected) Tokens.NeonGreen.copy(alpha = 0.25f) else Tokens.GlassWhite10)
        .clickable(onClick = onClick)
        .padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS)) {
        Text(label, color = if (selected) Tokens.NeonGreen else Color.White,
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun SongItem(song: Song, isActive: Boolean, onClick: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth().clickable(onClick = onClick),
        Tokens.RadiusM, if (isActive) Tokens.NeonGreen else Tokens.NeonBlue) {
        Row(Modifier.fillMaxWidth().padding(Tokens.SpaceM),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.name, color = if (isActive) Tokens.NeonGreen else Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.isLocal) Text("本地 · ", fontSize = 10.sp, color = Tokens.NeonBlue)
                    Text(song.artist, color = Tokens.Ink300, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (isActive) Icon(Icons.Default.PlayArrow, null, tint = Tokens.NeonGreen)
        }
    }
}

@Composable
private fun MiniPlayerBar(song: Song, isPlaying: Boolean) {
    val mode by remember { derivedStateOf { MusicStateHolder.playMode } }
    val pos by remember { derivedStateOf { MusicStateHolder.currentPosition } }
    val dur by remember { derivedStateOf { MusicStateHolder.duration } }
    val modeIcon = when (mode) {
        PlayMode.SEQUENTIAL -> "→"; PlayMode.LIST_LOOP -> "🔁"
        PlayMode.SINGLE_LOOP -> "🔂"; PlayMode.SHUFFLE -> "🔀"
    }
    GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonGreen, enableBreathe = isPlaying) {
        Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
            LinearProgressIndicator(
                progress = if (dur > 0) pos.toFloat() / dur else 0f,
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                color = Tokens.NeonGreen, trackColor = Tokens.Ink500)
            Spacer(Modifier.height(Tokens.SpaceS))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.name, color = Color.White, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = Tokens.Ink300, fontSize = 11.sp, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceM),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(modeIcon, fontSize = 18.sp,
                        modifier = Modifier.clickable { MusicController.cyclePlayMode() })
                    IconButton(onClick = { MusicController.playPrev() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipPrevious, "上一首", tint = Color.White)
                    }
                    FilledIconButton(onClick = { MusicController.togglePlayPause() },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Tokens.NeonGreen)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "播放", modifier = Modifier.size(24.dp), tint = Color.Black)
                    }
                    IconButton(onClick = { MusicController.playNext() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipNext, "下一首", tint = Color.White)
                    }
                }
            }
        }
    }
}
