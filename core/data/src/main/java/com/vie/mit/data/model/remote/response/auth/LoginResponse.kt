package com.vie.mit.data.model.remote.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("token") val token: String
)
