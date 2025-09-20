package com.lji.assignment.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.lji.assignment.BuildConfig
import com.lji.assignment.data.remote.GithubApiService
import com.lji.assignment.utils.NetworkObserver
import com.lji.assignment.utils.NetworkObserverImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies.
 *
 * This module is responsible for setting up and providing networking components
 * like OkHttpClient, Moshi, Retrofit, and the GitHub API service. By installing
 * this module in the [SingletonComponent], these dependencies are available
 * throughout the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    const val BASE_URL = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun providesMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                if(BuildConfig.DEBUG) {
                    val logging = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(logging)
                    addInterceptor(ChuckerInterceptor.Builder(context).build())
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(moshi: Moshi, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun providesGithubService(retrofit: Retrofit): GithubApiService {
        return retrofit.create(GithubApiService::class.java)
    }

    /**
     * Provides a singleton instance of NetworkObserver.
     * @param context The application context provided by Hilt.
     */
    @Singleton
    @Provides
    fun provideNetworkObserver(@ApplicationContext context: Context): NetworkObserver {
        return NetworkObserverImpl(context)
    }
}