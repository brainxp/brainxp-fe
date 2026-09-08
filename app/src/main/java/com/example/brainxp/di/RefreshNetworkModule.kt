package com.example.brainxp.di

import com.example.brainxp.core.network.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RefreshNetworkModule {
    const val REFRESH = "refresh"

    private const val TIMEOUT_SECONDS = 30L
    private const val CONTENT_TYPE = "application/json"

    @Provides
    @Singleton
    @Named(REFRESH)
    fun provideRefreshClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named(REFRESH)
    fun provideRefreshRetrofit(
        @Named(REFRESH) client: OkHttpClient,
        json: Json,
        @Named("baseUrl") baseUrl: String,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(CONTENT_TYPE.toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named(REFRESH)
    fun provideRefreshAuthApi(
        @Named(REFRESH) retrofit: Retrofit,
    ): AuthApi = retrofit.create(AuthApi::class.java)
}
