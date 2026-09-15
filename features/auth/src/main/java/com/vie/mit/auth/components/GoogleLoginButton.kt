package com.vie.mit.auth.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vie.mit.auth.R
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.ui.button.DebounceOutlinedButton

@Composable
fun GoogleLoginButton(onClick: () -> Unit) {
    DebounceOutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.onSurface
        ),
        contentPadding = PaddingValues(vertical = 12.dp),
        border = BorderStroke(0.5.dp, AppTheme.colors.outlineVariant),
        elevation = null
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google), contentDescription = "Gooogle Icon"
        )
        Spacer(modifier = Modifier.width(AppTheme.dimens.spacingSmall))
        Text(
            text = stringResource(R.string.continue_with_google),
            style = AppTheme.typography.labelLarge
        )
    }
}

@Preview
@Composable
fun PreviewGoogleLoginButton() {
    DebounceOutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.onSurface
        ),
        contentPadding = PaddingValues(vertical = 12.dp),
        border = BorderStroke(0.5.dp, AppTheme.colors.outlineVariant),
        elevation = null
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google), contentDescription = "Gooogle Icon"
        )
        Spacer(modifier = Modifier.width(AppTheme.dimens.spacingSmall))
        Text(
            text = stringResource(R.string.continue_with_google),
            style = AppTheme.typography.labelLarge
        )
    }
}