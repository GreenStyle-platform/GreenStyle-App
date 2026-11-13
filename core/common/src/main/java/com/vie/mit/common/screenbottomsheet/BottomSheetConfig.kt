package com.vie.mit.common.screenbottomsheet

import androidx.compose.runtime.Immutable

@Immutable
data class BottomSheetConfig(
    val initialPeekRatio: Float = 0.25f,
    val minSheetRatio: Float = 0.12f,
    val mapScaleLimitRatio: Float = 0.65f,
    val maxExpandedRatio: Float = 0.95f
)