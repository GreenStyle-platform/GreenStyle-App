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
            // Chèn (FileName.kt:LineNumber) vào đầu Log Message để Android Studio Logcat nhận diện đường dẫn (clickable link)
            Timber.plant(object : Timber.DebugTree() {
                private val elementThreadLocal = ThreadLocal<StackTraceElement>()

                override fun createStackElementTag(element: StackTraceElement): String {
                    elementThreadLocal.set(element)
                    return super.createStackElementTag(element) ?: "Debug"
                }

                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    val element = elementThreadLocal.get()
                    elementThreadLocal.remove()
                    val link = if (element != null) "(${element.fileName}:${element.lineNumber})" else ""
                    val formattedMessage = if (link.isNotEmpty()) "$link $message" else message
                    super.log(priority, tag, formattedMessage, t)
                }
            })
        }
    }
}