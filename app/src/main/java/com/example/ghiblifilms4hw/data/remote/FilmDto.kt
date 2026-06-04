package com.example.ghiblifilms4hw.data.remote

import com.google.gson.annotations.SerializedName
import com.example.ghiblifilms4hw.model.FilmEntity

data class FilmDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val director: String? = null,
    val producer: String? = null,
    @SerializedName("release_date")
    val releaseDate: String? = null,
    @SerializedName("rt_score")
    val rtScore: String? = null,
    val image: String? = null
) {
    fun toEntity(isFavorite: Boolean = false) = FilmEntity(
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
}