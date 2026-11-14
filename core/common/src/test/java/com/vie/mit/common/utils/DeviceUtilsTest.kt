package com.vie.mit.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceUtilsTest {

    @Test
    fun dpToPx_float_isCorrect() {
        val density = 2.0f
        val dp = 16.0f
        val px = DeviceUtils.dpToPx(density, dp)
        assertEquals(32.0f, px, 0.001f)
    }

    @Test
    fun dpToPx_int_isCorrect() {
        val density = 2.5f
        val dp = 10
        val px = DeviceUtils.dpToPx(density, dp)
        assertEquals(25, px)
    }

    @Test
    fun pxToDp_float_isCorrect() {
        val density = 3.0f
        val px = 90.0f
        val dp = DeviceUtils.pxToDp(density, px)
        assertEquals(30.0f, dp, 0.001f)
    }

    @Test
    fun pxToDp_int_isCorrect() {
        val density = 2.0f
        val px = 50
        val dp = DeviceUtils.pxToDp(density, px)
        assertEquals(25, dp)
    }

    @Test
    fun pxToDp_zeroDensity_returnsZero() {
        val density = 0.0f
        val px = 100.0f
        val dp = DeviceUtils.pxToDp(density, px)
        assertEquals(0.0f, dp, 0.001f)
    }
}
