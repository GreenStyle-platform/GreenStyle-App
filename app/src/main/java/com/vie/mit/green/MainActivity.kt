package com.vie.mit.green

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.vie.mit.common.base.BaseActivity
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import com.vie.mit.common.ui.theme.GreenTheme
import com.vie.mit.green.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var appNavigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    appNavigator.navigationEvents.collect { event ->
                        when (event) {
                            is NavigationEvent.NavigateTo -> {
                                navController.navigate(event.route, event.builder)
                            }
                            is NavigationEvent.NavigateUp -> {
                                navController.navigateUp()
                            }
                            is NavigationEvent.PopBackStack -> {
                                navController.popBackStack(
                                    event.route ?: return@collect,
                                    event.inclusive
                                )
                            }
                        }
                    }
                }
            }

            GreenTheme {
                AppNavHost(navController = navController)
            }
        }
    }
}
