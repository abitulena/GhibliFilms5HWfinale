package com.example.ghiblifilms4hw.ui.viewmodel

import com.example.ghiblifilms4hw.MainDispatcherRule
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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

        assertTrue(
            "State must be Loading immediately after construction",
            viewModel.uiState is FilmListUiState.Loading
        )
    }

    @Test
    fun successfulDataLoadShouldResultInSuccessState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(sampleFilms)
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is FilmListUiState.Success)
        assertEquals(2, (state as FilmListUiState.Success).films.size)
    }

    @Test
    fun networkErrorWithEmptyDatabaseShouldResultInErrorState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.failure(IOException("Network error"))

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state is FilmListUiState.Error)
        assertEquals("Network error", (state as FilmListUiState.Error).message)
    }

    @Test
    fun emptyDatabaseWithSuccessfulRefreshShouldResultInEmptyState() = runTest {
        coEvery { repository.getAllFilms() } returns flowOf(emptyList())
        coEvery { repository.refreshFilms() } returns Result.success(Unit)

        viewModel = FilmListViewModel(repository)
        advanceUntilIdle()

        assertFalse(
            "Empty DB must not produce Success state",
            viewModel.uiState is FilmListUiState.Success
        )
        assertTrue(viewModel.uiState is FilmListUiState.Empty)
    }
    @Test
    fun retryAfterErrorShouldInitiateNewRequestAndTransitionToSuccess() = runTest {
        val dbChannel = Channel<List<Film>>(Channel.UNLIMITED)
        coEvery { repository.getAllFilms() } returns dbChannel.consumeAsFlow()
        coEvery { repository.refreshFilms() } returnsMany listOf(
            Result.failure(IOException("Network error")),
            Result.success(Unit)
        )

        viewModel = FilmListViewModel(repository)

        dbChannel.send(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.uiState is FilmListUiState.Error)

        viewModel.retry()
        assertTrue(
            "retry() must set Loading immediately",
            viewModel.uiState is FilmListUiState.Loading
        )

        dbChannel.send(sampleFilms)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.refreshFilms() }
        assertTrue(viewModel.uiState is FilmListUiState.Success)
        assertEquals(2, (viewModel.uiState as FilmListUiState.Success).films.size)

        dbChannel.close()
    }
    @Test
    fun uiStateShouldFollowFullSequenceLoadingErrorLoadingSuccess() = runTest {
        val dbChannel = Channel<List<Film>>(Channel.UNLIMITED)
        coEvery { repository.getAllFilms() } returns dbChannel.consumeAsFlow()
        coEvery { repository.refreshFilms() } returnsMany listOf(
            Result.failure(IOException("Network error")),
            Result.success(Unit)
        )

        val states = mutableListOf<String>()

        fun recordState() {
            val name = viewModel.uiState::class.simpleName ?: return
            if (states.lastOrNull() != name) states.add(name)
        }

        viewModel = FilmListViewModel(repository)
        recordState()

        dbChannel.send(emptyList())
        advanceUntilIdle()
        recordState()

        viewModel.retry()
        recordState()

        dbChannel.send(sampleFilms)
        advanceUntilIdle()
        recordState()

        dbChannel.close()

        assertEquals(
            listOf("Loading", "Error", "Loading", "Success"),
            states
        )
    }
}