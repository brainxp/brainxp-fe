package com.example.brainxp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.brainxp.core.network.AlertApi
import com.example.brainxp.data.prefs.AlertSeenStore
import com.example.brainxp.data.repo.AlertRepository
import com.example.brainxp.data.repo.NetworkAlertRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlertModule {
    @Provides
    @Singleton
    @Named("alertStore")
    fun provideAlertStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = AlertSeenStore.from(context)

    @Provides
    @Singleton
    fun provideAlertSeenStore(
        @Named("alertStore") store: DataStore<Preferences>,
    ): AlertSeenStore = AlertSeenStore(store)

    @Provides
    @Singleton
    fun provideAlertApi(retrofit: Retrofit): AlertApi = retrofit.create(AlertApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface AlertBindings {
    @Binds
    fun bindAlertRepository(impl: NetworkAlertRepository): AlertRepository
}
