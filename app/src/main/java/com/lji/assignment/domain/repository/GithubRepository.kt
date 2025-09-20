package com.lji.assignment.domain.repository

import com.lji.assignment.data.local.GithubDao
import com.lji.assignment.data.remote.GithubApiService
import com.lji.assignment.domain.model.GHRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The central repository for fetching and managing repository data.
 * It decides whether to fetch from the network or use the cached data.
 * It uses Room for caching and Retrofit for network calls.
 */
@Singleton
class GitHubRepository @Inject constructor(
    private val githubApiService: GithubApiService,
    private val githubDao: GithubDao
) {
    /**
     * Searches for repositories and manages caching.
     * It first tries to fetch from the network, caches the result,
     * and then returns the cached data as a Flow.
     */
    fun getRepositoriesBySearch(query: String): Flow<List<GHRepo>> = flow {

        emit(githubDao.getGithubRepos().firstOrNull() ?: emptyList())

        val networkResponse = githubApiService.searchRepositories(query = query)
        val repos = networkResponse.items

        githubDao.clearAllRepos()
        githubDao.insertAll(repos)

        githubDao.getGithubRepos().collect { cachedRepos ->
            emit(cachedRepos)
        }
    }.catch { e ->
        throw e
    }
}