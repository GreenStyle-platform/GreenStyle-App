package com.vie.mit.green.di

import com.vie.mit.auth.AuthNavigation
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.green.navigation.NavigationManager
import com.vie.mit.green.navigation.auth_graph.AuthGraphNavigator
import com.vie.mit.green.navigation.main_graph.MainGraphNavigator
import com.vie.mit.home.HomeNavigation
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

    @Binds
    @Singleton
    abstract fun bindAuthNavigation(authNavigator: AuthGraphNavigator): AuthNavigation

    @Binds
    @Singleton
    abstract fun bindHomeNavigation(mainNavigator: MainGraphNavigator): HomeNavigation
}
