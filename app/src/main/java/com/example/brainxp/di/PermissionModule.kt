package com.example.brainxp.di

import com.example.brainxp.core.permission.AndroidPermissionIntents
import com.example.brainxp.core.permission.AndroidPermissionReader
import com.example.brainxp.core.permission.DefaultPermissionStateProvider
import com.example.brainxp.core.permission.PermissionIntents
import com.example.brainxp.core.permission.PermissionReader
import com.example.brainxp.core.permission.PermissionStateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface PermissionModule {
    @Binds
    fun bindPermissionReader(impl: AndroidPermissionReader): PermissionReader

    @Binds
    fun bindPermissionIntents(impl: AndroidPermissionIntents): PermissionIntents

    @Binds
    fun bindPermissionStateProvider(impl: DefaultPermissionStateProvider): PermissionStateProvider
}
