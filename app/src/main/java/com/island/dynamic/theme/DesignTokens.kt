package com.island.dynamic.theme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Tokens {
    val Ink900 = Color(0xFF000000); val Ink700 = Color(0xFF0F172A)
    val Ink600 = Color(0xFF1E293B); val Ink500 = Color(0xFF334155)
    val Ink300 = Color(0xFF94A3B8)
    val NeonGreen = Color(0xFF4ADE80); val NeonBlue = Color(0xFF60A5FA)
    val NeonPurple = Color(0xFFA78BFA); val NeonPink = Color(0xFFF472B6)
    val NeonAmber = Color(0xFFFBBF24); val NeonRed = Color(0xFFF87171)
    val GlassWhite10 = Color(0x1AFFFFFF); val GlassWhite15 = Color(0x26FFFFFF)
    val GlassWhite25 = Color(0x40FFFFFF)
    val GlassDark10 = Color(0x1A000000); val GlassDark25 = Color(0x40000000)
    val GlassBorder = Color(0x33FFFFFF); val GlassBorderStrong = Color(0x66FFFFFF)
    val RadiusS = 12.dp; val RadiusM = 16.dp; val RadiusL = 24.dp
    val RadiusXL = 28.dp; val RadiusPill = 999.dp
    val SpaceXS = 4.dp; val SpaceS = 8.dp; val SpaceM = 16.dp
    val SpaceL = 24.dp; val SpaceXL = 32.dp
    const val DurFast = 180; const val DurBase = 280; const val DurSlow = 420
    const val SpringDamping = 0.78f; const val SpringStiffness = 380f
    const val SpringDampingSoft = 0.85f; const val SpringStiffnessSoft = 220f
}
enum class UiStyle { IPHONE, GLASS, CARD }
object StyleHolder { var current by mutableStateOf(UiStyle.GLASS) }
