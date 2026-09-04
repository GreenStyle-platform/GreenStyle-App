package com.vie.mit.green

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.vie.mit.common.container.BaseActivity
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import com.vie.mit.common.ui.theme.GreenTheme
import com.vie.mit.green.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel>() {
    @Inject
    lateinit var appNavigator: AppNavigator

    override fun provideViewModel(): Class<MainViewModel> {
        return MainViewModel::class.java
    }

    override fun setupView(savedInstanceState: Bundle?) {
        setContent {
            val navController = rememberNavController()

            LaunchedEffect(navController, appNavigator) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    for (event in appNavigator.navigationChannel) {
                        when (event) {
                            is NavigationEvent.NavigateTo -> {
                                navController.navigate(event.route, event.builder)
                            }

                            is NavigationEvent.NavigateUp -> {
                                navController.navigateUp()
                            }

                            is NavigationEvent.PopBackStack -> {
                                val targetRoute = event.route
                                if (targetRoute != null) {
                                    navController.popBackStack(targetRoute, event.inclusive)
                                } else {
                                    navController.popBackStack()
                                }
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