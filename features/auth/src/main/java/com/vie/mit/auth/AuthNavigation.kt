package com.vie.mit.auth

interface AuthNavigation {
    fun navigateToLoginFromSplash()
    fun navigateToHomeFromSplash()
    fun navigateToHomeFromLogin()

    fun navigateToForgotPasswordFromLogin()

    fun navigateToSignUpFromLogin()

    fun navigateBack()

}
