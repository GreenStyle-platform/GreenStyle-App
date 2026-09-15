package com.vie.mit.common.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.theme.GreenTheme
import com.vie.mit.common.ui.button.DebounceButton

@Composable
fun NoticeDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.scrim)
                .padding(AppTheme.dimens.screenPaddingLarge), contentAlignment = Alignment.Center
        ) {
            NoticeDialogContent(
                modifier = modifier,
                title = title,
                message = message,
                buttonText = buttonText,
                onDismiss = onDismiss
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeDialogPreview() {
    GreenTheme {
        NoticeDialog(
            title = "Thông báo",
            message = "Đây là nội dung thông báo quan trọng mà bạn cần phải chú ý.",
            buttonText = "Đóng",
            onDismiss = {})
    }
}

@Composable
fun NoticeDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(AppTheme.dimens.radiusExtraLarge),
        color = AppTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimens.paddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMedium))
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.secondaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.spacingExtraLarge))
            DebounceButton(
                modifier = Modifier.fillMaxWidth(), onClick = onDismiss
            ) {
                Text(
                    text = buttonText, style = AppTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeDialogContentPreview() {
    GreenTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.scrim)
                .padding(AppTheme.dimens.screenPaddingLarge), contentAlignment = Alignment.Center
        ) {
            NoticeDialogContent(
                title = "Thông báo",
                message = "Đây là nội dung thông báo quan trọng mà bạn cần phải chú ý.",
                buttonText = "Đóng",
                onDismiss = {})
        }
    }
}
