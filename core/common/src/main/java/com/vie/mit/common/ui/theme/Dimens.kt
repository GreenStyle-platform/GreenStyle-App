package com.vie.mit.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppDimens(
    // Screen Padding
    val screenPaddingSmall: Dp = 8.dp,
    val screenPaddingMedium: Dp = 16.dp,
    val screenPaddingLarge: Dp = 24.dp,

    // General Padding
    val paddingExtraSmall: Dp = 2.dp,
    val paddingSmall: Dp = 4.dp,
    val paddingNormal:Dp = 8.dp,
    val paddingMedium: Dp = 12.dp,
    val paddingLarge: Dp = 16.dp,
    val paddingExtraLarge: Dp = 24.dp,

    // Text Padding/Spacing
    val textPaddingSmall: Dp = 4.dp,
    val textPaddingMedium: Dp = 8.dp,
    val textPaddingLarge: Dp = 12.dp,

    // Button Dimensions
    val buttonPaddingHorizontal: Dp = 16.dp,
    val buttonPaddingVertical: Dp = 8.dp,
    val buttonPaddingSmall: Dp = 8.dp,
    val buttonHeightSmall: Dp = 32.dp,
    val buttonHeightMedium: Dp = 48.dp,
    val buttonHeightLarge: Dp = 56.dp,
    val buttonIconSize: Dp = 18.dp,
    val buttonIconSpacing: Dp = 8.dp,

    // Component Spacing
    val spacingNone: Dp = 0.dp,
    val spacingExtraSmall: Dp = 2.dp,
    val spacingSmall: Dp = 4.dp,
    val spacingMedium: Dp = 8.dp,
    val spacingLarge: Dp = 16.dp,
    val spacingExtraLarge: Dp = 24.dp,
    val spacingHuge: Dp = 32.dp,
    val spacingExtraHuge: Dp = 48.dp,
    val spacingMassive: Dp = 64.dp,

    // Radius
    val radiusSmall: Dp = 4.dp,
    val radiusMedium: Dp = 8.dp,
    val radiusLarge: Dp = 12.dp,
    val radiusExtraLarge: Dp = 16.dp,
    val radiusMax: Dp = 100.dp
)

val Dimens = AppDimens()

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
