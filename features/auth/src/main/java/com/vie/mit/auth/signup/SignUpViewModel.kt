package com.vie.mit.auth.signup

import androidx.lifecycle.viewModelScope
import com.vie.mit.auth.AuthNavigation
import com.vie.mit.common.container.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authNavigation: AuthNavigation
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun signUp() {
        val currentState = _uiState.value
        if (currentState.username.isBlank() || currentState.email.isBlank() || 
            currentState.phone.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill all fields") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Simulating sign up call
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onBackClick() {
        authNavigation.navigateBack()
    }

    fun onLoginClick() {
        authNavigation.navigateToLoginFromSplash()
    }
}
