package com.island.dynamic

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.model.*
import com.island.dynamic.music.MusicController
import com.island.dynamic.service.*
import com.island.dynamic.settings.SettingsStore
import com.island.dynamic.theme.*
import com.island.dynamic.ui.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val recordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(applicationContext, ScreenRecordService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsStore.init(this)
        val appCtx: Context = applicationContext

        intent?.data?.let { uri ->
            if (uri.host == "music") {
                val kw = uri.getQueryParameter("keyword") ?: uri.getQueryParameter("q")
                if (!kw.isNullOrBlank()) {
                    IslandStateHolder.appMode = AppMode.MUSIC
                    appCtx.startForegroundService(Intent(appCtx, MusicPlaybackService::class.java))
                    MusicController.searchAndPlayFirst(kw, MusicStateHolder.searchLimit)
                }
            }
            if (uri.host == "download") {
                if (!uri.getQueryParameter("url").isNullOrBlank()) {
                    IslandStateHolder.appMode = AppMode.DOWNLOAD
                    appCtx.startForegroundService(Intent(appCtx, DownloadService::class.java))
                }
            }
        }

        setContent {
            MaterialTheme {
                var showSplash by remember { mutableStateOf(true) }
                var showSettings by remember { mutableStateOf(false) }
                var showIntro by remember { mutableStateOf(false) }
                val styleKey = StyleHolder.current

                LaunchedEffect(showSplash) {
                    if (!showSplash && !SettingsStore.introShown) {
                        kotlinx.coroutines.delay(400)
                        showIntro = true
                    }
                }

                Box(Modifier.fillMaxSize().background(Tokens.Ink900)) {
                    AnimatedContent(
                        targetState = Triple(showSplash, showSettings, styleKey),
                        transitionSpec = {
                            (fadeIn(tween(500)) + scaleIn(initialScale = 0.98f))
                                .togetherWith(fadeOut(tween(400)) + scaleOut(targetScale = 1.02f))
                        },
                        label = "root"
                    ) { (splash, settings, _) ->
                        when {
                            splash -> SplashScreen(onFinish = { showSplash = false })
                            settings -> SettingsScreen(
                                onBack = { showSettings = false },
                                onShowIntro = { showIntro = true })
                            else -> MainScreen(
                                appCtx = appCtx,
                                onStartIsland = {
                                    appCtx.startForegroundService(
                                        Intent(appCtx, IslandService::class.java))
                                },
                                onToggleRecord = { toggleRecording() },
                                onOpenOverlayPermission = { openOverlayPermission() },
                                onStartMusicService = {
                                    appCtx.startForegroundService(
                                        Intent(appCtx, MusicPlaybackService::class.java))
                                },
                                onStartDownloadService = {
                                    appCtx.startForegroundService(
                                        Intent(appCtx, DownloadService::class.java))
                                },
                                onOpenSettings = { showSettings = true })
                        }
                    }
                    if (showIntro) {
                        IntroDialog(onClose = {
                            showIntro = false
                            SettingsStore.markIntroShown(appCtx)
                        })
                    }
                }
            }
        }
    }

    private fun toggleRecording() {
        if (IslandStateHolder.isRecording) {
            startService(Intent(applicationContext, ScreenRecordService::class.java).apply {
                action = "STOP"
            })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    appCtx: Context,
    onStartIsland: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onStartMusicService: () -> Unit,
    onStartDownloadService: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var appMode by remember { mutableStateOf(IslandStateHolder.appMode) }
    LaunchedEffect(IslandStateHolder.appMode) { appMode = IslandStateHolder.appMode }

    Box(Modifier.fillMaxSize().background(Tokens.Ink900)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (appMode) {
                                AppMode.LIFE -> "灵动岛 · 生活"
                                AppMode.MUSIC -> "灵动岛 · 音乐"
                                AppMode.DOWNLOAD -> "灵动岛 · 下载"
                            },
                            color = Color.White, fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, "设置", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.Transparent) {
                    NavigationBarItem(
                        selected = appMode == AppMode.LIFE,
                        onClick = {
                            appMode = AppMode.LIFE
                            IslandStateHolder.appMode = AppMode.LIFE
                        },
                        icon = { Icon(Icons.Default.Home, "生活") },
                        label = { Text("生活", fontSize = 10.sp) })
                    NavigationBarItem(
                        selected = appMode == AppMode.MUSIC,
                        onClick = {
                            appMode = AppMode.MUSIC
                            IslandStateHolder.appMode = AppMode.MUSIC
                            onStartMusicService()
                        },
                        icon = { Icon(Icons.Default.PlayArrow, "音乐") },
                        label = { Text("音乐", fontSize = 10.sp) })
                    NavigationBarItem(
                        selected = appMode == AppMode.DOWNLOAD,
                        onClick = {
                            appMode = AppMode.DOWNLOAD
                            IslandStateHolder.appMode = AppMode.DOWNLOAD
                            onStartDownloadService()
                        },
                        icon = { Icon(Icons.Default.CloudDownload, "下载") },
                        label = { Text("下载", fontSize = 10.sp) })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (appMode) {
                    AppMode.LIFE -> LifeModeScreen(
                        onStartIsland, onToggleRecord, onOpenOverlayPermission)
                    AppMode.MUSIC -> MusicModeScreen(appCtx)
                    AppMode.DOWNLOAD -> DownloadScreen()
                }
            }
        }
    }
}

