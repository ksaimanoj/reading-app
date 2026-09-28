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
    primary = Color(0xFF315F83), onPrimary = Color.White,
    primaryContainer = Color(0xFFCBDDEA), onPrimaryContainer = Color(0xFF254966),
    secondary = Color(0xFF315F83), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEEBF4), onSecondaryContainer = Color(0xFF254966),
    tertiary = Color(0xFF315F83), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDEEBF4), onTertiaryContainer = Color(0xFF254966),
    background = Color(0xFFF8F6F1), onBackground = Color(0xFF25262B),
    surface = Color(0xFFF8F6F1), onSurface = Color(0xFF25262B),
    surfaceVariant = Color(0xFFE4E7EB), onSurfaceVariant = Color(0xFF5B606B),
    surfaceDim = Color(0xFFE0E2E5), surfaceBright = Color(0xFFF8F6F1),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F2ED),
    surfaceContainer = Color(0xFFF0EEE9), surfaceContainerHigh = Color(0xFFE8E9E8),
    surfaceContainerHighest = Color(0xFFDEE3E9),
    outline = Color(0xFF747E89), outlineVariant = Color(0xFFD8D8D8),
    inverseSurface = Color(0xFF2D333C), inverseOnSurface = Color(0xFFF5F3EF),
    inversePrimary = Color(0xFF9AC9E5), surfaceTint = Color(0xFF315F83),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AC9E5), onPrimary = Color(0xFF172C3C),
    primaryContainer = Color(0xFF385263), onPrimaryContainer = Color(0xFFE5F4FD),
    secondary = Color(0xFF9AC9E5), onSecondary = Color(0xFF172C3C),
    secondaryContainer = Color(0xFF30495B), onSecondaryContainer = Color(0xFFE5F4FD),
    tertiary = Color(0xFF9AC9E5), onTertiary = Color(0xFF172C3C),
    tertiaryContainer = Color(0xFF30495B), onTertiaryContainer = Color(0xFFE5F4FD),
    background = Color(0xFF171A22), onBackground = Color(0xFFF5F3EF),
    surface = Color(0xFF171A22), onSurface = Color(0xFFF5F3EF),
    surfaceVariant = Color(0xFF303947), onSurfaceVariant = Color(0xFFBBC3CF),
    surfaceDim = Color(0xFF171A22), surfaceBright = Color(0xFF343B47),
    surfaceContainerLowest = Color(0xFF11151C), surfaceContainerLow = Color(0xFF1D232D),
    surfaceContainer = Color(0xFF242A34), surfaceContainerHigh = Color(0xFF2C3440),
    surfaceContainerHighest = Color(0xFF3D4653),
    outline = Color(0xFF718094), outlineVariant = Color(0xFF485566),
    inverseSurface = Color(0xFFE6E9ED), inverseOnSurface = Color(0xFF25262B),
    inversePrimary = Color(0xFF315F83), surfaceTint = Color(0xFF9AC9E5),
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
