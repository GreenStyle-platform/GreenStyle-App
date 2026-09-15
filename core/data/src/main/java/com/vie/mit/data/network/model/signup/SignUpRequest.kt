package com.vie.mit.data.network.model.signup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(
    @SerialName("fullName") val userName: String,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("phone") val phoneNumber: String,
    @SerialName("confirmPassword") val confirmPassword: String
)