package com.lji.assignment.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubSearchResponse(
    @param:Json(name = "total_count") val totalCount: Int,
    @param:Json(name = "incomplete_results") val incompleteResults: Boolean,
    @param:Json(name = "items") val items: List<GHRepo>
)