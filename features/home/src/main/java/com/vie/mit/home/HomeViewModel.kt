package com.vie.mit.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeNavigation: HomeNavigation
) : ViewModel() {
    fun onRideSelected(idRide: Int) {
        homeNavigation.navigateToDetailRide(idRide)
    }

    suspend fun fetchData() {
        val job: Job = Job()
        val coroutineContext: CoroutineContext = Dispatchers.IO + job
        val coroutineScope: CoroutineScope = CoroutineScope(coroutineContext)
        val job2: Job = coroutineScope.launch(start = CoroutineStart.LAZY) {}
        val deferred: Deferred<Int> = coroutineScope.async {
            delay(300.milliseconds)
            return@async 1
        }
        val result: Int = deferred.await()
    }

}