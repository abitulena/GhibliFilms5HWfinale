package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.data.remote.FilmDto
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmDetailUiState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class FilmDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var repository: Repository

    private lateinit var viewModel: FilmDetailViewModel

    private val testFilmId = "123"
    private val testFilm = Film(
        id = testFilmId,
        title = "Spirited Away",
        director = "Hayao Miyazaki",
        releaseDate = "2001",
        rtScore = "97",
        image = "url"
    )

    private val testFilmDto = FilmDto(
        id = testFilmId,
        title = "Spirited Away",
        director = "Hayao Miyazaki",
        releaseDate = "2001",
        rtScore = "97",
        image = "url"
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun loadFilmDetailWhenFilmInDbReturnsSuccess() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("filmId" to testFilmId))
        coEvery { repository.getFilmById(testFilmId) } returns testFilm

        viewModel = FilmDetailViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FilmDetailUiState.Success)
        if (state is FilmDetailUiState.Success) {
            assertEquals(testFilmId, state.film.id)
            assertEquals("Spirited Away", state.film.title)
        }
    }

    @Test
    fun loadFilmDetailWhenFilmNotInDbFetchesFromApiAndSavesToCache() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("filmId" to testFilmId))
        coEvery { repository.getFilmById(testFilmId) } returns null
        coEvery { repository.getFilmFromApiById(testFilmId) } returns testFilmDto
        coEvery { repository.saveFilmToCache(testFilmDto) } returns Result.success(Unit)
        coEvery { repository.getFilmById(testFilmId) } returnsMany listOf(null, testFilm)

        viewModel = FilmDetailViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getFilmFromApiById(testFilmId) }
        coVerify(exactly = 1) { repository.saveFilmToCache(testFilmDto) }

        val state = viewModel.uiState.value
        assertTrue(state is FilmDetailUiState.Success)
        if (state is FilmDetailUiState.Success) {
            assertEquals(testFilmId, state.film.id)
        }
    }

    @Test
    fun loadFilmDetailWhenApiFailsReturnsError() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("filmId" to testFilmId))
        coEvery { repository.getFilmById(testFilmId) } returns null
        coEvery { repository.getFilmFromApiById(testFilmId) } returns null

        viewModel = FilmDetailViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FilmDetailUiState.Error)
        if (state is FilmDetailUiState.Error) {
            assertEquals("Film not found", state.message)
        }
    }

    @Test
    fun toggleFavoriteUpdatesUiState() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("filmId" to testFilmId))
        coEvery { repository.getFilmById(testFilmId) } returns testFilm
        coEvery { repository.toggleFavorite(testFilmId) } returns Result.success(Unit)

        viewModel = FilmDetailViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        val updatedFilm = testFilm.copy(isFavorite = true)
        coEvery { repository.getFilmById(testFilmId) } returns updatedFilm

        viewModel.toggleFavorite()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FilmDetailUiState.Success
        assertTrue(state.film.isFavorite)
    }
}