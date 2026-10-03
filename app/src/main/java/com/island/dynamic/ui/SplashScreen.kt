package com.island.dynamic.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.theme.Tokens

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val timeline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeline.animateTo(1f, tween(3000, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        onFinish()
    }
    val t = timeline.value
    val dotScale = stage(t, 0f, 0.17f)
    val burstScale = stage(t, 0.15f, 0.32f)
    val r1 = stage(t, 0.28f, 0.55f)
    val r2 = stage(t, 0.33f, 0.60f)
    val r3 = stage(t, 0.38f, 0.65f)
    val logoIn = stage(t, 0.42f, 0.66f)
    val textIn = stage(t, 0.56f, 0.80f)
    val exit = stage(t, 0.82f, 1f)

    val logoScale by remember {
        derivedStateOf {
            val p = logoIn
            when {
                p < 0.7f -> p / 0.7f * 1.15f
                else -> 1.15f - (p - 0.7f) / 0.3f * 0.15f
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(
                listOf(Color(0xFF0A0E1A), Color(0xFF000000)), radius = 1200f))
            .graphicsLayer {
                translationY = -exit * 220f
                alpha = 1f - exit
                scaleX = 1f + exit * 0.08f
                scaleY = 1f + exit * 0.08f
            },
        contentAlignment = Alignment.Center
    ) {
        // 三层扩散环
        WaveRing(r1, 260.dp, 3.dp, Tokens.NeonGreen)
        WaveRing(r2, 340.dp, 2.dp, Tokens.NeonBlue)
        WaveRing(r3, 420.dp, 1.5.dp, Tokens.NeonPurple)

        // 中心爆发点
        Box(Modifier.graphicsLayer {
            val s = if (burstScale > 0f) {
                val b = burstScale
                if (b < 0.5f) 1f + b * 1.2f else 1.6f - (b - 0.5f) * 1.2f
            } else 0.3f + dotScale * 0.7f
            scaleX = s
            scaleY = s
            alpha = if (burstScale > 0.8f) 1f - (burstScale - 0.8f) * 5f else 1f
        }.size(8.dp).clip(CircleShape).background(Color.White))

        // 爆发光晕
        if (burstScale in 0.01f..0.95f) {
            Box(Modifier.graphicsLayer {
                val b = burstScale
                val hs = if (b < 0.5f) b * 2f else (1f - (b - 0.5f) * 2f)
                scaleX = hs * 4f
                scaleY = hs * 4f
                alpha = hs * 0.55f
            }.size(80.dp).blur(60.dp).clip(CircleShape)
                .background(Brush.radialGradient(
                    listOf(Tokens.NeonGreen, Color.Transparent))))
        }

        // Logo
        Box(Modifier.graphicsLayer {
            scaleX = logoScale
            scaleY = logoScale
            alpha = logoIn.coerceAtMost(1f)
        }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(180.dp).blur(50.dp).clip(CircleShape)
                .background(Brush.radialGradient(
                    listOf(Tokens.NeonGreen.copy(alpha = 0.5f), Color.Transparent))))
            Box(Modifier.size(140.dp)
                .shadow(32.dp, RoundedCornerShape(40.dp),
                    ambientColor = Tokens.NeonGreen.copy(alpha = 0.4f),
                    spotColor = Tokens.NeonGreen.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(40.dp))
                .background(Brush.linearGradient(
                    listOf(Tokens.GlassWhite15, Tokens.GlassDark25))))
            Box(Modifier.size(140.dp).clip(RoundedCornerShape(40.dp))
                .background(Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.2f),
                    0.4f to Color.Transparent)))
            Box(Modifier.width(70.dp).height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.Black)
                .shadow(12.dp, RoundedCornerShape(11.dp),
                    ambientColor = Tokens.NeonGreen.copy(alpha = 0.8f),
                    spotColor = Tokens.NeonGreen))
            val dp by rememberInfiniteTransition(label = "d")
                .animateFloat(0.6f, 1f,
                    infiniteRepeatable(tween(800, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse), label = "dp")
            Box(Modifier.graphicsLayer { alpha = dp }.size(6.dp)
                .clip(CircleShape).background(Tokens.NeonGreen))
        }

        // 文字
        Column(
            Modifier.align(Alignment.Center).offset(y = 140.dp)
                .graphicsLayer { alpha = textIn },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("灵 动 岛", color = Color.White,
                fontSize = (24 + (1f - textIn) * 8).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (6 + (1f - textIn) * 12).sp)
            Spacer(Modifier.height(10.dp))
            Text("MULTI-THREAD  EDITION", color = Tokens.NeonGreen.copy(alpha = textIn),
                fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
        }
    }
}

@Composable
private fun WaveRing(progress: Float, maxSize: androidx.compose.ui.unit.Dp,
                     border: androidx.compose.ui.unit.Dp, color: Color) {
    if (progress <= 0f || progress >= 1f) return
    val size = maxSize * progress
    val alpha = (1f - progress) * 0.7f
    Box(Modifier.size(size).clip(CircleShape)
        .background(color.copy(alpha = alpha * 0.15f))
        .then(Modifier))
}

private fun stage(t: Float, s: Float, e: Float): Float =
    ((t - s) / (e - s)).coerceIn(0f, 1f)
