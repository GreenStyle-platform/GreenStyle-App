package com.vie.mit.auth.signup

import androidx.lifecycle.viewModelScope
import com.vie.mit.auth.AuthNavigation
import com.vie.mit.auth.R
import com.vie.mit.common.container.BaseViewModel
import com.vie.mit.data.network.model.signup.SignUpResponse
import com.vie.mit.data.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authNavigation: AuthNavigation, private val authRepository: AuthRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()
    private val _event = MutableSharedFlow<SignUpEvent>()
    val event = _event.asSharedFlow()

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

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun signUp() {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        if (currentState.username.isBlank() || currentState.email.isBlank() || currentState.phone.isBlank() || currentState.password.isBlank() || currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill all fields") }
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val response: Result<SignUpResponse> = authRepository.signUp(
                email = currentState.email,
                password = currentState.password,
                userName = currentState.username,
                phoneNumber = currentState.phone,
                confirmPassword = currentState.confirmPassword
            )
            _uiState.update { it.copy(isLoading = false) }
            response.onSuccess { signUpResponse: SignUpResponse ->
                if (signUpResponse.isActive) {
                    _event.emit(SignUpEvent.ShowToast(R.string.sign_up_success))
                    authNavigation.navigateBack()
                    Timber.d(signUpResponse.toString())
                } else {
                    _event.emit(SignUpEvent.ShowToast(R.string.sign_up_failed))
                }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onBackClick() {
        authNavigation.navigateBack()
    }

    fun onLoginClick() {
        authNavigation.navigateToLoginFromSplash()
    }
}
