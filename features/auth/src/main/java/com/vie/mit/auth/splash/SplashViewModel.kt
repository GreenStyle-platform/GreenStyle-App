package com.vie.mit.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.auth.AuthNavigation
import com.vie.mit.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authNavigation: AuthNavigation,
    private val authRepository: AuthRepository
) : ViewModel() {
    init {
        checkLoggedIn()
    }

    private fun checkLoggedIn() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val isLoggedIn = authRepository.isLoggedIn().first()
            val navigate: () -> Unit = if (isLoggedIn) {
                {
                    authNavigation.navigateToHomeFromSplash()
                }
            } else {
                {
                    authNavigation.navigateToLoginFromSplash()
                }
            }
            val duration = 3.seconds - (System.currentTimeMillis() - startTime).milliseconds
            delay(duration)
            navigate.invoke()
        }
    }
}