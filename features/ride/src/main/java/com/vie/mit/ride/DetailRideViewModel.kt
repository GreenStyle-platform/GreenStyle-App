package com.vie.mit.ride

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vie.mit.common.navigation.Arguments
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailRideViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    init {
        val idRide = savedStateHandle.get<Int>(Arguments.ID_RIDE)
        Timber.tag("okela").d("phuc1: $idRide")
    }
}
