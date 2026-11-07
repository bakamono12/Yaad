package com.bakadev.yaad.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Saffron = Color(0xFFE8590C)
val SaffronSoft = Color(0xFFFFE8D6)
val Cream = Color(0xFFFFF7ED)
val Ink = Color(0xFF1F1A17)
val Muted = Color(0xFF6B625C)
val DoneGreen = Color(0xFF2B8A3E)
val WarnBg = Color(0xFFFFF3BF)
val Danger = Color(0xFFC92A2A)

private val Colors = lightColorScheme(
    primary = Saffron,
    onPrimary = Color.White,
    primaryContainer = SaffronSoft,
    onPrimaryContainer = Color(0xFF5C2400),
    secondary = DoneGreen,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EAE3),
    onSurfaceVariant = Muted,
    error = Danger,
)

@Composable
fun YaadTheme(content: @Composable () -> Unit) {
    val base = Typography()
    // Everything a size bigger than stock — easy to read without glasses.
    val type = base.copy(
        titleLarge = base.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        labelLarge = base.labelLarge.copy(fontSize = 18.sp),
    )
    MaterialTheme(colorScheme = Colors, typography = type, content = content)
}
