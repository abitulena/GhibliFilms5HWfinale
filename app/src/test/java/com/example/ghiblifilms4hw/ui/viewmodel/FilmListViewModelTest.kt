package com.example.ghiblifilms4hw.ui.viewmodel

import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
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
import java.io.IOException

class FilmListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var repository: Repository

    private lateinit var viewModel: FilmListViewModel

    private val sampleFilms = listOf(
        Film(id = "1", title = "Spirited Away", director = "Hayao Miyazaki", isFavorite = false),
        Film(id = "2", title = "My Neighbor Totoro", director = "Hayao Miyazaki", isFavorite = true)
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun initialStateShouldBeLoading() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)

        assertTrue(viewModel.uiState.value is FilmListUiState.Loading)
    }

    @Test
    fun successfulDataLoadingShouldResultInSuccessState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FilmListUiState.Success)
        if (state is FilmListUiState.Success) {
            assertEquals(2, state.films.size)
            assertEquals("Spirited Away", state.films[0].title)
        }
    }

    @Test
    fun errorLoadingDataShouldResultInErrorState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.failure(IOException("Network error"))

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FilmListUiState.Error)
    }

    @Test
    fun searchQueryShouldFilterFilmsCorrectly() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("Spirited")

        val state = viewModel.uiState.value as FilmListUiState.Success
        assertEquals("Spirited", state.searchQuery)
        assertEquals(1, state.filteredFilms.size)
        assertEquals("Spirited Away", state.filteredFilms[0].title)
    }

    @Test
    fun emptySearchResultsShouldGiveEmptyNotSuccessWithEmptyList() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("NonExistentFilm")

        val state = viewModel.uiState.value as FilmListUiState.Success
        assertFalse(state.hasFilteredResults)
        assertTrue(state.hasActiveFilters)
        assertEquals(0, state.filteredFilms.size)
    }

    @Test
    fun directorFilterShouldShowOnlyFilmsBySelectedDirector() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateDirectorFilter("Hayao Miyazaki")

        val state = viewModel.uiState.value as FilmListUiState.Success
        assertEquals("Hayao Miyazaki", state.selectedDirector)
        assertEquals(2, state.filteredFilms.size)
    }

    @Test
    fun resetFiltersShouldClearSearchAndDirectorFilters() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("Spirited")
        viewModel.updateDirectorFilter("Hayao Miyazaki")
        viewModel.resetFilters()

        val state = viewModel.uiState.value as FilmListUiState.Success
        assertEquals("", state.searchQuery)
        assertNull(state.selectedDirector)
        assertEquals(2, state.filteredFilms.size)
    }

    @Test
    fun showFiltersShouldToggleFilterVisibility() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        assertFalse((viewModel.uiState.value as FilmListUiState.Success).showFilters)

        viewModel.toggleFilters()
        assertTrue((viewModel.uiState.value as FilmListUiState.Success).showFilters)

        viewModel.toggleFilters()
        assertFalse((viewModel.uiState.value as FilmListUiState.Success).showFilters)
    }
}