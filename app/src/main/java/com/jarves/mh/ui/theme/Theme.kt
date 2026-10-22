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

// Authentic Google Antigravity Cosmic Signature Palette
val AntigravityBlue = Color(0xFF5B8DEF)
val AntigravityViolet = Color(0xFF9B72FF)
val AntigravityAqua = Color(0xFF36E4DA)
val AntigravityBg = Color(0xFF0A0D14)
val AntigravitySurface = Color(0xFF111622)
val AntigravityCard = Color(0xFF171E2E)
val AntigravityBorder = Color(0xFF253047)

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

private val AntigravityCosmicDarkColors = darkColorScheme(
    primary = AntigravityBlue,
    onPrimary = Color(0xFF061024),
    primaryContainer = Color(0xFF182442),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = AntigravityViolet,
    onSecondary = Color(0xFF130B29),
    secondaryContainer = Color(0xFF231C3E),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = AntigravityAqua,
    onTertiary = Color(0xFF032826),
    tertiaryContainer = Color(0xFF0D2F33),
    onTertiaryContainer = Color(0xFFB8FFF9),
    background = AntigravityBg,
    onBackground = Color(0xFFEEF2FF),
    surface = AntigravitySurface,
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = AntigravityCard,
    onSurfaceVariant = Color(0xFF9AA8C7),
    surfaceContainerLowest = Color(0xFF06080E),
    surfaceContainerLow = Color(0xFF0D111B),
    surfaceContainer = Color(0xFF131927),
    surfaceContainerHigh = Color(0xFF192133),
    surfaceContainerHighest = Color(0xFF212B42),
    outline = AntigravityBorder,
    outlineVariant = Color(0xFF1C2438),
    error = Color(0xFFFF7B7B),
    onError = Color(0xFF2D0606),
    errorContainer = Color(0xFF3B1218),
    onErrorContainer = Color(0xFFFECACA),
)

private val AntigravityCosmicAmoledColors = AntigravityCosmicDarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0D16),
    surfaceVariant = Color(0xFF121827),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF070A12),
    surfaceContainer = Color(0xFF0E1320),
)

enum class AppThemeMode { SYSTEM, DARK, AMOLED, LIGHT }

@Composable
fun PocketTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    antigravityMode: Boolean = false,
    content: @Composable () -> Unit,
) {
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
