package com.island.dynamic.ui
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.theme.*
import kotlinx.coroutines.delay
@Composable
fun IntroDialog(onClose: () -> Unit) {
    val paragraphs = listOf(
        "灵动岛是一款为 Android 8.1 及以上系统深度定制的智能交互应用。它将 iPhone 14 Pro 首创的灵动岛概念引入安卓生态，并在其基础上进行了全面创新与本土化改造。",
        "v9.0 新增 16 进程并行分块下载器，采用 HTTP Range 请求将文件切分为多达 32 个数据块，每个分块独立下载并直写文件偏移位置，速度可达单线程的十倍以上。支持暂停、续传、失败自动重试三次，实时显示总速度和剩余时间。",
        "应用采用液态玻璃设计语言，融合了高斯模糊、光晕折射、顶部高光三层视觉效果，配合精心调校的弹簧动画参数，每一次交互都如丝般顺滑。您可以在设置中自由切换 iPhone 原生风、液态玻璃风、卡片风三种界面风格，全局实时生效。",
        "灵动岛悬浮窗常驻屏幕顶部，支持八种状态自动切换：空闲、充电、通知、录屏、天气、日历、音乐控制、下载进度。通过拖动手势，您可以将灵动岛吸附到屏幕最左或最右，向左吸附后五秒自动回中，向上拖动则隐藏为幽灵点，轻触即可恢复。",
        "音乐模式内置网易云搜索与本地全盘扫描，支持顺序、列表循环、单曲循环、随机四种播放模式。播放停止后自动切回生活模式，避免打扰。",
        "生活模式集成通知监听、天气刷新、日历显示、电池监控、屏幕录制五大功能，全部无需密钥即可使用。启动动画采用五阶段编排，从黑洞汇聚到文字浮现，三秒呈现完整视觉盛宴。",
        "灵动岛，让手机交互更有温度。")
    val visible = remember { mutableStateListOf(*Array(paragraphs.size) { false }) }
    LaunchedEffect(Unit) { paragraphs.forEachIndexed { i, _ -> delay(180L); visible[i] = true } }
    DialogOverlay(onDismiss = onClose) {
        GlassSurface(Modifier.width(340.dp).fillMaxHeight(0.85f), cornerRadius = Tokens.RadiusXL, glowColor = Tokens.NeonGreen, enableBreathe = true) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().padding(Tokens.SpaceL), contentAlignment = Alignment.Center) {
                    val pulse by rememberInfiniteTransition(label = "p")
                        .animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ps")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(72.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.blur(18.dp).clip(RoundedCornerShape(24.dp)).background(Tokens.NeonGreen.copy(alpha = 0.45f)))
                        Box(Modifier.size(72.dp).offset(y = (-72).dp).clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Tokens.GlassWhite25, Tokens.GlassDark25)))
                            .border(1.dp, Tokens.GlassBorderStrong, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                            Box(Modifier.width(38.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black))
                            Box(Modifier.size(4.dp).offset(x = 10.dp).clip(RoundedCornerShape(2.dp)).background(Tokens.NeonGreen)) }
                        Spacer(Modifier.height((-56).dp))
                        Text("灵动岛", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("DYNAMIC ISLAND · v9.0", color = Tokens.NeonGreen, fontSize = 10.sp, letterSpacing = 3.sp) } }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Tokens.GlassBorder, Color.Transparent))))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Tokens.SpaceL)) {
                    paragraphs.forEachIndexed { i, p ->
                        val alpha by animateFloatAsState(if (visible[i]) 1f else 0f, tween(420), label = "a$i")
                        val ty by animateFloatAsState(if (visible[i]) 0f else 12f, tween(420), label = "t$i")
                        Text(p, color = Color.White.copy(alpha = alpha * 0.92f), fontSize = 13.sp, lineHeight = 22.sp,
                            modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = ty })
                        if (i < paragraphs.size - 1) Spacer(Modifier.height(Tokens.SpaceM)) } }
                Box(Modifier.fillMaxWidth().padding(Tokens.SpaceL)) {
                    GlassButton("开始使用", Modifier.fillMaxWidth(), accent = Tokens.NeonGreen, onClick = onClose) } } } }
}
@Composable
private fun DialogOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(0.72f, 360f)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.value }.background(Color.Black.copy(alpha = 0.62f)).clickable { onDismiss() })
        Box(Modifier.align(Alignment.Center).graphicsLayer {
            val p = progress.value
            scaleX = 0.88f + p * 0.12f; scaleY = 0.88f + p * 0.12f
            alpha = p; translationY = (1f - p) * -20f }) { content() } } }
