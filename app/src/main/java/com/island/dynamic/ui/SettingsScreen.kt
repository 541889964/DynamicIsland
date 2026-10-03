package com.island.dynamic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.settings.SettingsStore
import com.island.dynamic.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit, onShowIntro: () -> Unit) {
    val context = LocalContext.current
    val style = StyleHolder.current

    Box(Modifier.fillMaxSize().background(Tokens.Ink900)) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Tokens.SpaceL)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(Tokens.RadiusS))
                        .background(Tokens.GlassWhite10)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(Tokens.SpaceM))
                Text("设置", color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(Tokens.SpaceXL))
            SectionTitle("界面风格")
            Spacer(Modifier.height(Tokens.SpaceS))

            GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonPurple) {
                Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
                    StyleOption("iPhone 原生风", "纯色 + 柔和阴影",
                        UiStyle.IPHONE, style) {
                        SettingsStore.setStyle(context, UiStyle.IPHONE)
                    }
                    Spacer(Modifier.height(Tokens.SpaceS))
                    StyleOption("液态玻璃风", "半透 + 流动高光",
                        UiStyle.GLASS, style) {
                        SettingsStore.setStyle(context, UiStyle.GLASS)
                    }
                    Spacer(Modifier.height(Tokens.SpaceS))
                    StyleOption("卡片风", "实色 + 强阴影",
                        UiStyle.CARD, style) {
                        SettingsStore.setStyle(context, UiStyle.CARD)
                    }
                }
            }

            Spacer(Modifier.height(Tokens.SpaceL))
            SectionTitle("下载")
            Spacer(Modifier.height(Tokens.SpaceS))

            GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonBlue) {
                Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("下载线程数", color = Color.White, fontSize = 15.sp)
                            Text("${SettingsStore.downloadThreads} 进程",
                                color = Tokens.Ink300, fontSize = 12.sp)
                        }
                        Text("${SettingsStore.downloadThreads}", color = Tokens.NeonBlue,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = SettingsStore.downloadThreads.toFloat(),
                        onValueChange = { SettingsStore.setDownloadThreads(context, it.toInt()) },
                        valueRange = 1f..32f, steps = 30
                    )
                }
            }

            Spacer(Modifier.height(Tokens.SpaceL))
            SectionTitle("音乐")
            Spacer(Modifier.height(Tokens.SpaceS))

            GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonGreen) {
                Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("自动切回生活模式", color = Color.White, fontSize = 15.sp)
                            Text("停止播放后 ${SettingsStore.autoSwitchSec} 秒",
                                color = Tokens.Ink300, fontSize = 12.sp)
                        }
                        Text("${SettingsStore.autoSwitchSec}s", color = Tokens.NeonGreen,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = SettingsStore.autoSwitchSec.toFloat(),
                        onValueChange = { SettingsStore.setAutoSwitchSec(context, it.toInt()) },
                        valueRange = 5f..60f, steps = 10
                    )
                }
            }

            Spacer(Modifier.height(Tokens.SpaceL))
            SectionTitle("关于")
            Spacer(Modifier.height(Tokens.SpaceS))

            GlassSurface(Modifier.fillMaxWidth(), Tokens.RadiusL, Tokens.NeonBlue) {
                Column(Modifier.fillMaxWidth().padding(Tokens.SpaceM)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onShowIntro() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(Tokens.RadiusS))
                                .background(Tokens.GlassWhite10),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, null, tint = Color.White,
                                modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(Tokens.SpaceM))
                        Column(Modifier.weight(1f)) {
                            Text("软件介绍", color = Color.White, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium)
                            Text("了解灵动岛全部功能", color = Tokens.Ink300, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Tokens.Ink300)
                    }
                    Spacer(Modifier.height(Tokens.SpaceS))
                    Text("灵动岛 v9.0 · Android 8.1+",
                        color = Tokens.Ink300, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(Tokens.SpaceXL))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Tokens.Ink300, fontSize = 13.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
}

@Composable
private fun StyleOption(
    title: String, subtitle: String,
    target: UiStyle, current: UiStyle, onClick: () -> Unit
) {
    val selected = target == current
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.RadiusM))
            .background(if (selected) Tokens.NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp,
                if (selected) Tokens.NeonPurple.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(Tokens.RadiusM))
            .clickable(onClick = onClick)
            .padding(Tokens.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(Tokens.RadiusS))
            .background(if (selected) Tokens.NeonPurple.copy(alpha = 0.4f) else Tokens.Ink600))
        Spacer(Modifier.width(Tokens.SpaceM))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Tokens.Ink300, fontSize = 12.sp)
        }
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = Tokens.NeonPurple)
    }
}
