package com.example.ghiblifilms4hw.data

import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.local.FilmDao
import com.example.ghiblifilms4hw.data.remote.FilmDto
import com.example.ghiblifilms4hw.data.remote.GhibliApiService
import com.example.ghiblifilms4hw.model.FilmEntity
import com.example.ghiblifilms4hw.model.Film
import kotlinx.coroutines.test.advanceUntilIdle
import io.mockk.MockKAnnotations
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        id = "1", title = "Spirited Away", description = "Great film",
        director = "Hayao Miyazaki", producer = "Toshio Suzuki",
        releaseDate = "2001", rtScore = "97", image = "url"
    )

    private val sampleFilmEntity = FilmEntity(
        id = "1", title = "Spirited Away", description = "Great film",
        director = "Hayao Miyazaki", producer = "Toshio Suzuki",
        releaseDate = "2001", rtScore = "97", image = "url", isFavorite = false
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
            filmDao.insertFilms(match { films ->
                films.size == 1 && films[0].isFavorite == true
            })
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

    @Test
    fun getAllFilmsEmitsUpdatesInOrder() = runTest {
        val channel = Channel<List<FilmEntity>>(Channel.UNLIMITED)
        coEvery { filmDao.getAllFilms() } returns channel.consumeAsFlow()

        val emissions = mutableListOf<List<Film>>()
        val job = launch {
            repository.getAllFilms().collect { emissions.add(it) }
        }

        channel.send(emptyList())
        channel.send(listOf(sampleFilmEntity))
        advanceUntilIdle()
        job.cancel()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0].isEmpty())
        assertEquals("Spirited Away", emissions[1][0].title)
        assertEquals("Hayao Miyazaki", emissions[1][0].director)
    }

    @Test
    fun getAllFilmsMappsEntityToFilmCorrectly() = runTest {
        val entityWithFavorite = sampleFilmEntity.copy(isFavorite = true)
        coEvery { filmDao.getAllFilms() } returns flowOf(listOf(entityWithFavorite))

        val result = repository.getAllFilms().first()

        assertEquals(1, result.size)
        val film = result[0]
        assertEquals("1", film.id)
        assertEquals("Spirited Away", film.title)
        assertEquals("Hayao Miyazaki", film.director)
        assertTrue(film.isFavorite)
    }
}
