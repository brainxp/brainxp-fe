package com.example.brainxp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.brainxp.core.device.InstallBinding
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.AuthGuideAccount
import com.example.brainxp.data.prefs.DataStoreRewardCache
import com.example.brainxp.data.prefs.GuideAccount
import com.example.brainxp.data.prefs.RewardCache
import com.example.brainxp.data.prefs.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    @Named("settingsStore")
    fun provideSettingsStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = SettingsDataStore.from(context)

    @Provides
    @Singleton
    @Named("deviceStore")
    fun provideDeviceStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = InstallBinding.from(context)

    @Provides
    @Singleton
    fun provideInstallBinding(
        @Named("deviceStore") store: DataStore<Preferences>,
    ): InstallBinding = InstallBinding(store)

    @Provides
    @Singleton
    @Named("authStore")
    fun provideAuthStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = AuthDataStore.from(context)

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @Named("settingsStore") store: DataStore<Preferences>,
    ): SettingsDataStore = SettingsDataStore(store)

    @Provides
    @Singleton
    fun provideAuthDataStore(
        @Named("authStore") store: DataStore<Preferences>,
    ): AuthDataStore = AuthDataStore(store)

    @Provides
    @Singleton
    fun provideGuideAccount(binding: AuthGuideAccount): GuideAccount = binding

    @Provides
    @Singleton
    fun provideRewardCache(
        @Named("settingsStore") store: DataStore<Preferences>,
    ): RewardCache = DataStoreRewardCache(store)
}
