package com.vie.mit.common.container

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

abstract class BaseActivity<VM : BaseViewModel> : ComponentActivity() {
    lateinit var viewModel: VM

    abstract fun provideViewModel(): Class<VM>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[provideViewModel()]
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT
            ), navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setupView(savedInstanceState)
    }

    abstract fun setupView(savedInstanceState: Bundle?)
}