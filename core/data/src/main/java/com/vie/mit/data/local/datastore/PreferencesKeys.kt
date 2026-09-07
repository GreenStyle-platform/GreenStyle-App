package com.vie.mit.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val USER_TOKEN = stringPreferencesKey("user_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val EXPIRES_AT = stringPreferencesKey("expires_at")
    val HAS_SEEN_BEFORE_LOGIN_ONBOARDING = booleanPreferencesKey("has_seen_before_login_onboarding")
    val HAS_COMPLETED_AFTER_LOGIN_ONBOARDING =
        booleanPreferencesKey("has_completed_after_login_onboarding")
}
