package com.example.brainxp.di

import com.example.brainxp.core.time.AppClock
import com.example.brainxp.core.time.SystemAppClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface TimeModule {
    @Binds
    fun bindAppClock(impl: SystemAppClock): AppClock
}
