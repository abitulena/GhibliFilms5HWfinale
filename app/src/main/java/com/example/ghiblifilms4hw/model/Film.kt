package com.example.ghiblifilms4hw.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Film(
    val id: String,
    val title: String,
    val description: String? = null,
    val director: String? = null,
    val producer: String? = null,
    val releaseDate: String? = null,
    val rtScore: String? = null,
    val image: String? = null,
    val isFavorite: Boolean = false
) : Parcelable {
    fun toEntity() = FilmEntity(
        id = id,
        title = title,
        description = description,
        director = director,
        producer = producer,
        releaseDate = releaseDate,
        rtScore = rtScore,
        image = image,
        isFavorite = isFavorite
    )

    companion object {
        fun fromEntity(entity: FilmEntity) = Film(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            director = entity.director,
            producer = entity.producer,
            releaseDate = entity.releaseDate,
            rtScore = entity.rtScore,
            image = entity.image,
            isFavorite = entity.isFavorite
        )
    }
}