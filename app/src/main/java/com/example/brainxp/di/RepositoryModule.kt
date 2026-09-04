package com.example.brainxp.di

import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.data.repo.SessionRepository
import com.example.brainxp.data.repo.fake.FakeActivityLogRepository
import com.example.brainxp.data.repo.fake.FakeFamilyRepository
import com.example.brainxp.data.repo.fake.FakeMaterialRepository
import com.example.brainxp.data.repo.fake.FakeRestrictionRepository
import com.example.brainxp.data.repo.fake.FakeRewardRepository
import com.example.brainxp.data.repo.fake.FakeSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindMaterialRepository(impl: FakeMaterialRepository): MaterialRepository

    @Binds
    fun bindSessionRepository(impl: FakeSessionRepository): SessionRepository

    @Binds
    fun bindRewardRepository(impl: FakeRewardRepository): RewardRepository

    @Binds
    fun bindRestrictionRepository(impl: FakeRestrictionRepository): RestrictionRepository

    @Binds
    fun bindActivityLogRepository(impl: FakeActivityLogRepository): ActivityLogRepository

    @Binds
    fun bindFamilyRepository(impl: FakeFamilyRepository): FamilyRepository
}
