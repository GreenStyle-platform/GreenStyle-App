package com.vie.mit.green.homecontainer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.vie.mit.green.navigation.main_graph.HomeTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeContainerViewModel @Inject constructor() : ViewModel() {
    private val _selectedTab = mutableStateOf<Any>(HomeTab)
    val selectedTab: State<Any> = _selectedTab

    fun onTabSelected(tab: Any) {
        _selectedTab.value = tab
    }
}
