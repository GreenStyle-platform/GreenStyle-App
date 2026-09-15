package com.vie.mit.auth.login

import androidx.annotation.StringRes

sealed interface LoginEvent {
    data class ShowToast(@param:StringRes val messageId: Int) : LoginEvent
}
