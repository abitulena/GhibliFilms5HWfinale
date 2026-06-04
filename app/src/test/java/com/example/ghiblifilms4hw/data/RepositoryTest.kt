package com.example.ghiblifilms4hw.data

import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.local.FilmDao
import com.example.ghiblifilms4hw.data.remote.FilmDto
import com.example.ghiblifilms4hw.data.remote.GhibliApiService
import com.example.ghiblifilms4hw.model.FilmEntity
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

class RepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var api: GhibliApiService

    @MockK
    private lateinit var filmDao: FilmDao

    private lateinit var repository: Repository

    private val sampleFilmDto = FilmDto(
        id = "1",
        title = "Spirited Away",
        description = "Great film",
        director = "Hayao Miyazaki",
        producer = "Toshio Suzuki",
        releaseDate = "2001",
        rtScore = "97",
        image = "url"
    )

    private val sampleFilmEntity = FilmEntity(
        id = "1",
        title = "Spirited Away",
        description = "Great film",
        director = "Hayao Miyazaki",
        producer = "Toshio Suzuki",
        releaseDate = "2001",
        rtScore = "97",
        image = "url",
        isFavorite = false
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = Repository(api, filmDao)
    }

    @Test
    fun refreshFilmsShouldPreserveFavoriteStatusWhenUpdatingFromApi() = runTest {
        val existingFavorite = sampleFilmEntity.copy(isFavorite = true)
        coEvery { filmDao.getAllFilms() } returns flowOf(listOf(existingFavorite))
        coEvery { api.getFilms() } returns listOf(sampleFilmDto)
        coEvery { filmDao.insertFilms(any()) } returns Unit

        val result = repository.refreshFilms()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            filmDao.insertFilms(
                match { films ->
                    films.size == 1 && films[0].isFavorite == true
                }
            )
        }
    }

    @Test
    fun toggleFavoriteShouldUpdateFavoriteStatusCorrectly() = runTest {
        coEvery { filmDao.getFilmById("1") } returns sampleFilmEntity
        coEvery { filmDao.updateFavoriteStatus("1", true) } returns Unit

        val result = repository.toggleFavorite("1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { filmDao.updateFavoriteStatus("1", true) }
    }

    @Test
    fun toggleFavoriteOnNonExistentFilmShouldNotUpdate() = runTest {
        coEvery { filmDao.getFilmById("999") } returns null

        val result = repository.toggleFavorite("999")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { filmDao.updateFavoriteStatus(any(), any()) }
    }

    @Test
    fun saveFilmToCacheShouldPreserveExistingFavoriteStatus() = runTest {
        val existingFavorite = sampleFilmEntity.copy(isFavorite = true)
        coEvery { filmDao.getFilmById("1") } returns existingFavorite
        coEvery { filmDao.insertFilm(any()) } returns Unit

        val result = repository.saveFilmToCache(sampleFilmDto)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            filmDao.insertFilm(
                match { film ->
                    film.id == "1" && film.isFavorite == true
                }
            )
        }
    }

    @Test
    fun refreshFilmsShouldHandleApiErrorGracefully() = runTest {
        coEvery { filmDao.getAllFilms() } returns flowOf(emptyList())
        coEvery { api.getFilms() } throws IOException("Network error")

        val result = repository.refreshFilms()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}