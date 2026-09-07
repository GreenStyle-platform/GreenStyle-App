package com.vie.mit.data.network.util

import com.vie.mit.data.network.exception.toAppException
import timber.log.Timber

suspend fun <T> handleApiCall(call: suspend () -> T): Result<T> = try {
    Result.success(call())
} catch (e: Exception) {
    Timber.e("handleApiCall: ${e}")
    Result.failure(e.toAppException())
}
