package com.example.ghiblifilms4hw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ghiblifilms4hw.model.FilmEntity

@Database(
    entities = [FilmEntity::class],
    version = 6,
    exportSchema = false
)
abstract class FilmDatabase : RoomDatabase() {
    abstract fun filmDao(): FilmDao
}