package com.example.brainxp.di

import android.content.Context
import androidx.room.Room
import com.example.brainxp.data.db.ActivityEventDao
import com.example.brainxp.data.db.AnswerDao
import com.example.brainxp.data.db.BrainXPDatabase
import com.example.brainxp.data.db.MIGRATION_1_2
import com.example.brainxp.data.db.MIGRATION_2_3
import com.example.brainxp.data.db.MIGRATION_3_4
import com.example.brainxp.data.db.MIGRATION_4_5
import com.example.brainxp.data.db.MaterialDao
import com.example.brainxp.data.db.PendingOperationDao
import com.example.brainxp.data.db.QuestionDao
import com.example.brainxp.data.db.QuestionSessionDao
import com.example.brainxp.data.db.RestrictedAppDao
import com.example.brainxp.data.db.UnlockSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): BrainXPDatabase =
        Room
            .databaseBuilder(context, BrainXPDatabase::class.java, BrainXPDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideMaterialDao(db: BrainXPDatabase): MaterialDao = db.materialDao()

    @Provides
    fun provideQuestionSessionDao(db: BrainXPDatabase): QuestionSessionDao = db.questionSessionDao()

    @Provides
    fun provideQuestionDao(db: BrainXPDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideAnswerDao(db: BrainXPDatabase): AnswerDao = db.answerDao()

    @Provides
    fun provideUnlockSessionDao(db: BrainXPDatabase): UnlockSessionDao = db.unlockSessionDao()

    @Provides
    fun provideRestrictedAppDao(db: BrainXPDatabase): RestrictedAppDao = db.restrictedAppDao()

    @Provides
    fun provideActivityEventDao(db: BrainXPDatabase): ActivityEventDao = db.activityEventDao()

    @Provides
    fun providePendingOperationDao(db: BrainXPDatabase): PendingOperationDao = db.pendingOperationDao()
}
