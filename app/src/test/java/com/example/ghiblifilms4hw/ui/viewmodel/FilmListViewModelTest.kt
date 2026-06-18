package com.example.ghiblifilms4hw.ui.viewmodel

import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun initialStateShouldBeLoadingBeforeCoroutinesRun() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.success(Unit)
        viewModel = FilmListViewModel(repository)
        assertTrue(
            "State must be Loading immediately after construction, before coroutines run",
            viewModel.uiState.value is FilmListUiState.Loading
        )

        advanceUntilIdle()
        assertFalse(
            "State must change from Loading after coroutines complete",
            viewModel.uiState.value is FilmListUiState.Loading
        )
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
    fun networkErrorWithEmptyDatabaseShouldResultInErrorState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.failure(IOException("Network error"))

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Expected Error state but was ${state::class.simpleName}",
            state is FilmListUiState.Error
        )
        assertEquals("Network error", (state as FilmListUiState.Error).message)
    }

    @Test
    fun emptyDatabaseWithSuccessfulRefreshShouldResultInEmptyState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Expected Empty state but was ${state::class.simpleName}",
            state is FilmListUiState.Empty
        )
        assertFalse(
            "Empty DB must not produce Success state",
            state is FilmListUiState.Success
        )
    }

    @Test
    fun searchWithNoMatchesShouldKeepSuccessStateWithEmptyFilteredList() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("NonExistentFilm")

        val state = viewModel.uiState.value
        assertTrue(
            "State should remain Success when filter finds nothing (films are in DB, just filtered out)",
            state is FilmListUiState.Success
        )
        val successState = state as FilmListUiState.Success
        assertFalse("hasFilteredResults should be false", successState.hasFilteredResults)
        assertTrue("hasActiveFilters should be true", successState.hasActiveFilters)
        assertEquals(0, successState.filteredFilms.size)
        assertEquals(2, successState.films.size)
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
    fun toggleFiltersShouldSwitchFilterVisibility() = runTest {
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

    @Test
    fun retryAfterErrorShouldTransitionLoadingAndReloadSuccessfully() = runTest {
        val dbFlow = MutableStateFlow<List<Film>>(emptyList())

        coEvery { repository.getAllFilms() } returns dbFlow
        coEvery { repository.refreshFilms() } returnsMany listOf(
            Result.failure(IOException("Network error")),
            Result.success(Unit)
        )

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        assertTrue(
            "Expected Error after failed refresh, got: ${viewModel.uiState.value::class.simpleName}",
            viewModel.uiState.value is FilmListUiState.Error
        )
        dbFlow.value = sampleFilms

        viewModel.retry()
        assertTrue(
            "retry() must transition to Loading immediately",
            viewModel.uiState.value is FilmListUiState.Loading
        )

        advanceUntilIdle()

        coVerify(exactly = 2) { repository.refreshFilms() }

        val finalState = viewModel.uiState.value
        assertTrue(
            "Expected Success after retry, got: ${finalState::class.simpleName}",
            finalState is FilmListUiState.Success
        )
        assertEquals(2, (finalState as FilmListUiState.Success).films.size)
    }
}