package com.dualnumber.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.dualnumber.app.data.Line

private val LightColors = lightColorScheme(
    primary = Color(0xFF16181D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EBEF),
    onPrimaryContainer = Color(0xFF16181D),
    secondary = Color(0xFF3B5BDB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDFF),
    onSecondaryContainer = Color(0xFF233A99),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF16181D),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F3F6),
    surfaceContainerHighest = Color(0xFFE9EBEF),
    outline = Color(0xFFD5D9DF),
    outlineVariant = Color(0xFFE6E8EC),
    error = Color(0xFFE03131),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF1F3F5),
    onPrimary = Color(0xFF0F1115),
    primaryContainer = Color(0xFF2A2F38),
    onPrimaryContainer = Color(0xFFF1F3F5),
    secondary = Color(0xFF8CA2FF),
    onSecondary = Color(0xFF0F1115),
    secondaryContainer = Color(0xFF1F2A52),
    onSecondaryContainer = Color(0xFFDCE3FF),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFECEEF1),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFECEEF1),
    surfaceVariant = Color(0xFF1D2128),
    onSurfaceVariant = Color(0xFF9AA1AC),
    surfaceContainerLowest = Color(0xFF171A20),
    surfaceContainerLow = Color(0xFF171A20),
    surfaceContainer = Color(0xFF1A1D23),
    surfaceContainerHigh = Color(0xFF22262E),
    surfaceContainerHighest = Color(0xFF2A2F38),
    outline = Color(0xFF353A44),
    outlineVariant = Color(0xFF262A32),
    error = Color(0xFFFF6B6B),
)

/** 기본번호(파랑) · 듀얼번호(초록) 색상과 라벨 */
@Immutable
data class LineStyle(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
    val label: String,     // 기본 / 듀얼
    val longLabel: String, // 기본번호 / 듀얼번호
    val via: String,       // 기본으로 / 듀얼로
)

@Composable
fun lineStyle(line: Line): LineStyle {
    val dark = isSystemInDarkTheme()
    return when (line) {
        Line.MAIN -> if (dark) {
            LineStyle(Color(0xFF8CA2FF), Color(0xFF0F1115), Color(0xFF1F2A52), Color(0xFFDCE3FF), "기본", "기본번호", "기본으로")
        } else {
            LineStyle(Color(0xFF3B5BDB), Color.White, Color(0xFFE8EDFF), Color(0xFF233A99), "기본", "기본번호", "기본으로")
        }
        Line.DUAL -> if (dark) {
            LineStyle(Color(0xFF5FD4A8), Color(0xFF0F1115), Color(0xFF0F3B2E), Color(0xFFCFF5E6), "듀얼", "듀얼번호", "듀얼로")
        } else {
            LineStyle(Color(0xFF0C9A6C), Color.White, Color(0xFFDDF6EC), Color(0xFF07624A), "듀얼", "듀얼번호", "듀얼로")
        }
    }
}

@Composable
fun DualTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
