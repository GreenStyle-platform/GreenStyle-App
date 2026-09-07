package com.vie.mit.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.vie.mit.data.local.datastore.security.EncryptionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(
    private val dataStore: DataStore<Preferences>, private val encryptionService: EncryptionService
) {
    fun getString(key: Preferences.Key<String>, defaultValue: String = ""): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    suspend fun saveString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun saveUserToken(token: String) {
        val encryptedToken = encryptionService.encrypt(token)
        if (encryptedToken != null) {
            saveString(PreferencesKeys.USER_TOKEN, encryptedToken)
        } else {
            throw SecurityException("Failed to encrypt token")
        }
    }

    suspend fun getUserTokenOnce(): String {
        val encryptedToken = getString(PreferencesKeys.USER_TOKEN).first()
        return if (encryptedToken.isNotEmpty()) {
            encryptionService.decrypt(encryptedToken) ?: ""
        } else {
            ""
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        val encryptedToken = encryptionService.encrypt(refreshToken)
        if (encryptedToken != null) {
            saveString(PreferencesKeys.REFRESH_TOKEN, encryptedToken)
        } else {
            throw SecurityException("Failed to encrypt refresh token")
        }
    }

    suspend fun getRefreshTokenOnce(): String {
        val encryptedToken = getString(PreferencesKeys.REFRESH_TOKEN).first()
        return if (encryptedToken.isNotEmpty()) {
            encryptionService.decrypt(encryptedToken) ?: ""
        } else {
            ""
        }
    }

    suspend fun saveExpiresTime(expiresAt: Long) {
        val encryptedTime = encryptionService.encrypt(expiresAt.toString())
        if (encryptedTime != null) {
            saveString(PreferencesKeys.EXPIRES_AT, encryptedTime)
        } else {
            throw SecurityException("Failed to encrypt expires time")
        }
    }

    fun getExpiresTime(): Flow<Long> {
        return getString(PreferencesKeys.EXPIRES_AT).map { encryptedTime ->
            if (encryptedTime.isNotEmpty()) {
                encryptionService.decrypt(encryptedTime)?.toLongOrNull() ?: 0L
            } else {
                0L
            }
        }
    }

    suspend fun hasSeenBeforeLoginOnboarding(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_BEFORE_LOGIN_ONBOARDING] ?: false
        }.first()
    }

    suspend fun updateOnboardingStatus(hasSeen: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_BEFORE_LOGIN_ONBOARDING] = hasSeen
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
