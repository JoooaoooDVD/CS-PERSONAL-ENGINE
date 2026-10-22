package com.jarves.mh.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// CS Personal Engine — Obsidian Dark-Grey & Pitch-Black Signature Palette
val EngineCyan = Color(0xFF38BDF8)
val EnginePlatinum = Color(0xFFE2E8F0)
val EngineEmerald = Color(0xFF34D399)
val EngineAmber = Color(0xFFF59E0B)
val EngineViolet = Color(0xFF818CF8)

// Retained aliases used across UI files, upgraded to Obsidian/Titanium aesthetics
val PocketOrange = Color(0xFF38BDF8)
val PocketBlue = Color(0xFF818CF8)
val PocketGreen = Color(0xFF34D399)
val PocketBackground = Color(0xFF090A0D)
val PocketSurface = Color(0xFF111318)
val PocketSurfaceVariant = Color(0xFF191C24)
val PocketOutline = Color(0xFF282D3A)

private val ObsidianDarkColors = darkColorScheme(
    primary = EngineCyan,
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFF132536),
    onPrimaryContainer = Color(0xFFD6F1FF),
    secondary = EnginePlatinum,
    onSecondary = Color(0xFF111318),
    secondaryContainer = Color(0xFF1F2430),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = EngineEmerald,
    onTertiary = Color(0xFF022C1E),
    tertiaryContainer = Color(0xFF0D2E24),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF090A0D),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF191C24),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF050608),
    surfaceContainerLow = Color(0xFF0D0F14),
    surfaceContainer = Color(0xFF13161D),
    surfaceContainerHigh = Color(0xFF1A1E27),
    surfaceContainerHighest = Color(0xFF222733),
    outline = Color(0xFF2A303D),
    outlineVariant = Color(0xFF1E232E),
    error = Color(0xFFF87171),
    onError = Color(0xFF2D0606),
    errorContainer = Color(0xFF3B1215),
    onErrorContainer = Color(0xFFFECACA),
)

private val AmoledPitchBlackColors = darkColorScheme(
    primary = EngineCyan,
    onPrimary = Color(0xFF03111A),
    primaryContainer = Color(0xFF0F2030),
    onPrimaryContainer = Color(0xFFD6F1FF),
    secondary = EnginePlatinum,
    onSecondary = Color(0xFF0A0B0E),
    secondaryContainer = Color(0xFF171A21),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = EngineEmerald,
    onTertiary = Color(0xFF022C1E),
    tertiaryContainer = Color(0xFF09261D),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0B0D11),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF14171F),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF07080B),
    surfaceContainer = Color(0xFF0E1016),
    surfaceContainerHigh = Color(0xFF151821),
    surfaceContainerHighest = Color(0xFF1D212D),
    outline = Color(0xFF232834),
    outlineVariant = Color(0xFF171B24),
    error = Color(0xFFF87171),
    onError = Color(0xFF2D0606),
    errorContainer = Color(0xFF350E11),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF082F49),
    secondary = Color(0xFF334155),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
)

enum class AppThemeMode { SYSTEM, DARK, AMOLED, LIGHT }

@Composable
fun PocketTheme(themeMode: AppThemeMode = AppThemeMode.DARK, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val scheme = when (themeMode) {
        AppThemeMode.DARK -> ObsidianDarkColors
        AppThemeMode.AMOLED -> AmoledPitchBlackColors
        AppThemeMode.LIGHT -> LightColors
        AppThemeMode.SYSTEM -> if (systemDark) ObsidianDarkColors else LightColors
    }
    val isDark = themeMode != AppThemeMode.LIGHT && (themeMode != AppThemeMode.SYSTEM || systemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = scheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = scheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
