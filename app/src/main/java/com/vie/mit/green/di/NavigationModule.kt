package com.vie.mit.green.di

import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.green.navigation.NavigationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    @Binds
    @Singleton
    abstract fun bindAppNavigator(navigationManager: NavigationManager): AppNavigator
}
