package com.vie.mit.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
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
 * - Chuyển đổi qua lại giữa dp, px và sp
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
     * Chuyển đổi từ DP sang Pixel (px) dạng Float với mật độ điểm ảnh (density) cho trước.
     */
    fun dpToPx(density: Float, dp: Float): Float {
        return dp * density
    }

    /**
     * Chuyển đổi từ DP sang Pixel (px) dạng Int (làm tròn) với mật độ điểm ảnh (density) cho trước.
     */
    fun dpToPx(density: Float, dp: Int): Int {
        return (dp * density).roundToInt()
    }

    /**
     * Chuyển đổi từ Pixel (px) sang DP dạng Float với mật độ điểm ảnh (density) cho trước.
     */
    fun pxToDp(density: Float, px: Float): Float {
        return if (density > 0f) px / density else 0f
    }

    /**
     * Chuyển đổi từ Pixel (px) sang DP dạng Int (làm tròn) với mật độ điểm ảnh (density) cho trước.
     */
    fun pxToDp(density: Float, px: Int): Int {
        return if (density > 0f) (px / density).roundToInt() else 0
    }

    /**
     * Chuyển đổi từ DP sang Pixel (px) dạng Float.
     */
    fun dpToPx(context: Context, dp: Float): Float {
        return dpToPx(getScreenDensity(context), dp)
    }

    /**
     * Chuyển đổi từ DP sang Pixel (px) dạng Int (làm tròn).
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return dpToPx(getScreenDensity(context), dp)
    }

    /**
     * Chuyển đổi từ Pixel (px) sang DP dạng Float.
     */
    fun pxToDp(context: Context, px: Float): Float {
        return pxToDp(getScreenDensity(context), px)
    }

    /**
     * Chuyển đổi từ Pixel (px) sang DP dạng Int (làm tròn).
     */
    fun pxToDp(context: Context, px: Int): Int {
        return pxToDp(getScreenDensity(context), px)
    }

    /**
     * Chuyển đổi từ SP sang Pixel (px) dạng Float (cho font size).
     */
    fun spToPx(context: Context, sp: Float): Float {
        return sp * getScreenScaledDensity(context)
    }

    /**
     * Chuyển đổi từ Pixel (px) sang SP dạng Float (cho font size).
     */
    fun pxToSp(context: Context, px: Float): Float {
        val scaledDensity = getScreenScaledDensity(context)
        return if (scaledDensity > 0f) px / scaledDensity else 0f
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
 * Chuyển đổi từ DP sang Pixel (px) dạng Float.
 */
fun Context.dpToPx(dp: Float): Float = DeviceUtils.dpToPx(this, dp)

/**
 * Chuyển đổi từ DP sang Pixel (px) dạng Int (làm tròn).
 */
fun Context.dpToPx(dp: Int): Int = DeviceUtils.dpToPx(this, dp)

/**
 * Chuyển đổi từ Pixel (px) sang DP dạng Float.
 */
fun Context.pxToDp(px: Float): Float = DeviceUtils.pxToDp(this, px)

/**
 * Chuyển đổi từ Pixel (px) sang DP dạng Int (làm tròn).
 */
fun Context.pxToDp(px: Int): Int = DeviceUtils.pxToDp(this, px)

/**
 * Chuyển đổi từ SP sang Pixel (px) dạng Float.
 */
fun Context.spToPx(sp: Float): Float = DeviceUtils.spToPx(this, sp)

/**
 * Chuyển đổi từ Pixel (px) sang SP dạng Float.
 */
fun Context.pxToSp(px: Float): Float = DeviceUtils.pxToSp(this, px)

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

// =============================================================================
// Extension functions & properties on Int & Float (dp / px / sp conversion)
// =============================================================================

/**
 * Chuyển đổi Int (dp) sang Pixel (px) dạng Int dùng [Context].
 */
fun Int.dpToPx(context: Context): Int = DeviceUtils.dpToPx(context, this)

/**
 * Chuyển đổi Float (dp) sang Pixel (px) dạng Float dùng [Context].
 */
fun Float.dpToPx(context: Context): Float = DeviceUtils.dpToPx(context, this)

/**
 * Chuyển đổi Int (px) sang DP dạng Int dùng [Context].
 */
fun Int.pxToDp(context: Context): Int = DeviceUtils.pxToDp(context, this)

/**
 * Chuyển đổi Float (px) sang DP dạng Float dùng [Context].
 */
fun Float.pxToDp(context: Context): Float = DeviceUtils.pxToDp(context, this)

/**
 * Extension thuộc tính chuyển Float (dp) sang Pixel (px) dạng Float sử dụng DisplayMetrics hệ thống.
 */
val Float.dpPx: Float
    get() = this * Resources.getSystem().displayMetrics.density

/**
 * Extension thuộc tính chuyển Int (dp) sang Pixel (px) dạng Int sử dụng DisplayMetrics hệ thống.
 */
val Int.dpPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

/**
 * Extension thuộc tính chuyển Float (px) sang DP dạng Float sử dụng DisplayMetrics hệ thống.
 */
val Float.pxDp: Float
    get() {
        val density = Resources.getSystem().displayMetrics.density
        return if (density > 0f) this / density else 0f
    }

/**
 * Extension thuộc tính chuyển Int (px) sang DP dạng Int sử dụng DisplayMetrics hệ thống.
 */
val Int.pxDp: Int
    get() {
        val density = Resources.getSystem().displayMetrics.density
        return if (density > 0f) (this / density).roundToInt() else 0
    }
