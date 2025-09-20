package com.lji.assignment.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing the owner (user) of a GitHub repository.
 * This is nested within the GHRepo data, primarily to extract the 'login' name.
 */
@JsonClass(generateAdapter = true)
data class GitHubUser(
    @param:Json(name = "login") val login: String,
)