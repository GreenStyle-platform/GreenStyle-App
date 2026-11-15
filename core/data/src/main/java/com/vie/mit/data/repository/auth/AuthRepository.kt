package com.vie.mit.data.repository.auth

import com.vie.mit.data.local.datastore.Preferences
import com.vie.mit.data.local.datastore.PreferencesKeys
import com.vie.mit.data.model.domain.auth.SignUpResult
import com.vie.mit.data.model.domain.auth.UserAuthToken
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.request.auth.LoginRequest
import com.vie.mit.data.model.remote.request.auth.SignUpRequest
import com.vie.mit.data.network.api.auth.AuthApi
import com.vie.mit.data.network.util.handleApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val preferences: Preferences,
) {
    fun isLoggedIn(): Flow<Boolean> {
        return preferences.getString(PreferencesKeys.USER_TOKEN).map { it.isNotEmpty() }
    }

    suspend fun login(email: String, password: String): Result<UserAuthToken> = handleApiCall {
        Timber.d("PhucTH: 123 $email - $password")
        val response = authApi.login(LoginRequest(email, password))
        Timber.d("PhucTH $response")
        preferences.saveUserToken(response.token)
        response.toDomain()
    }

    suspend fun signUp(
        email: String,
        password: String,
        userName: String,
        phoneNumber: String,
        confirmPassword: String,
    ): Result<SignUpResult> = handleApiCall {
        val response = authApi.signUp(
            SignUpRequest(
                userName = userName,
                email = email,
                password = password,
                phoneNumber = phoneNumber,
                confirmPassword = confirmPassword,
            ),
        )
        response.toDomain()
    }

    suspend fun logout(): Result<Unit> = handleApiCall {
        authApi.logout()
        preferences.clearAll()
    }

    suspend fun forgotPassword(email: String): Result<Unit> = handleApiCall {
        authApi.forgotPassword(email)
    }
}
