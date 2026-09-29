package com.vie.mit.home.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class RideItem(
    val id: Int, val title: String, val destination: String, val price: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    Column {
        Text(
            modifier = Modifier.clickable(enabled = true, onClick = {
                viewModel.navigateToMainMap()
            }),
            text = "Dẫn đường",
        )
    }
}