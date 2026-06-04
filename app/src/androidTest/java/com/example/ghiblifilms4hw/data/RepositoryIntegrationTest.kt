package com.example.ghiblifilms4hw.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ghiblifilms4hw.data.local.FilmDatabase
import com.example.ghiblifilms4hw.data.remote.FilmDto
import com.example.ghiblifilms4hw.data.remote.GhibliApiService
import com.example.ghiblifilms4hw.model.FilmEntity
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    @MockK
    private lateinit var api: GhibliApiService

    private lateinit var database: FilmDatabase
    private lateinit var repository: Repository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FilmDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = Repository(api, database.filmDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshFilmsSyncsApiDataToDatabaseCorrectly() = runTest {
        val apiFilms = listOf(
            FilmDto("1", "Spirited Away", description = "A wonderful film", director = "Miyazaki", producer = "Suzuki", releaseDate = "2001", rtScore = "97", image = "url1"),
            FilmDto("2", "Totoro", description = "Heartwarming", director = "Miyazaki", producer = "Hara", releaseDate = "1988", rtScore = "93", image = "url2")
        )
        coEvery { api.getFilms() } returns apiFilms

        val result = repository.refreshFilms()
        val filmsInDb = repository.getAllFilms().first()

        assertTrue(result.isSuccess)
        assertEquals(2, filmsInDb.size)
        assertEquals("1", filmsInDb[0].id)
        assertEquals("Spirited Away", filmsInDb[0].title)
        assertEquals("A wonderful film", filmsInDb[0].description)
        assertEquals("Miyazaki", filmsInDb[0].director)
        assertEquals("2", filmsInDb[1].id)
        assertEquals("Totoro", filmsInDb[1].title)
    }

    @Test
    fun favoritesPersistAfterRefresh() = runTest {
        val favoriteFilm = FilmEntity("1", "Spirited Away", isFavorite = true)
        database.filmDao().insertFilm(favoriteFilm)

        val apiFilms = listOf(FilmDto("1", "Spirited Away"))
        coEvery { api.getFilms() } returns apiFilms

        repository.refreshFilms()
        val films = repository.getAllFilms().first()

        assertEquals(1, films.size)
        assertTrue(films[0].isFavorite)
        assertEquals("Spirited Away", films[0].title)
    }

    @Test
    fun emptyApiResponseDoesNotDeleteExistingFilms() = runTest {
        database.filmDao().insertFilm(FilmEntity("1", "Old Film", isFavorite = false))
        database.filmDao().insertFilm(FilmEntity("2", "Favorite Film", isFavorite = true))

        coEvery { api.getFilms() } returns emptyList()

        val result = repository.refreshFilms()
        val films = repository.getAllFilms().first()

        assertTrue(result.isSuccess)
        assertEquals(2, films.size)
        assertTrue(films.any { it.title == "Favorite Film" })
        assertTrue(films.any { it.title == "Old Film" })
    }

    @Test
    fun refreshFilmsPreservesFavoriteStatusFromDb() = runTest {
        database.filmDao().insertFilm(FilmEntity("1", "Spirited Away", isFavorite = true))

        val apiFilms = listOf(FilmDto("1", "Spirited Away Updated"))
        coEvery { api.getFilms() } returns apiFilms

        repository.refreshFilms()
        val films = repository.getAllFilms().first()

        assertEquals(1, films.size)
        assertTrue(films[0].isFavorite)
        assertEquals("Spirited Away Updated", films[0].title)
    }
}