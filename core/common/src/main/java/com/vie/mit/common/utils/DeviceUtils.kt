package com.vie.mit.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * Data class chứa thông số kích thước và độ phân giải màn hình.
 */
data class ScreenInfo(
    val widthPx: Int,
    val heightPx: Int,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val densityDpi: Int,
    val scaledDensity: Float
)

/**
 * Data class chứa thông tin tổng quan của thiết bị.
 */
data class DeviceInfo(
    val deviceId: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val sdkInt: Int,
    val osVersion: String,
    val screenInfo: ScreenInfo
)

/**
 * Utility hỗ trợ lấy các thông số từ thiết bị:
 * - Device ID (Android ID)
 * - Mật độ điểm ảnh (density, densityDpi, scaledDensity)
 * - Chiều rộng, chiều dài màn hình (pixel và dp)
 * - Thông số phần cứng và hệ điều hành (brand, model, manufacturer, sdkInt,...)
 */
object DeviceUtils {

    /**
     * Lấy Android ID định danh thiết bị (Settings.Secure.ANDROID_ID).
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
    }

    /**
     * Lấy mật độ điểm ảnh màn hình dạng hệ số tỉ lệ (ví dụ: 1.0, 1.5, 2.0, 3.0, 4.0).
     */
    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    /**
     * Lấy mật độ điểm ảnh màn hình theo đơn vị DPI (dots-per-inch, ví dụ: 160, 240, 320, 480).
     */
    fun getScreenDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * Lấy hệ số tỉ lệ font chữ (scaled density cho đơn vị sp).
     */
    @Suppress("DEPRECATION")
    fun getScreenScaledDensity(context: Context): Float {
        return context.resources.displayMetrics.scaledDensity
    }

    /**
     * Lấy chiều rộng màn hình theo đơn vị Pixel (px).
     * Hỗ trợ tương thích cả Android 11+ (API 30+) và các phiên bản thấp hơn.
     */
    fun getScreenWidthPx(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            context.resources.displayMetrics.widthPixels
        }
    }

    /**
     * Lấy chiều cao màn hình theo đơn vị Pixel (px).
     * Hỗ trợ tương thích cả Android 11+ (API 30+) và các phiên bản thấp hơn.
     */
    fun getScreenHeightPx(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            context.resources.displayMetrics.heightPixels
        }
    }

    /**
     * Lấy chiều rộng màn hình theo đơn vị DP (dp).
     */
    fun getScreenWidthDp(context: Context): Int {
        val density = getScreenDensity(context)
        return if (density > 0f) {
            (getScreenWidthPx(context) / density).roundToInt()
        } else {
            context.resources.configuration.screenWidthDp
        }
    }

    /**
     * Lấy chiều cao màn hình theo đơn vị DP (dp).
     */
    fun getScreenHeightDp(context: Context): Int {
        val density = getScreenDensity(context)
        return if (density > 0f) {
            (getScreenHeightPx(context) / density).roundToInt()
        } else {
            context.resources.configuration.screenHeightDp
        }
    }

    /**
     * Lấy toàn bộ thông số màn hình và đóng gói vào [ScreenInfo].
     */
    @Suppress("DEPRECATION")
    fun getScreenInfo(context: Context): ScreenInfo {
        val displayMetrics = context.resources.displayMetrics
        val widthPx = getScreenWidthPx(context)
        val heightPx = getScreenHeightPx(context)
        val density = displayMetrics.density
        val densityDpi = displayMetrics.densityDpi
        val scaledDensity = displayMetrics.scaledDensity

        val widthDp = if (density > 0f) {
            (widthPx / density).roundToInt()
        } else {
            context.resources.configuration.screenWidthDp
        }

        val heightDp = if (density > 0f) {
            (heightPx / density).roundToInt()
        } else {
            context.resources.configuration.screenHeightDp
        }

        return ScreenInfo(
            widthPx = widthPx,
            heightPx = heightPx,
            widthDp = widthDp,
            heightDp = heightDp,
            density = density,
            densityDpi = densityDpi,
            scaledDensity = scaledDensity
        )
    }

    /**
     * Lấy toàn bộ thông tin thiết bị và đóng gói vào [DeviceInfo].
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(context),
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            sdkInt = Build.VERSION.SDK_INT,
            osVersion = Build.VERSION.RELEASE,
            screenInfo = getScreenInfo(context)
        )
    }
}

// =============================================================================
// Extension properties & functions on Context
// =============================================================================

/**
 * Lấy Device ID (Android ID).
 */
val Context.deviceId: String
    get() = DeviceUtils.getDeviceId(this)

/**
 * Lấy mật độ điểm ảnh màn hình (density scale).
 */
val Context.screenDensity: Float
    get() = DeviceUtils.getScreenDensity(this)

/**
 * Lấy mật độ điểm ảnh màn hình theo DPI.
 */
val Context.screenDensityDpi: Int
    get() = DeviceUtils.getScreenDensityDpi(this)

/**
 * Lấy chiều rộng màn hình theo Pixel (px).
 */
val Context.screenWidthPx: Int
    get() = DeviceUtils.getScreenWidthPx(this)

/**
 * Lấy chiều cao màn hình theo Pixel (px).
 */
val Context.screenHeightPx: Int
    get() = DeviceUtils.getScreenHeightPx(this)

/**
 * Lấy chiều rộng màn hình theo DP (dp).
 */
val Context.screenWidthDp: Int
    get() = DeviceUtils.getScreenWidthDp(this)

/**
 * Lấy chiều cao màn hình theo DP (dp).
 */
val Context.screenHeightDp: Int
    get() = DeviceUtils.getScreenHeightDp(this)

/**
 * Lấy thông tin chi tiết màn hình [ScreenInfo].
 */
val Context.screenInfo: ScreenInfo
    get() = DeviceUtils.getScreenInfo(this)

/**
 * Lấy thông tin chi tiết thiết bị [DeviceInfo].
 */
val Context.deviceInfo: DeviceInfo
    get() = DeviceUtils.getDeviceInfo(this)
