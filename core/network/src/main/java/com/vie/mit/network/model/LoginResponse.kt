package com.vie.mit.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("token") val token: String, @SerialName("refreshToken") val refreshToken: String
)