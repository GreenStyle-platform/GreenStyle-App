package com.vie.mit.common.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryButtonBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val disabledColor: Color,
    val surface: Color,
    val surfaceDim: Color,
    val onSurface: Color,
    val outline: Color,
    val error: Color,
    val scrim: Color,
    val smallIconColor: Color,
    val success: Color,
    val isLight: Boolean = true,
    val navyBlue: Color,
    val overlayColor: Color,
    val backgroundGreenCard: Color,
    val black: Color,
    val white: Color
)

fun lightAppColors(): AppColors = AppColors(
    primary = GreenPrimary,
    onPrimary = SurfaceWhite,
    primaryButtonBackground = GreenPrimaryButton,
    primaryText = TextPrimary,
    secondaryText = TextSecondary,
    disabledColor = DisabledText,
    surface = SurfaceWhite,
    surfaceDim = SurfaceDim,
    onSurface = OnSurfaceBlack,
    outline = OutlineGray,
    error = ErrorRed,
    scrim = ScrimOverlay,
    smallIconColor = SmallIconBlue,
    success = SuccessGreen,
    isLight = true,
    navyBlue = NavyBlue,
    overlayColor = OverlayColor,
    backgroundGreenCard = GreenBackgroundCard,
    black = Color.Black,
    white = Color.White
)
