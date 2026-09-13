package com.vie.mit.data.network.util

import com.vie.mit.data.network.exception.toAppException
import timber.log.Timber

import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> handleApiCall(call: suspend () -> T): Result<T> = try {
    Result.success(call())
} catch (e: CancellationException) {
    // Phải ném lại lỗi này để cơ chế Cancel của Coroutine hoạt động đúng
    Timber.e("PhucTH: $e")
    throw e
} catch (e: Exception) {
    val appException = e.toAppException()
    Timber.d("PhucTH: handleApiCall: $appException")
    Result.failure(appException)
}
