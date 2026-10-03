package com.island.dynamic.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Tokens.RadiusL,
    glowColor: Color = Tokens.NeonGreen,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        when (StyleHolder.current) {
            UiStyle.GLASS -> {
                Box(Modifier.matchParentSize().blur(24.dp).clip(RoundedCornerShape(cornerRadius))
                    .background(glowColor.copy(alpha = 0.18f)))
                Box(Modifier.matchParentSize()
                    .shadow(8.dp, RoundedCornerShape(cornerRadius))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Brush.linearGradient(
                        listOf(Tokens.GlassWhite15, Tokens.GlassWhite10, Tokens.GlassDark10)))
                    .border(1.dp, Tokens.GlassBorder, RoundedCornerShape(cornerRadius)))
            }
            UiStyle.IPHONE -> {
                Box(Modifier.matchParentSize()
                    .shadow(6.dp, RoundedCornerShape(cornerRadius))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Tokens.Ink700))
            }
            UiStyle.CARD -> {
                Box(Modifier.matchParentSize()
                    .shadow(16.dp, RoundedCornerShape(cornerRadius))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Tokens.Ink600))
            }
        }
        content()
    }
}

@Composable
fun GlassButton(
    text: String, modifier: Modifier = Modifier,
    accent: Color = Tokens.NeonGreen, onClick: () -> Unit
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(Tokens.RadiusM))
            .background(accent.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(Tokens.RadiusM))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White) }
}
