package com.vie.mit.data.repository.auth

import com.vie.mit.data.network.model.login.LoginResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isLoggedIn(): Flow<Boolean>
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun signUp(email: String, password: String): Result<LoginResponse>
    suspend fun logout(): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
}