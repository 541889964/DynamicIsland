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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.dynamic.theme.Tokens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        phase = 1
        delay(400)
        phase = 2
        logoVisible = true
        delay(500)
        textVisible = true
        delay(1200)
        exiting = true
        delay(400)
        onFinish()
    }

    val exitProgress by animateFloatAsState(
        targetValue = if (exiting) 1f else 0f,
        animationSpec = tween(400), label = "exit")
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f), label = "logo")
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(500), label = "text")
    val ring1 by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(1200), label = "r1")
    val ring2 by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(1000), label = "r2")

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(
                listOf(Color(0xFF0A0E1A), Color(0xFF000000)), radius = 1200f))
            .graphicsLayer {
                translationY = -exitProgress * 220f
                alpha = 1f - exitProgress
                scaleX = 1f + exitProgress * 0.08f
                scaleY = 1f + exitProgress * 0.08f
            },
        contentAlignment = Alignment.Center
    ) {
        if (ring1 in 0.01f..0.99f) {
            Box(Modifier.size(260.dp * ring1).clip(CircleShape)
                .background(Tokens.NeonGreen.copy(alpha = (1f - ring1) * 0.25f)))
        }
        if (ring2 in 0.01f..0.99f) {
            Box(Modifier.size(340.dp * ring2).clip(CircleShape)
                .background(Tokens.NeonBlue.copy(alpha = (1f - ring2) * 0.2f)))
        }
        Box(Modifier.graphicsLayer {
            scaleX = logoScale; scaleY = logoScale
            alpha = logoScale.coerceIn(0f, 1f)
        }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(180.dp).blur(50.dp).clip(CircleShape)
                .background(Brush.radialGradient(
                    listOf(Tokens.NeonGreen.copy(alpha = 0.5f), Color.Transparent))))
            Box(Modifier.size(140.dp).clip(RoundedCornerShape(40.dp))
                .background(Brush.linearGradient(
                    listOf(Tokens.GlassWhite15, Tokens.GlassDark25))))
            Box(Modifier.size(140.dp).clip(RoundedCornerShape(40.dp))
                .background(Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.2f),
                    0.4f to Color.Transparent)))
            Box(Modifier.width(70.dp).height(22.dp).clip(RoundedCornerShape(11.dp))
                .background(Color.Black))
            val pulse by rememberInfiniteTransition(label = "p")
                .animateFloat(0.5f, 1f,
                    infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pv")
            Box(Modifier.graphicsLayer { alpha = pulse }.size(6.dp)
                .clip(CircleShape).background(Tokens.NeonGreen))
        }
        Column(
            Modifier.align(Alignment.Center).offset(y = 140.dp)
                .graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("灵 动 岛", color = Color.White, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
            Spacer(Modifier.height(10.dp))
            Text("MULTI-THREAD EDITION", color = Tokens.NeonGreen,
                fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
        }
    }
}
