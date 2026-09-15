package com.vie.mit.green.homecontainer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme

@Composable
fun BottomNavigationItem(
    modifier: Modifier = Modifier, item: BottomNavItem, isSelected: Boolean, onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(
            enabled = true, onClick = onClick, indication = null, interactionSource = null
        ), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.label),
            tint = if (isSelected) AppTheme.colors.surfaceTint else AppTheme.colors.disabledColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(item.label),
            style = AppTheme.typography.labelMedium.copy(if (isSelected) AppTheme.colors.surfaceTint else AppTheme.colors.disabledColor)
        )
    }
}
