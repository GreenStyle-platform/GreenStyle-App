package com.vie.mit.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vie.mit.common.ui.theme.AppTheme

@Composable
fun AuthBackground() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.primary)
                .weight(0.6f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.surfaceDim)
                .weight(0.4f)
        )
    }
}
