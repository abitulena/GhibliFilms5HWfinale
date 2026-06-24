package com.example.ghiblifilms4hw.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface GhibliApiService {
    @GET("films")
    suspend fun getFilms(): List<FilmDto>

    @GET("films/{id}")
    suspend fun getFilmById(@Path("id") id: String): FilmDto
}