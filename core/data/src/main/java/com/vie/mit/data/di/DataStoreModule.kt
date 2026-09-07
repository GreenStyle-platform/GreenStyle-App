package com.vie.mit.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vie.mit.data.local.datastore.DataStoreTokenProvider
import com.vie.mit.data.local.datastore.security.EncryptionService
import com.vie.mit.data.local.datastore.security.KeystoreEncryptionService
import com.vie.mit.data.network.interceptor.TokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds
    @Singleton
    abstract fun bindEncryptionService(
        keystoreEncryptionService: KeystoreEncryptionService
    ): EncryptionService

    @Binds
    @Singleton
    abstract fun bindTokenProvider(
        dataStoreTokenProvider: DataStoreTokenProvider
    ): TokenProvider

    companion object {
        private const val USER_PREFERENCES = "user_preferences"

        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES) })
        }
    }
}