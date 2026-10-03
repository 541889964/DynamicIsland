package com.island.dynamic.theme
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun GlassSurface(modifier: Modifier = Modifier, cornerRadius: Dp = Tokens.RadiusL,
    glowColor: Color = Tokens.NeonGreen, enableBreathe: Boolean = false,
    content: @Composable () -> Unit) {
    when (StyleHolder.current) {
        UiStyle.GLASS -> GlassStyleBody(modifier, cornerRadius, glowColor, enableBreathe, content)
        UiStyle.IPHONE -> IPhoneStyleBody(modifier, cornerRadius, content)
        UiStyle.CARD -> CardStyleBody(modifier, cornerRadius, content)
    }
}
@Composable
private fun GlassStyleBody(modifier: Modifier, cr: Dp, glow: Color, breathe: Boolean, content: @Composable () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "b")
    val b by infinite.animateFloat(
        if (breathe) 0.4f else 0.6f, if (breathe) 0.75f else 0.6f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bv")
    Box(modifier) {
        Box(Modifier.matchParentSize().blur(36.dp).clip(RoundedCornerShape(cr))
            .background(glow.copy(alpha = b * 0.28f)))
        Box(Modifier.matchParentSize()
            .shadow(12.dp, RoundedCornerShape(cr), ambientColor = glow.copy(alpha = 0.4f), spotColor = glow.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(cr))
            .background(Brush.linearGradient(0f to Tokens.GlassWhite15, 0.5f to Tokens.GlassWhite10, 1f to Tokens.GlassDark10))
            .border(1.dp, Brush.linearGradient(0f to Tokens.GlassBorderStrong, 0.5f to Tokens.GlassBorder, 1f to Color.Transparent), RoundedCornerShape(cr)))
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(cr))
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.14f), 0.3f to Color.Transparent)))
        content()
    }
}
@Composable
private fun IPhoneStyleBody(modifier: Modifier, cr: Dp, content: @Composable () -> Unit) {
    Box(modifier) {
        Box(Modifier.matchParentSize()
            .shadow(8.dp, RoundedCornerShape(cr), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(cr)).background(Tokens.Ink700.copy(alpha = 0.92f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(cr)))
        content()
    }
}
@Composable
private fun CardStyleBody(modifier: Modifier, cr: Dp, content: @Composable () -> Unit) {
    Box(modifier) {
        Box(Modifier.matchParentSize()
            .shadow(20.dp, RoundedCornerShape(cr), ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.7f))
            .clip(RoundedCornerShape(cr)).background(Tokens.Ink600))
        content()
    }
}
@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, accent: Color = Tokens.NeonGreen, onClick: () -> Unit) {
    val style = StyleHolder.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(modifier
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .height(52.dp).clip(RoundedCornerShape(Tokens.RadiusM))
        .background(when (style) {
            UiStyle.GLASS -> Brush.horizontalGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.65f)))
            UiStyle.IPHONE -> Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.85f)))
            UiStyle.CARD -> Brush.horizontalGradient(listOf(Tokens.Ink500, Tokens.Ink600)) })
        .border(1.dp, if (style == UiStyle.GLASS) Color.White.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(Tokens.RadiusM))
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { scope.launch { scale.animateTo(0.96f, spring(0.6f, 600f)) }; tryAwaitRelease(); scope.launch { scale.animateTo(1f, spring(0.65f, 500f)) } },
                onTap = { onClick() }) },
        contentAlignment = Alignment.Center) { Text(text, color = Color.White) }
}
