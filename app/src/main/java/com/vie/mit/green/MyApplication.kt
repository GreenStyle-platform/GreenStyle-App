package com.vie.mit.green

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import android.content.pm.ApplicationInfo

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            // Sử dụng Custom DebugTree để hiển thị file và số dòng
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    // Trả về Tag có định dạng kèm (FileName.kt:LineNumber) để có thể click được trên Logcat
                    return "${super.createStackElementTag(element)} - (${element.fileName}:${element.lineNumber})"
                }
            })
        }
    }
}