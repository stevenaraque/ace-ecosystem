package com.ace.mobile.di

import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.remote.api.*
import com.ace.mobile.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBaseUrl(): String = "https://ace-ecosystem.onrender.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideNoAuthOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("noAuth")
    fun provideNoAuthRetrofit(
        baseUrl: String,
        @Named("noAuth") client: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@Named("noAuth") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userDao: UserDao,
        authApiProvider: Provider<AuthApi> // ◄──          Cambiado de AuthApi a Provider<AuthApi>
    ): AuthInterceptor {
        return AuthInterceptor(userDao, authApiProvider) // ◄── Le pasamos el proveedor
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        baseUrl: String,
        @Named("auth") client: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    // Dentro de tu @Module @InstallIn(SingletonComponent::class)
// Agrega este provider:

    @Provides
    @Singleton
    fun provideExerciseApi(retrofit: Retrofit): ExerciseApi {
        return retrofit.create(ExerciseApi::class.java)
    }
    // Dentro del object NetworkModule, agregar:

    @Provides
    @Singleton
    fun provideXpFormulaApi(retrofit: Retrofit): XpFormulaApi {
        return retrofit.create(XpFormulaApi::class.java)
    }
    // REEMPLAZAR — agregar al final de NetworkModule.kt
    @Provides
    @Singleton
    fun provideRankingApi(retrofit: Retrofit): com.ace.mobile.data.remote.api.RankingApi {
        return retrofit.create(com.ace.mobile.data.remote.api.RankingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStatsApi(retrofit: Retrofit): StatsApi {
        return retrofit.create(StatsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideHistoryApi(retrofit: Retrofit): HistoryApi {
        return retrofit.create(HistoryApi::class.java)
    }

}