package com.lji.assignment.di

import android.content.Context
import androidx.room.Room
import com.lji.assignment.data.local.AppDataBase
import com.lji.assignment.data.local.GithubDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 *
 * This module is responsible for setting up and providing the Room database
 * and its associated DAO (Data Access Object) as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule {

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): AppDataBase {
        return Room.databaseBuilder(
            context,
            AppDataBase::class.java,
            "github_database"
        ).fallbackToDestructiveMigration(false).build()
    }

    @Provides
    @Singleton
    fun provideGithubDao(githubDatabase: AppDataBase): GithubDao {
        return githubDatabase.githubDao()
    }
}