package com.example.ghiblifilms4hw.ui.viewmodel

import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FavouritesUiState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class FavouritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var repository: Repository

    private lateinit var viewModel: FavouritesViewModel

    private val favouriteFilms = listOf(
        Film(id = "1", title = "Spirited Away", isFavorite = true),
        Film(id = "2", title = "Totoro", isFavorite = true)
    )

    private val emptyList = emptyList<Film>()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun loadFavouritesWhenNotEmptyReturnsSuccess() = runTest {
        coEvery { repository.getFavoriteFilms() } returns flowOf(favouriteFilms)

        viewModel = FavouritesViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FavouritesUiState.Success)
        if (state is FavouritesUiState.Success) {
            assertEquals(2, state.films.size)
            assertEquals("Spirited Away", state.films[0].title)
        }
    }

    @Test
    fun loadFavouritesWhenEmptyReturnsEmpty() = runTest {
        coEvery { repository.getFavoriteFilms() } returns flowOf(emptyList)

        viewModel = FavouritesViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FavouritesUiState.Empty)
    }

    @Test
    fun removeFromFavouritesUpdatesUiState() = runTest {
        coEvery { repository.getFavoriteFilms() } returns flowOf(favouriteFilms)
        coEvery { repository.toggleFavorite("1") } returns Result.success(Unit)

        viewModel = FavouritesViewModel(repository)
        advanceUntilIdle()

        val beforeState = viewModel.uiState.value as FavouritesUiState.Success
        assertEquals(2, beforeState.films.size)

        val updatedList = listOf(favouriteFilms[1])
        coEvery { repository.getFavoriteFilms() } returns flowOf(updatedList)

        viewModel.removeFromFavourites("1")
        advanceUntilIdle()

        coVerify(atLeast = 1) { repository.toggleFavorite("1") }

        val afterState = viewModel.uiState.value as FavouritesUiState.Success
        assertEquals(1, afterState.films.size)
        assertEquals("Totoro", afterState.films[0].title)
    }
}