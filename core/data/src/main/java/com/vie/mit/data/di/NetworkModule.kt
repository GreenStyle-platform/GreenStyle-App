package com.vie.mit.data.di

import com.vie.mit.data.Constants
import com.vie.mit.data.network.api.auth.AuthApi
import com.vie.mit.data.network.api.geocode.GeocodeApi
import com.vie.mit.data.network.api.routeplanner.RoutePlannerApi
import com.vie.mit.data.network.api.transit.TransitRouteApi
import com.vie.mit.data.network.api.transit.TransitStopApi
import com.vie.mit.data.network.interceptor.AuthInterceptor
import com.vie.mit.data.network.interceptor.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider
    ): AuthInterceptor {
        return AuthInterceptor(tokenProvider)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient, json: Json
    ): Retrofit {
        return Retrofit.Builder().baseUrl(Constants.BASE_URL).client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideGeocodeApi(retrofit: Retrofit): GeocodeApi = retrofit.create()

    @Provides
    @Singleton
    fun provideTransitStopApi(retrofit: Retrofit): TransitStopApi = retrofit.create()

    @Provides
    @Singleton
    fun provideTransitRouteApi(retrofit: Retrofit): TransitRouteApi = retrofit.create()

    @Provides
    @Singleton
    fun provideRoutePlannerApi(retrofit: Retrofit): RoutePlannerApi = retrofit.create()
}
