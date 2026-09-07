package com.vie.mit.data.network.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token: String? = tokenProvider.cachedToken ?: runBlocking {
            tokenProvider.getAccessToken()
        }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader(
                    "Authorization", "Bearer $token"
                )
            }
        }.build()
        return chain.proceed(request)
    }
}