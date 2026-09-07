package com.vie.mit.auth.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.vie.mit.auth.R
import com.vie.mit.common.ui.theme.AppTheme

@Composable
fun SplashScreen(
    viewModel: SplashViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.4f))
        Image(
            painter = painterResource(id = R.drawable.logo_app), contentDescription = "GreenGo"
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingSmall))
        Text(
            text = stringResource(com.vie.mit.common.R.string.app_name),
            style = AppTheme.typography.displaySmall
        )
        Spacer(Modifier.weight(0.6f))
    }
}

@Preview
@Composable
fun PreviewSplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier.padding(paddingValues = PaddingValues(horizontal = AppTheme.dimens.paddingMedium)),
            painter = painterResource(id = R.drawable.logo_app),
            contentDescription = "GreenGo"
        )
        Spacer(modifier = Modifier.height(AppTheme.dimens.spacingSmall))
        Text(
            text = stringResource(com.vie.mit.common.R.string.app_name),
            style = AppTheme.typography.displaySmall,
            color = AppTheme.colors.primary
        )
    }
}