package com.vie.mit.data.network.interceptor

interface TokenProvider {
    suspend fun getAccessToken(): String?

    var cachedToken: String?
}