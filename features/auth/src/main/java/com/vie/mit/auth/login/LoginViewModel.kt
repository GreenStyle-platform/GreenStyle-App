package com.vie.mit.auth.login

import androidx.lifecycle.ViewModel
import com.vie.mit.auth.AuthNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authNavigation: AuthNavigation
) : ViewModel() {

    fun onLoginSuccess() {
        authNavigation.navigateToHome()
    }
}