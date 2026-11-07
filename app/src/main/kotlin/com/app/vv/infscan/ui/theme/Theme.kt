package com.app.vv.infscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalInfColors = staticCompositionLocalOf { LightColors }
private val LocalInfTypography = staticCompositionLocalOf { InfTypography() }

object InfTheme {
    val colors: InfColors
        @Composable @ReadOnlyComposable get() = LocalInfColors.current
    val type: InfTypography
        @Composable @ReadOnlyComposable get() = LocalInfTypography.current
}

private fun InfColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = soft,
        onPrimaryContainer = onSoft,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = soft,
        onSecondaryContainer = onSoft,
        tertiary = accent,
        onTertiary = onAccent,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = inkSecondary,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
        surfaceContainerLow = background,
        surfaceContainerLowest = surface,
        outline = inkSecondary,
        outlineVariant = outline,
        error = error,
        onError = onError,
        scrim = scrim,
        inverseSurface = snackbar,
        inverseOnSurface = onSnackbar,
        inversePrimary = snackbarAction,
    )
}

@Composable
fun InfScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val type = InfTypography()
    CompositionLocalProvider(
        LocalInfColors provides colors,
        LocalInfTypography provides type,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = type.toMaterial(),
            content = content,
        )
    }
}
