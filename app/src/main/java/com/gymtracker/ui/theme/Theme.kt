package com.gymtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymtracker.settings.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = GymAccent,
    onPrimary = GymOnAccent,
    secondary = GymAccent,
    onSecondary = GymOnAccent,
    background = GymBg,
    onBackground = GymText,
    surface = GymPanel,
    onSurface = GymText,
    surfaceVariant = GymPanel2,
    onSurfaceVariant = GymMuted,
    outline = GymLine,
    error = GymDanger,
    onError = GymOnAccent,
)

private val LightScheme = lightColorScheme(
    primary = GymAccentDark,
    onPrimary = GymOnAccent,
    secondary = GymAccentDark,
    onSecondary = GymOnAccent,
    background = LightBg,
    onBackground = LightText,
    surface = LightPanel,
    onSurface = LightText,
    surfaceVariant = LightPanel2,
    onSurfaceVariant = LightMuted,
    outline = LightLine,
    error = GymDanger,
)

@Composable
fun GymTrackerTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalGymPalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = GymTypography,
            content = content,
        )
    }
}


val GymTypography = androidx.compose.material3.Typography(
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

/** Палитра по мокапу: панели, поля, акцент — точные цвета макета. */
data class GymPalette(
    val panel: Color,
    val panel2: Color,
    val field: Color,
    val photo: Color,
    val line: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val dangerBorder: Color,
    val text: Color,
    /** Оранжевый для информационных действий; акцент остаётся основным. */
    val info: Color,
)

val DarkPalette = GymPalette(
    panel = GymPanel,
    panel2 = GymPanel2,
    field = GymField,
    photo = GymPhoto,
    line = GymLine,
    muted = GymMuted,
    accent = GymAccent,
    onAccent = GymOnAccent,
    danger = GymDanger,
    dangerBorder = GymDangerBorder,
    text = GymText,
    info = GymInfo,
)

val LightPalette = GymPalette(
    panel = LightPanel,
    panel2 = LightPanel2,
    field = LightField,
    photo = LightPanel2,
    line = LightLine,
    muted = LightMuted,
    accent = GymAccentDark,
    onAccent = GymOnAccent,
    danger = GymDanger,
    dangerBorder = GymDangerBorder,
    text = LightText,
    info = GymInfo,
)

val LocalGymPalette = compositionLocalOf { DarkPalette }
