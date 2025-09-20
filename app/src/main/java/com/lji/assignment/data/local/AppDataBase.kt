package com.lji.assignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lji.assignment.convertors.Converters
import com.lji.assignment.domain.model.GHRepo

/**
 * The main database class for the application.
 * This class serves as the main access point to the underlying SQLite database.
 * It is an abstract class that extends RoomDatabase.
 */
@Database(entities = [GHRepo::class] ,version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDataBase: RoomDatabase() {
    abstract fun githubDao(): GithubDao
}