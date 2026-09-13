package com.vie.mit.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.auth.AuthNavigation
import com.vie.mit.auth.R
import com.vie.mit.data.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authNavigation: AuthNavigation, private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<LoginEvent>()
    val event: SharedFlow<LoginEvent> = _event.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value
        if (uiState.value.isLoading) return
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(currentState.email, currentState.password)

            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                onLoginSuccess()
            }.onFailure { error ->
                Timber.e("PhucTH: Login Exception: ${error.message}")
                _uiState.update { it.copy(errorMessage = error.message ?: "Login failed") }
            }
        }
    }

    fun onForgotPassword() {
        authNavigation.navigateToForgotPasswordFromLogin()
    }

    fun onSignUp() {
        authNavigation.navigateToSignUpFromLogin()
    }

    fun onLoginSuccess() {
        authNavigation.navigateToHomeFromLogin()
        viewModelScope.launch {
            _event.emit(LoginEvent.ShowToast(R.string.login_success))
        }
    }
}