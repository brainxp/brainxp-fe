package com.example.brainxp.di

import com.example.brainxp.data.repo.QuestionReporter
import com.example.brainxp.data.repo.QuizRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface QuestionReportModule {
    @Binds
    fun bindQuestionReporter(impl: QuizRepository): QuestionReporter
}
