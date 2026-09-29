package com.vie.mit.mapbox

import android.app.Activity
import android.content.Context
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

class MapPermission(
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit,
    private val onExplanation: ((List<String>) -> Unit)? = null
) : PermissionsListener {
    private val permissionsManager: PermissionsManager = PermissionsManager(this)

    // Kiểm tra xem đã được cấp quyền vị trí hay chưa
    fun areLocationPermissionsGranted(context: Context): Boolean {
        return PermissionsManager.areLocationPermissionsGranted(context)
    }

    // Yêu cầu cấp quyền runtime
    fun requestLocationPermissions(activity: Activity) {
        if (areLocationPermissionsGranted(activity)) {
            onPermissionGranted()
        } else {
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    // Chuyển kết quả từ Activity vào PermissionsManager
    fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        onExplanation?.invoke(permissionsToExplain)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
}