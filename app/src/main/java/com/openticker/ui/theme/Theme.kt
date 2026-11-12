package com.openticker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF07070B)
val Night = Color(0xFF0C0C12)
val Surface1 = Color(0xFF15151C)
val Surface2 = Color(0xFF1E1E27)
val Surface3 = Color(0xFF2A2A36)
val Accent = Color(0xFFF2F2F5)
val AccentDeep = Color(0xFFB6B7C0)
val Mint = Color(0xFF35E6B6)
val Sky = Color(0xFF4CC9FF)
val Pink = Color(0xFFFF4D8D)
val Violet = Color(0xFF8B7BFF)
val TextMain = Color(0xFFF5F5F7)
val TextDim = Color(0xFF9C9CA8)
val Hairline = Color(0xFF2C2C38)

val DisplayFont = FontFamily.SansSerif
val MonoFont = FontFamily.Monospace

val TickerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp,
        color = TextMain
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
        color = TextMain
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.5).sp,
        color = TextMain
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        color = TextMain
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        color = TextMain
    ),
    bodyLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextMain
    ),
    bodyMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = TextDim
    ),
    labelSmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        color = TextDim
    )
)

private val TickerColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF0A0A0F),
    primaryContainer = AccentDeep,
    onPrimaryContainer = Color(0xFF0A0A0F),
    secondary = Mint,
    onSecondary = Color(0xFF001A12),
    tertiary = Pink,
    background = Night,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Hairline,
    error = Color(0xFFFF5A5A)
)

val TickerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun OpenTickerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TickerColorScheme,
        typography = TickerTypography,
        shapes = TickerShapes,
        content = content
    )
}
