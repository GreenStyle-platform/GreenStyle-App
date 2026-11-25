package com.vie.mit.mapbox.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.mapbox.maps.extension.compose.ComposeMapInitOptions

/**
 * Neutral map-toned background shown behind TextureView-backed MapboxMap instances
 * while the first GL frame initializes, preventing transparent/gray flashes during transitions.
 */
val MapPlaceholderBackgroundColor: Color = Color(0xFFF3F4F6)

/**
 * Creates a remembered [ComposeMapInitOptions] configured with `textureView = true`.
 *
 * Using TextureView instead of the default SurfaceView eliminates Android's SurfaceView
 * punch-hole artifact and dual-SurfaceView overlap when transitioning between map screens
 * inside Jetpack Compose Navigation.
 */
@Composable
fun rememberTextureMapInitOptions(mapName: String = ""): ComposeMapInitOptions {
    val density = LocalDensity.current.density
    return remember(density, mapName) {
        ComposeMapInitOptions(
            pixelRatio = density,
            textureView = true,
            mapName = mapName,
        )
    }
}
