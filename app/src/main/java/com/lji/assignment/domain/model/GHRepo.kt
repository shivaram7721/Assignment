package com.lji.assignment.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A data class to represent a single GitHub repository.
 * It is marked with @Entity for Room and @JsonClass for Moshi.
 *
 * @param id The unique identifier of the repository.
 * @param name The name of the repository.
 * @param repoURL The URL to the repository on GitHub's website.
 * @param owner The owner (user) of the repository.
 */
@JsonClass(generateAdapter = true)
@Entity(tableName = "gh_repos")
data class GHRepo(
    @PrimaryKey
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "html_url") val repoURL: String,
    @param:Json(name = "owner") val owner: GitHubUser
) {
    // A secondary property to hold the owner's login name for the database and UI.
    val ownerLogin: String
        get() = owner.login
}
