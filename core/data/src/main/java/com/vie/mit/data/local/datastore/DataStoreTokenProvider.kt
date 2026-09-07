package com.vie.mit.data.local.datastore

import com.vie.mit.data.network.interceptor.TokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreTokenProvider @Inject constructor(
    private val preferencesDatasource: Preferences,
) : TokenProvider {
    override suspend fun getAccessToken(): String? {
        val token = preferencesDatasource.getUserTokenOnce().ifEmpty { null }
        cachedToken = token
        return token
    }

    override var cachedToken: String? = null
}
