package com.vie.mit.data.network.exception

import retrofit2.HttpException
import java.io.IOException

sealed class AppException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class NetworkException(message: String?, cause: Throwable?) : AppException(message, cause)
    class UnauthorizedException(message: String?, cause: Throwable?) : AppException(message, cause)
    class NotFoundException(message: String?, cause: Throwable?) : AppException(message, cause)
    class ServerException(message: String?, cause: Throwable?) : AppException(message, cause)
    class ApiException(val code: Int, message: String?, cause: Throwable?) : AppException(message, cause)
    class UnknownException(message: String?, cause: Throwable?) : AppException(message, cause)
}

fun Throwable.toAppException(): AppException {
    return when (this) {
        is AppException -> this
        is IOException -> AppException.NetworkException(
            "Network error occurred",
            this
        )
        is HttpException -> {
            val code = code()
            val message = message()
            when (code) {
                401 -> AppException.UnauthorizedException(message, this)
                404 -> AppException.NotFoundException(message, this)
                in 500..599 -> AppException.ServerException(message, this)
                else -> AppException.ApiException(code, message, this)
            }
        }
        else -> AppException.UnknownException(message ?: "Unknown error occurred", this)
    }
}
