package com.vie.mit.network.interceptor

interface TokenProvider {
    suspend fun getAccessToken(): String?

    var cachedToken: String?
}