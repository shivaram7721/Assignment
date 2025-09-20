package com.lji.assignment.data.remote

import com.lji.assignment.domain.model.GHRepo
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manages network requests using Retrofit.
 */
class NetworkService {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val gitHubApiService: GithubApiService = retrofit.create(GithubApiService::class.java)

    /**
     * Fetches repositories for a given GitHub username.
     */
    suspend fun fetchRepositoriesForUser(username: String): Result<List<GHRepo>> {
        return try {
            val repos = gitHubApiService.getUserRepositories(username)
            Result.success(repos)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Searches for repositories based on a query.
     * The query can be "language:swift", "android", "user:jake", "stars:>1000", etc.
     */
    suspend fun searchGitHubRepositories(query: String): Result<List<GHRepo>> {
        return try {
            // The search API returns GitHubSearchResponse which contains the list of items
            val response = gitHubApiService.searchRepositories(query = query)
            Result.success(response.items)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}