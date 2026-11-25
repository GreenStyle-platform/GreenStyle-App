package com.vie.mit.mapbox.util

import androidx.compose.ui.graphics.toArgb
import com.vie.mit.common.theme.AppColors

/**
 * Resolves @ColorInt text color corresponding to transit type from [AppColors].
 * Matches transit type identifiers:
 * - "BUS" -> busColor
 * - "SUBWAY", "METRO" -> metroColor
 * - "BIKE", "BICYCLE" -> bikeColor
 * - "RAIL", "TRAIN" -> railColor
 */
fun AppColors.getTransitTextColor(typeId: String): Int {
    val color = when (typeId.uppercase()) {
        "BUS" -> this.busColor
        "SUBWAY", "METRO" -> this.metroColor
        "BIKE", "BICYCLE", "BICYCLE_RENT" -> this.bikeColor
        "RAIL", "TRAIN" -> this.railColor
        else -> this.busColor
    }
    return color.toArgb()
}
