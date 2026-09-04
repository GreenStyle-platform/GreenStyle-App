package com.vie.mit.auth.splash

import androidx.lifecycle.ViewModel
import com.vie.mit.auth.AuthNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authNavigation: AuthNavigation
) : ViewModel() {

    fun onSplashFinish() {
        authNavigation.navigateToLogin()
    }
}