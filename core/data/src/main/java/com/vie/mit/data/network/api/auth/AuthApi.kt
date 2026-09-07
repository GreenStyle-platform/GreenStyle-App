package com.vie.mit.data.network.api.auth

import com.vie.mit.data.network.model.login.LoginRequest
import com.vie.mit.data.network.model.login.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @POST("auth/signup")
    suspend fun signUp(@Body loginRequest: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body email: String)
}