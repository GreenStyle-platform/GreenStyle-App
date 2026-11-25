package com.vie.mit.green.ui.setting

import androidx.lifecycle.ViewModel
import com.vie.mit.common.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
) : ViewModel() {
    fun navigateBack() {
        appNavigator.navigateUp()
    }
}
