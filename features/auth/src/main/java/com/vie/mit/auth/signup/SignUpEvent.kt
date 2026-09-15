package com.vie.mit.auth.signup

import androidx.annotation.StringRes

sealed interface SignUpEvent {
    data class ShowToast(@param:StringRes val messageId: Int) : SignUpEvent
}