@Composable
private fun LifeModeScreen(
    onStartIsland: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenOverlayPermission: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(Tokens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(Tokens.SpaceM)
    ) {
        GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonGreen) {
            Column(Modifier.fillMaxWidth().padding(Tokens.SpaceL)) {
                Text("欢迎使用灵动岛", color = Color.White,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("通知 · 天气 · 日历 · 电量 · 录屏 · 下载",
                    color = Tokens.Ink300, fontSize = 13.sp)
            }
        }
        GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonBlue) {
            Column(
                Modifier.fillMaxWidth().padding(Tokens.SpaceM),
                verticalArrangement = Arrangement.spacedBy(Tokens.SpaceS)
            ) {
                ActionRow(Icons.Default.Settings, "授予悬浮窗权限", onOpenOverlayPermission)
                ActionRow(Icons.Default.Star, "启动灵动岛", onStartIsland)
                ActionRow(Icons.Default.Videocam,
                    if (IslandStateHolder.isRecording) "停止录屏" else "开始录屏",
                    onToggleRecord)
            }
        }
        Text("显示模式", color = Tokens.Ink300, fontSize = 13.sp,
            fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceS)) {
            ModeChip("时钟", IslandMode.IDLE)
            ModeChip("天气", IslandMode.WEATHER)
            ModeChip("日历", IslandMode.CALENDAR)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceS)) {
            ModeChip("通知", IslandMode.NOTIFICATION)
            ModeChip("充电", IslandMode.CHARGING)
            ModeChip("录屏", IslandMode.RECORDING)
        }
        Spacer(Modifier.weight(1f))
        GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL,
            if (IslandStateHolder.isCharging) Tokens.NeonGreen else Tokens.NeonBlue) {
            Row(
                Modifier.fillMaxWidth().padding(Tokens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("电池", color = Tokens.Ink300, fontSize = 12.sp)
                    Text("${IslandStateHolder.batteryLevel}%",
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("${IslandStateHolder.batteryLevel}%",
                    color = Tokens.NeonGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.RadiusM))
            .clickable(onClick = onClick)
            .padding(Tokens.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    Box(
        Modifier.clip(RoundedCornerShape(Tokens.RadiusPill))
            .background(if (selected) Tokens.NeonGreen.copy(alpha = 0.25f) else Tokens.GlassWhite10)
            .clickable { IslandStateHolder.mode = target }
            .padding(horizontal = Tokens.SpaceM, vertical = Tokens.SpaceS)
    ) {
        Text(label, color = if (selected) Tokens.NeonGreen else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun MusicModeScreen(appCtx: Context) {
    var keyword by remember { mutableStateOf("") }
    val results by remember { derivedStateOf { MusicStateHolder.searchResults.toList() } }
    val locals by remember { derivedStateOf { MusicStateHolder.localMusic.toList() } }
    val isSearching by remember { derivedStateOf { MusicStateHolder.isSearching } }
    val isScanning by remember { derivedStateOf { MusicStateHolder.isScanningLocal } }
    val scanProgress by remember { derivedStateOf { MusicStateHolder.scanProgress } }
    val currentSong by remember { derivedStateOf { MusicStateHolder.currentSong } }
    val isPlaying by remember { derivedStateOf { MusicStateHolder.isPlaying } }

    Column(Modifier.fillMaxSize()) {
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM),
            Tokens.RadiusXL, Tokens.NeonGreen
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索歌曲...", color = Tokens.Ink300) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Tokens.Ink300) },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp, color = Tokens.NeonGreen)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Tokens.NeonGreen)
            )
        }
        Spacer(Modifier.height(Tokens.SpaceS))
        Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceM)) {
            GlassButton(
                text = if (isScanning) "扫描中 ${(scanProgress * 100).toInt()}%"
                       else "扫描本地音乐 (${locals.size})",
                modifier = Modifier.fillMaxWidth(),
                accent = Tokens.NeonBlue,
                onClick = { if (!isScanning) MusicController.scanLocalMusic(appCtx) })
        }
        Spacer(Modifier.height(Tokens.SpaceS))
        val displayList = if (results.isNotEmpty()) results else locals
        if (displayList.isNotEmpty()) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Tokens.SpaceM)
            ) {
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
                Text("点击上方按钮扫描本地音乐", color = Tokens.Ink300, fontSize = 14.sp)
            }
        }
        currentSong?.let { MiniPlayerBar(it, isPlaying) }
    }
}

@Composable
private fun SongItem(song: Song, isActive: Boolean, onClick: () -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        Tokens.RadiusM,
        if (isActive) Tokens.NeonGreen else Tokens.NeonBlue
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Tokens.SpaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(song.name,
                    color = if (isActive) Tokens.NeonGreen else Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Tokens.Ink300, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isActive) Icon(Icons.Default.PlayArrow, null, tint = Tokens.NeonGreen)
        }
    }
}

@Composable
private fun MiniPlayerBar(song: Song, isPlaying: Boolean) {
    GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonGreen) {
        Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
            Text(song.name, color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Tokens.SpaceS))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { MusicController.playPrev() }) {
                    Icon(Icons.Default.SkipPrevious, "上一首", tint = Color.White)
                }
                FilledIconButton(
                    onClick = { MusicController.togglePlayPause() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Tokens.NeonGreen)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "播放", tint = Color.Black)
                }
                IconButton(onClick = { MusicController.playNext() }) {
                    Icon(Icons.Default.SkipNext, "下一首", tint = Color.White)
                }
            }
        }
    }
}
