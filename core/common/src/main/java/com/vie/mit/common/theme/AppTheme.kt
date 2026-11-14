package com.vie.mit.common.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColorScheme = staticCompositionLocalOf { lightAppColors() }
val LocalAppTypography = staticCompositionLocalOf { Typography }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColorScheme.current

    val typography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val dimens: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current
}

@Composable
fun GreenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = lightAppColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColorScheme provides appColors,
        LocalAppTypography provides Typography,
        LocalAppDimens provides Dimens
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = appColors.primary,
                onPrimary = appColors.onPrimary,
                surface = appColors.surface,
                onSurface = appColors.onSurface,
                onSurfaceVariant = appColors.secondaryText,
                outline = appColors.outline,
                error = appColors.error
            ),
            typography = Typography,
            content = content
        )
    }
}
