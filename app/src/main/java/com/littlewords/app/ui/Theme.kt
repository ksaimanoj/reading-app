package com.littlewords.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.littlewords.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B58), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCECE3), onPrimaryContainer = Color(0xFF163D32),
    secondary = Color(0xFF786244), secondaryContainer = Color(0xFFEDE3D0),
    background = Color(0xFFF8F5EE), onBackground = Color(0xFF202C27),
    surface = Color(0xFFF8F5EE), onSurface = Color(0xFF202C27),
    surfaceVariant = Color(0xFFECE9DF), onSurfaceVariant = Color(0xFF5D665E),
    surfaceContainer = Color(0xFFF0EDE4), surfaceContainerLow = Color(0xFFF2EFE6),
    outline = Color(0xFF78837A), outlineVariant = Color(0xFFD9DED3),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFADDCC3), onPrimary = Color(0xFF0D3528),
    primaryContainer = Color(0xFF214C3D), onPrimaryContainer = Color(0xFFCEEEDC),
    secondary = Color(0xFFD9C59E), secondaryContainer = Color(0xFF443D2E),
    background = Color(0xFF131D19), onBackground = Color(0xFFEEEFE4),
    surface = Color(0xFF131D19), onSurface = Color(0xFFEEEFE4),
    surfaceVariant = Color(0xFF29372F), onSurfaceVariant = Color(0xFFB8C6BA),
    surfaceContainer = Color(0xFF1E2A23), surfaceContainerLow = Color(0xFF19251F),
    outline = Color(0xFF859B8B), outlineVariant = Color(0xFF3A4C40),
)

@Composable fun LittleWordsTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 38.sp, lineHeight = 42.sp),
            titleLarge = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        ), content = content,
    )
}
