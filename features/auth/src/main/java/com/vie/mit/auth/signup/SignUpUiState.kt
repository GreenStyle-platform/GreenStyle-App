package com.vie.mit.auth.signup

data class SignUpUiState(
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)
