package com.example.ghiblifilms4hw.ui.viewmodel

import app.cash.turbine.test
import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class FlowBehaviorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var repository: Repository

    private lateinit var viewModel: FilmListViewModel

    private val sampleFilms = listOf(
        Film(id = "1", title = "Film 1"),
        Film(id = "2", title = "Film 2")
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }
    @Test
    fun completeEmissionSequenceShouldBeLoadingThenEmptyThenSuccess() = runTest {
        val dbFlow = MutableSharedFlow<List<Film>>(replay = 0)

        coEvery { repository.getAllFilms() } returns dbFlow
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        var capturedViewModel: FilmListViewModel? = null
        val states = mutableListOf<FilmListUiState>()
        val collectJob = launch {
            while (capturedViewModel == null) {
                kotlinx.coroutines.yield()
            }
            capturedViewModel!!.uiState.collect { states.add(it) }
        }

        viewModel = FilmListViewModel(repository)
        capturedViewModel = viewModel

        advanceUntilIdle()

        dbFlow.emit(emptyList())
        advanceUntilIdle()

        dbFlow.emit(sampleFilms)
        advanceUntilIdle()

        collectJob.cancel()

        assertTrue(
            "Expected at least 3 states: Loading, Empty, Success. Got: ${states.map { it::class.simpleName }}",
            states.size >= 3
        )
        assertTrue(
            "First emission must be Loading, got: ${states[0]::class.simpleName}",
            states[0] is FilmListUiState.Loading
        )
        assertTrue(
            "Second emission must be Empty, got: ${states[1]::class.simpleName}",
            states[1] is FilmListUiState.Empty
        )
        assertTrue(
            "Third emission must be Success, got: ${states[2]::class.simpleName}",
            states[2] is FilmListUiState.Success
        )
        assertEquals(2, (states[2] as FilmListUiState.Success).films.size)
    }

    @Test
    fun flowUpdatesShouldProduceSuccessStateWhenFilmsAppear() = runTest {
        val dbFlow = MutableStateFlow<List<Film>>(emptyList())

        coEvery { repository.getAllFilms() } returns dbFlow
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        assertTrue(
            "Expected Empty after empty DB, got: ${viewModel.uiState.value::class.simpleName}",
            viewModel.uiState.value is FilmListUiState.Empty
        )

        dbFlow.value = sampleFilms
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Expected Success after films appeared, got: ${state::class.simpleName}",
            state is FilmListUiState.Success
        )
        assertEquals(2, (state as FilmListUiState.Success).films.size)
    }
    @Test
    fun duplicateSearchQueryShouldNotCreateExtraEmissions() = runTest {
        val filmFlow = MutableStateFlow(sampleFilms)
        coEvery { repository.getAllFilms() } returns filmFlow
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem() as FilmListUiState.Success
            assertEquals("", initial.searchQuery)

            viewModel.updateSearchQuery("Film 1")
            val afterFirst = awaitItem() as FilmListUiState.Success
            assertEquals("Film 1", afterFirst.searchQuery)
            assertEquals(1, afterFirst.filteredFilms.size)

            viewModel.updateSearchQuery("Film 1")
            advanceUntilIdle()

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun newSubscriberReceivesCurrentStateImmediately() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(
                "Late subscriber must receive current Success state immediately, got: ${state::class.simpleName}",
                state is FilmListUiState.Success
            )
            assertEquals(2, (state as FilmListUiState.Success).films.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}