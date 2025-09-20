package com.lji.assignment.convertors

import androidx.room.TypeConverter
import com.lji.assignment.domain.model.GitHubUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * A Room TypeConverter for the GitHubUser data class.
 * This allows Room to store and retrieve the complex GitHubUser object
 * by converting it to and from a JSON string.
 */
class Converters {
    // The Moshi instance is needed to perform the JSON serialization and deserialization
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(GitHubUser::class.java)

    /**
     * Converts a GitHubUser object to a JSON string for database storage.
     */
    @TypeConverter
    fun fromUser(user: GitHubUser): String {
        return adapter.toJson(user)
    }

    /**
     * Converts a JSON string from the database back to a GitHubUser object.
     */
    @TypeConverter
    fun toUser(json: String): GitHubUser? {
        return adapter.fromJson(json)
    }
}