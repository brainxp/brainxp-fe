package com.example.brainxp.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface QuizApi {
    @POST("subjects/{subjectId}/quizzes")
    suspend fun start(
        @Path("subjectId") subjectId: String,
        @Body body: QuizStartDto,
    ): QuizDto

    @GET("quizzes/{sessionId}")
    suspend fun quiz(
        @Path("sessionId") sessionId: String,
    ): QuizDto

    @POST("quizzes/{sessionId}/submit")
    suspend fun submit(
        @Path("sessionId") sessionId: String,
    ): ReceiptDto

    @POST("quizzes/{sessionId}/answers")
    suspend fun answer(
        @Path("sessionId") sessionId: String,
        @Body body: AnswerRequestDto,
    ): AnswerSavedDto
}
