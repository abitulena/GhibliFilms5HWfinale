package com.example.ghiblifilms4hw.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ghiblifilms4hw.model.FilmEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var database: FilmDatabase
    private lateinit var dao: FilmDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FilmDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.filmDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadFilmsFromDatabase() = runBlocking {
        val films = listOf(
            FilmEntity("1", "Spirited Away", description = "A wonderful film", director = "Miyazaki", isFavorite = false),
            FilmEntity("2", "Totoro", description = "Heartwarming", director = "Miyazaki", isFavorite = true)
        )
        dao.insertFilms(films)
        val result = dao.getAllFilms().first()

        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("Spirited Away", result[0].title)
        assertEquals("A wonderful film", result[0].description)
        assertEquals("Miyazaki", result[0].director)
        assertFalse(result[0].isFavorite)

        assertEquals("2", result[1].id)
        assertEquals("Totoro", result[1].title)
        assertTrue(result[1].isFavorite)
    }

    @Test
    fun updateFavoriteStatusCorrectlyPersists() = runBlocking {
        val film = FilmEntity("1", "Spirited Away", isFavorite = false)
        dao.insertFilm(film)

        dao.updateFavoriteStatus("1", true)
        val updated = dao.getFilmById("1")

        assertNotNull(updated)
        assertTrue(updated?.isFavorite == true)
        assertEquals("Spirited Away", updated?.title)
    }

    @Test
    fun getFavoriteFilmsReturnsOnlyFavoritedFilms() = runBlocking {
        val films = listOf(
            FilmEntity("1", "Film 1", isFavorite = true),
            FilmEntity("2", "Film 2", isFavorite = false),
            FilmEntity("3", "Film 3", isFavorite = true)
        )
        dao.insertFilms(films)
        val favorites = dao.getFavoriteFilms().first()

        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isFavorite })
        assertEquals("Film 1", favorites[0].title)
        assertEquals("Film 3", favorites[1].title)
    }

    @Test
    fun clearAllRemovesAllFilms() = runBlocking {
        val films = listOf(
            FilmEntity("1", "Film 1"),
            FilmEntity("2", "Film 2")
        )
        dao.insertFilms(films)

        dao.clearAll()
        val result = dao.getAllFilms().first()

        assertEquals(0, result.size)
    }
}