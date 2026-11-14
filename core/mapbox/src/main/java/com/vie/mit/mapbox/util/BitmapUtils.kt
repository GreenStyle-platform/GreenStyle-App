package com.vie.mit.mapbox.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Converts a drawable resource (including VectorDrawable XML) into a [Bitmap].
 */
fun bitmapFromDrawableRes(
    context: Context,
    @DrawableRes resourceId: Int,
    targetWidth: Int? = null,
    targetHeight: Int? = null,
): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, resourceId) ?: return null
    val width = targetWidth ?: drawable.intrinsicWidth.takeIf { it > 0 } ?: 72
    val height = targetHeight ?: drawable.intrinsicHeight.takeIf { it > 0 } ?: 72
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

