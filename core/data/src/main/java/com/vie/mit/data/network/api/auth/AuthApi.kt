package com.vie.mit.data.network.api.auth

import com.vie.mit.data.model.remote.request.auth.LoginRequest
import com.vie.mit.data.model.remote.request.auth.SignUpRequest
import com.vie.mit.data.model.remote.response.auth.LoginResponse
import com.vie.mit.data.model.remote.response.auth.SignUpResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @POST("users/register")
    suspend fun signUp(@Body signUpRequest: SignUpRequest): SignUpResponse

    @POST("auth/logout")
    suspend fun logout()

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body email: String)
}
