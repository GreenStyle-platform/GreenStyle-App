package com.vie.mit.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vie.mit.mapbox.MapView2DLocationPuck

@Composable
fun MapScreen(viewModel: MapViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        MapView2DLocationPuck()
    }
}
