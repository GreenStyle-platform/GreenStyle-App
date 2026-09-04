package com.vie.mit.network.api.auth

import com.vie.mit.network.model.LoginRequest
import com.vie.mit.network.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse
}