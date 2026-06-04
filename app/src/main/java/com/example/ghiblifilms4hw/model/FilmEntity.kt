package com.example.ghiblifilms4hw.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "films")
data class FilmEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val director: String? = null,
    val producer: String? = null,
    val releaseDate: String? = null,
    val rtScore: String? = null,
    val image: String? = null,
    val isFavorite: Boolean = false
)