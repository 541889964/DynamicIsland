package com.island.dynamic.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Tokens.RadiusL,
    glowColor: Color = Tokens.NeonGreen,
    enableBreathe: Boolean = false,
    content: @Composable () -> Unit
) {
    when (StyleHolder.current) {
        UiStyle.GLASS -> GlassStyleBody(modifier, cornerRadius, glowColor, enableBreathe, content)
        UiStyle.IPHONE -> IPhoneStyleBody(modifier, cornerRadius, content)
        UiStyle.CARD -> CardStyleBody(modifier, cornerRadius, content)
    }
}

@Composable
private fun GlassStyleBody(
    modifier: Modifier, cornerRadius: Dp, glowColor: Color,
    enableBreathe: Boolean, content: @Composable () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "breathe")
    val breathe by infinite.animateFloat(
        if (enableBreathe) 0.4f else 0.6f, if (enableBreathe) 0.75f else 0.6f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b")
    Box(modifier) {
        Box(Modifier.matchParentSize().blur(36.dp).clip(RoundedCornerShape(cornerRadius))
            .background(glowColor.copy(alpha = breathe * 0.28f)))
        Box(Modifier.matchParentSize()
            .shadow(12.dp, RoundedCornerShape(cornerRadius),
                ambientColor = glowColor.copy(alpha = 0.4f),
                spotColor = glowColor.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(
                0f to Tokens.GlassWhite15, 0.5f to Tokens.GlassWhite10, 1f to Tokens.GlassDark10))
            .border(1.dp, Brush.linearGradient(
                0f to Tokens.GlassBorderStrong, 0.5f to Tokens.GlassBorder, 1f to Color.Transparent),
                RoundedCornerShape(cornerRadius)))
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.14f), 0.3f to Color.Transparent)))
        content()
    }
}

@Composable
private fun IPhoneStyleBody(modifier: Modifier, cornerRadius: Dp, content: @Composable () -> Unit) {
    Box(modifier) {
        Box(Modifier.matchParentSize()
            .shadow(8.dp, RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Tokens.Ink700.copy(alpha = 0.92f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(cornerRadius)))
        content()
    }
}

@Composable
private fun CardStyleBody(modifier: Modifier, cornerRadius: Dp, content: @Composable () -> Unit) {
    Box(modifier) {
        Box(Modifier.matchParentSize()
            .shadow(20.dp, RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.7f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Tokens.Ink600))
        content()
    }
}

/**
 * 丝滑弹窗 — 关闭时反向动画 + 拖拽关闭
 * 关闭流程：内容 scale 1→0.92 同时 fade + 背景淡出 + 触摸事件延时释放
 */
@Composable
fun AnimatedDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    allowSwipeDown: Boolean = true,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragY by remember { mutableStateOf(0f) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            closing = false
            progress.snapTo(0f)
            progress.animateTo(1f, spring(0.72f, 360f))
        } else if (!closing) {
            closing = true
            progress.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            closing = false
        }
    }

    if (progress.value > 0.001f) {
        Box(Modifier.fillMaxSize()) {
            // 背景遮罩 — 透明度跟随 progress
            Box(Modifier.fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(Color.Black.copy(alpha = 0.62f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } })

            // 弹窗内容
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = dragY.dp)
                    .graphicsLayer {
                        val p = progress.value
                        scaleX = 0.88f + p * 0.12f
                        scaleY = 0.88f + p * 0.12f
                        alpha = p
                        // 关闭时轻微上移 + 模糊
                        translationY = (1f - p) * -20f
                    }
                    .then(
                        if (allowSwipeDown) Modifier.pointerInput(Unit) {
                            var accY = 0f
                            detectDragGestures(
                                onDragStart = { accY = 0f },
                                onDragEnd = {
                                    if (accY > 120f) onDismiss() else dragY = 0f
                                    accY = 0f
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    accY += drag.y
                                    dragY = (dragY + drag.y).coerceAtLeast(0f)
                                })
                        } else Modifier
                    )
            ) { content() }
        }
    }
}

@Composable
fun GlassButton(
    text: String, modifier: Modifier = Modifier,
    accent: Color = Tokens.NeonGreen, onClick: () -> Unit
) {
    val style = StyleHolder.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .height(52.dp).clip(RoundedCornerShape(Tokens.RadiusM))
            .background(
                when (style) {
                    UiStyle.GLASS -> Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.65f)))
                    UiStyle.IPHONE -> Brush.horizontalGradient(
                        listOf(accent, accent.copy(alpha = 0.85f)))
                    UiStyle.CARD -> Brush.horizontalGradient(
                        listOf(Tokens.Ink500, Tokens.Ink600))
                })
            .border(1.dp,
                if (style == UiStyle.GLASS) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(Tokens.RadiusM))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { scale.animateTo(0.96f, spring(0.6f, 600f)) }
                        tryAwaitRelease()
                        scope.launch { scale.animateTo(1f, spring(0.65f, 500f)) }
                    },
                    onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White) }
}
