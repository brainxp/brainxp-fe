package com.example.brainxp.di

import com.example.brainxp.blocking.InstalledAppsSource
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.AppLabels
import com.example.brainxp.data.repo.ConsumptionReporter
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.data.repo.NetworkMaterialRepository
import com.example.brainxp.data.repo.NetworkRewardRepository
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.data.repo.RewardReconciler
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.data.repo.RoomActivityLogRepository
import com.example.brainxp.data.repo.RoomRestrictionRepository
import com.example.brainxp.data.repo.RoomUnlockRepository
import com.example.brainxp.data.repo.UnlockRepository
import com.example.brainxp.data.repo.fake.FakeFamilyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindMaterialRepository(impl: NetworkMaterialRepository): MaterialRepository

    @Binds
    fun bindRewardRepository(impl: NetworkRewardRepository): RewardRepository

    @Binds
    fun bindRestrictionRepository(impl: RoomRestrictionRepository): RestrictionRepository

    @Binds
    fun bindActivityLogRepository(impl: RoomActivityLogRepository): ActivityLogRepository

    @Binds
    fun bindAppLabels(impl: InstalledAppsSource): AppLabels

    @Binds
    fun bindFamilyRepository(impl: FakeFamilyRepository): FamilyRepository

    @Binds
    fun bindUnlockRepository(impl: RoomUnlockRepository): UnlockRepository

    @Binds
    fun bindConsumptionReporter(impl: RewardReconciler): ConsumptionReporter
}
