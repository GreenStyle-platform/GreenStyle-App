package com.vie.mit.data.network.exception

import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

sealed class AppException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class NetworkException(message: String?, cause: Throwable?) : AppException(message, cause)
    class UnauthorizedException(message: String?, cause: Throwable?) : AppException(message, cause)
    class NotFoundException(message: String?, cause: Throwable?) : AppException(message, cause)
    class ServerException(message: String?, cause: Throwable?) : AppException(message, cause)
    class ApiException(code:Int,message: String?, cause: Throwable?) : AppException(message, cause)

    class UnknownException(message: String?, cause: Throwable?) : AppException(message, cause)
}

fun Throwable.toAppException(): AppException {
    return when (this) {
        is AppException -> this
        is IOException -> AppException.NetworkException(
            "Network error occurred", this
        )

        is HttpException -> {
            val code = code()
            // Hàm message() của HttpException chỉ trả về HTTP status message (VD: "Bad Request").
            var errorMessage = message()
            Timber.d("PhucTH: $errorMessage")
            // Đọc errorBody từ response để lấy thông điệp JSON do Server trả về
            val errorBodyString = response()?.errorBody()?.string()
            if (!errorBodyString.isNullOrEmpty()) {
                try {
                    // Dùng JSONObject để parse chuỗi JSON và lấy ra trường "message"
                    val jsonObject = org.json.JSONObject(errorBodyString)
                    if (jsonObject.has("message")) {
                        errorMessage = jsonObject.getString("message")
                    }
                    Timber.d("PhucTH: $errorBodyString")
                } catch (e: Exception) {
                    Timber.e("PhucTH: Lỗi parse JSON error: $e")
                }
            }

            Timber.tag("PhucTH").d("code: $code , message: $errorMessage")
            when (code) {
                401 -> AppException.UnauthorizedException(errorMessage, this)
                404 -> AppException.NotFoundException(errorMessage, this)
                in 500..599 -> AppException.ServerException(errorMessage, this)
                else -> {
                    Timber.tag("PhucTH").e("code: $code - message: $errorMessage")
                    AppException.ApiException(code,errorMessage, this)
                }
            }
        }

        else -> AppException.UnknownException(message ?: "Unknown error occurred", this)
    }
}
