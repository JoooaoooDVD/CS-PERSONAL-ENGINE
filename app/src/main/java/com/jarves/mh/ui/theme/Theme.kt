package com.jarves.mh.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Pure Dark Minimal Claude-Code Palette (Preserving existing public names for compatibility)
val ClaudeTerracotta = Color(0xFFD97757)
val ClaudeDarkBg = Color(0xFF0C0C0E)
val ClaudeDarkSurface = Color(0xFF151518)
val ClaudeDarkCard = Color(0xFF1B1B1F)
val ClaudeDarkBorder = Color(0xFF26262C)
val ClaudeTextPrimary = Color(0xFFF4F4F6)
val ClaudeTextSecondary = Color(0xFF8E8E95)

val EngineCyan = ClaudeTerracotta
val EnginePlatinum = Color(0xFFE4E4E7)
val EngineEmerald = Color(0xFF4ADE80)
val EngineAmber = Color(0xFFFBBF24)
val EngineViolet = Color(0xFFA1A1AA)

val AntigravityBlue = ClaudeTerracotta
val AntigravityViolet = Color(0xFFA1A1AA)
val AntigravityAqua = Color(0xFF4ADE80)
val AntigravityBg = ClaudeDarkBg
val AntigravitySurface = ClaudeDarkSurface
val AntigravityCard = ClaudeDarkCard
val AntigravityBorder = ClaudeDarkBorder

val PocketOrange = ClaudeTerracotta
val PocketBlue = ClaudeTerracotta
val PocketGreen = Color(0xFF4ADE80)
val PocketBackground = ClaudeDarkBg
val PocketSurface = ClaudeDarkSurface
val PocketSurfaceVariant = ClaudeDarkCard
val PocketOutline = ClaudeDarkBorder

private val PureClaudeDarkColors = darkColorScheme(
    primary = ClaudeTerracotta,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2B1B15),
    onPrimaryContainer = Color(0xFFFFDDD1),
    secondary = Color(0xFFD4D4D8),
    onSecondary = Color(0xFF121215),
    secondaryContainer = Color(0xFF222227),
    onSecondaryContainer = Color(0xFFE4E4E7),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF132A1C),
    onTertiaryContainer = Color(0xFFBBF7D0),
    background = ClaudeDarkBg,
    onBackground = ClaudeTextPrimary,
    surface = ClaudeDarkSurface,
    onSurface = ClaudeTextPrimary,
    surfaceVariant = ClaudeDarkCard,
    onSurfaceVariant = ClaudeTextSecondary,
    surfaceContainerLowest = Color(0xFF070709),
    surfaceContainerLow = Color(0xFF111114),
    surfaceContainer = ClaudeDarkSurface,
    surfaceContainerHigh = ClaudeDarkCard,
    surfaceContainerHighest = Color(0xFF232329),
    outline = ClaudeDarkBorder,
    outlineVariant = Color(0xFF1E1E24),
    error = Color(0xFFF87171),
    onError = Color(0xFF2D0606),
    errorContainer = Color(0xFF351315),
    onErrorContainer = Color(0xFFFECACA),
)

private val PureAmoledBlackColors = PureClaudeDarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D10),
    surfaceVariant = Color(0xFF151519),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF09090B),
    surfaceContainer = Color(0xFF0E0E12),
)

enum class AppThemeMode { SYSTEM, DARK, AMOLED, LIGHT }

@Composable
fun PocketTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    antigravityMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Always pure dark theme per user requirement ("bus ekadam dark color theme rakho")
    val scheme = if (themeMode == AppThemeMode.AMOLED) PureAmoledBlackColors else PureClaudeDarkColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = scheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = scheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
