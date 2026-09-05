package com.example.brainxp.di

import com.example.brainxp.core.network.AuthApi
import com.example.brainxp.core.network.AuthInterceptor
import com.example.brainxp.core.network.AuthTokenStore
import com.example.brainxp.core.network.FamilyApi
import com.example.brainxp.core.network.MaterialApi
import com.example.brainxp.core.network.NetworkTokenRefresher
import com.example.brainxp.core.network.PersistentAuthTokenStore
import com.example.brainxp.core.network.PolicyApi
import com.example.brainxp.core.network.QuizApi
import com.example.brainxp.core.network.RewardApi
import com.example.brainxp.core.network.TokenAuthenticator
import com.example.brainxp.core.network.TokenRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://brainxp-api.satu-miliar-pertama-di-2027.biz.id/"
    private const val TIMEOUT_SECONDS = 30L
    private const val CONTENT_TYPE = "application/json"

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            isLenient = true
        }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providePolicyApi(retrofit: Retrofit): PolicyApi = retrofit.create(PolicyApi::class.java)

    @Provides
    @Singleton
    fun provideFamilyApi(retrofit: Retrofit): FamilyApi = retrofit.create(FamilyApi::class.java)

    @Provides
    @Singleton
    fun provideRewardApi(retrofit: Retrofit): RewardApi = retrofit.create(RewardApi::class.java)

    @Provides
    @Singleton
    fun provideQuizApi(retrofit: Retrofit): QuizApi = retrofit.create(QuizApi::class.java)

    @Provides
    @Singleton
    fun provideMaterialApi(retrofit: Retrofit): MaterialApi = retrofit.create(MaterialApi::class.java)

    @Provides
    @Named("baseUrl")
    fun provideBaseUrl(): String = BASE_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
        @Named("baseUrl") baseUrl: String,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(CONTENT_TYPE.toMediaType()))
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
interface NetworkBindings {
    @Binds
    fun bindAuthTokenStore(impl: PersistentAuthTokenStore): AuthTokenStore

    @Binds
    fun bindTokenRefresher(impl: NetworkTokenRefresher): TokenRefresher
}
