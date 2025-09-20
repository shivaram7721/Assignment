package com.lji.assignment.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lji.assignment.domain.model.GHRepo
import kotlinx.coroutines.flow.Flow

/**
 * The Data Access Object (DAO) for the GHRepo entity.
 * This interface defines the methods that Room uses to interact with the
 * `repositories` table in the database.
 */
@Dao
interface GithubDao {
    @Query("SELECT * FROM gh_repos")
    fun getGithubRepos(): Flow<List<GHRepo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<GHRepo>)

    @Query("DELETE FROM gh_repos")
    suspend fun clearAllRepos()
}
