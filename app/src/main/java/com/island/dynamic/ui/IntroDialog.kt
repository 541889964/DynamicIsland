package com.island.dynamic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.theme.*

@Composable
fun IntroDialog(onClose: () -> Unit) {
    val paragraphs = listOf(
        "灵动岛是一款为 Android 8.1 及以上系统深度定制的智能交互应用。它将 iPhone 首创的灵动岛概念引入安卓生态，并进行了全面创新与本土化改造。",
        "应用采用液态玻璃设计语言，融合高斯模糊、光晕折射、顶部高光三层视觉效果，配合精心调校的弹簧动画参数，每一次交互都如丝般顺滑。您可以在设置中自由切换 iPhone 原生风、液态玻璃风、卡片风三种界面风格。",
        "灵动岛悬浮窗常驻屏幕顶部，支持八种状态自动切换：空闲、充电、通知、录屏、天气、日历、音乐控制、下载进度。通过拖动手势，您可以将灵动岛吸附到屏幕最左或最右。",
        "音乐模式内置网易云搜索与本地全盘扫描，支持顺序、列表循环、单曲循环、随机四种播放模式。",
        "生活模式集成通知监听、天气刷新、日历显示、电池监控、屏幕录制五大功能，全部无需密钥即可使用。",
        "灵动岛，让手机交互更有温度。"
    )

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(340.dp)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .background(Tokens.Ink700)
                .clickable(enabled = false) { }
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().padding(Tokens.SpaceL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("灵动岛", color = Color.White, fontSize = 24.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("DYNAMIC ISLAND v9.0", color = Tokens.NeonGreen,
                            fontSize = 10.sp, letterSpacing = 3.sp)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp)
                    .background(Tokens.GlassBorder))
                Column(
                    Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(Tokens.SpaceL)
                ) {
                    paragraphs.forEachIndexed { i, p ->
                        Text(p, color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp, lineHeight = 22.sp)
                        if (i < paragraphs.size - 1) Spacer(Modifier.height(Tokens.SpaceM))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(Tokens.SpaceL)) {
                    GlassButton("开始使用", Modifier.fillMaxWidth(),
                        accent = Tokens.NeonGreen, onClick = onClose)
                }
            }
        }
    }
}
