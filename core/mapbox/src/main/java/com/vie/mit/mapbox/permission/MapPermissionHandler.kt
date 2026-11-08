package com.vie.mit.mapbox.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vie.mit.common.extension.openAppSettings
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.ui.button.DebounceButton
import com.vie.mit.common.ui.button.DebounceOutlinedButton
import timber.log.Timber

@Composable
fun MapPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    /**Tại sao lại dùng rememberUpdatedState ?
     * currentOnPermissionGranted là reference trỏ tới phiên bản mới nhất của callback onPermissionGranted.
     * */
    val currentOnPermissionGranted by rememberUpdatedState(onPermissionGranted)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)

    var showSettingsDialog by remember { mutableStateOf(value = false) }

    fun checkPermissionGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) || (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            showSettingsDialog = false
            currentOnPermissionGranted()
        } else {
            showSettingsDialog = true
            currentOnPermissionDenied()
        }
    }

    // Tự động kiểm tra lại quyền khi người dùng quay trở lại từ Cài đặt (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (checkPermissionGranted()) {
                    showSettingsDialog = false
                    currentOnPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (checkPermissionGranted()) {
            currentOnPermissionGranted()
        } else {
            Timber.d("PhucTH: Permission: permissionLauncher called")
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    if (showSettingsDialog) {
        PermissionSettingsDialog(
            onOpenSettings = {
                context.openAppSettings()
            },
            onDismiss = {
                showSettingsDialog = false
            }
        )
    }
}

// TODO: Thực hiện tạo 1 confirm dialog chung cho các project gồm nội dung và 2 button.

@Composable
private fun PermissionSettingsDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.scrim)
                .padding(AppTheme.dimens.screenPaddingLarge),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
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
                        text = "Cần cấp quyền vị trí",
                        style = AppTheme.typography.titleLarge,
                        color = AppTheme.colors.primaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(AppTheme.dimens.spacingMedium))
                    Text(
                        text = "Ứng dụng cần quyền truy cập vị trí để hiển thị bản đồ và vị trí hiện tại của bạn. Vui lòng mở Cài đặt để cấp quyền.",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(AppTheme.dimens.spacingExtraLarge))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DebounceOutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        ) {
                            Text(
                                text = "Hủy",
                                style = AppTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(AppTheme.dimens.spacingMedium))
                        DebounceButton(
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSettings
                        ) {
                            Text(
                                text = "Mở Cài đặt",
                                style = AppTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

