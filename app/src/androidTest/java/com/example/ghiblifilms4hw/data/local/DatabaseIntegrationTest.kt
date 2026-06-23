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
        database = Room.inMemoryDatabaseBuilder(context, FilmDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = database.filmDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadFilmsFromDatabase() = runBlocking {
        val films = listOf(
            FilmEntity("1", "Spirited Away", isFavorite = false),
            FilmEntity("2", "Totoro", isFavorite = true)
        )
        dao.insertFilms(films)
        val result = dao.getAllFilms().first()

        assertEquals(2, result.size)
        assertEquals("Spirited Away", result[0].title)
        assertTrue(result[1].isFavorite)
    }

    @Test
    fun updateFavoriteStatusCorrectlyPersists() = runBlocking {
        dao.insertFilm(FilmEntity("1", "Spirited Away", isFavorite = false))
        dao.updateFavoriteStatus("1", true)
        val updated = dao.getFilmById("1")

        assertNotNull(updated)
        assertTrue(updated?.isFavorite == true)
    }

    @Test
    fun getFavoriteFilmsReturnsOnlyFavoritedFilms() = runBlocking {
        dao.insertFilms(listOf(
            FilmEntity("1", "Film 1", isFavorite = true),
            FilmEntity("2", "Film 2", isFavorite = false),
            FilmEntity("3", "Film 3", isFavorite = true)
        ))
        val favorites = dao.getFavoriteFilms().first()

        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isFavorite })
    }
}