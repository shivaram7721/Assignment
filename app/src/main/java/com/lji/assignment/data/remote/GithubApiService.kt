package com.lji.assignment.data.remote

import com.lji.assignment.domain.model.GHRepo
import com.lji.assignment.domain.model.GitHubSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the GitHub API.
 * Defines the endpoint for fetching user repositories.
 */
interface GithubApiService {

    /**
     * Fetches repositories for a specific GitHub user.
     * @param username The username of the GitHub user.
     * @return A list of GHRepo entities.
     */
    @GET("users/{username}/repos")
    suspend fun getUserRepositories(@Path("username") username: String): List<GHRepo>

    // New endpoint for searching all repositories
    // The 'q' parameter is the search query (e.g., "language:swift", "android", "stars:>1000")
    // Additional parameters like sort, order, per_page, page can be added.
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30
    ): GitHubSearchResponse

}