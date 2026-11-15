package com.vie.mit.data.model.remote.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpResponse(
    @SerialName("is_active") val isActive: Boolean
)
