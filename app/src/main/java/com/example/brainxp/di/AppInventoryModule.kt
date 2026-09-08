package com.example.brainxp.di

import com.example.brainxp.core.network.AppInventoryApi
import com.example.brainxp.data.repo.AppInventoryRepository
import com.example.brainxp.data.repo.NetworkAppInventoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInventoryModule {
    @Provides
    @Singleton
    fun provideAppInventoryApi(retrofit: Retrofit): AppInventoryApi = retrofit.create(AppInventoryApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface AppInventoryBindings {
    @Binds
    fun bindAppInventoryRepository(impl: NetworkAppInventoryRepository): AppInventoryRepository
}
