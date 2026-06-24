package com.example.ghiblifilms4hw.di

import android.content.Context
import androidx.room.Room
import com.example.ghiblifilms4hw.data.local.FilmDao
import com.example.ghiblifilms4hw.data.local.FilmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFilmDatabase(@ApplicationContext context: Context): FilmDatabase {
        return Room.databaseBuilder(
            context,
            FilmDatabase::class.java,
            "film_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideFilmDao(database: FilmDatabase): FilmDao {
        return database.filmDao()
    }
}