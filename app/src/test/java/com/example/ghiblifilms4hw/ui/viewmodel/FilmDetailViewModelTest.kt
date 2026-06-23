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

    private val sampleFilm = Film(
        id = "1",
        title = "Spirited Away",
        description = "Great film",
        director = "Hayao Miyazaki",
        isFavorite = false
    )

    private val sampleFilmDto = FilmDto(
        id = "1",
        title = "Spirited Away",
        description = "Great film",
        director = "Hayao Miyazaki",
        producer = "Toshio Suzuki",
        releaseDate = "2001",
        rtScore = "97",
        image = "url"
    )

    private fun savedStateHandle(filmId: String = "1") =
        SavedStateHandle(mapOf("filmId" to filmId))

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun initialStateIsLoading() = runTest {
        coEvery { repository.getFilmById("1") } returns sampleFilm

        viewModel = FilmDetailViewModel(repository, savedStateHandle())

        assertTrue(
            "State must be Loading immediately after construction",
            viewModel.uiState is FilmDetailUiState.Loading
        )
    }

    @Test
    fun filmFoundInDbProducesSuccessState() = runTest {
        coEvery { repository.getFilmById("1") } returns sampleFilm

        viewModel = FilmDetailViewModel(repository, savedStateHandle())
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(
            "Expected Success when film is in DB, got: ${state::class.simpleName}",
            state is FilmDetailUiState.Success
        )
        assertEquals("Spirited Away", (state as FilmDetailUiState.Success).film.title)
    }

    @Test
    fun filmNotInDbFetchedFromApiAndCachedProducesSuccessState() = runTest {
        coEvery { repository.getFilmById("1") } returnsMany listOf(null, sampleFilm)
        coEvery { repository.getFilmFromApiById("1") } returns sampleFilmDto
        coEvery { repository.saveFilmToCache(sampleFilmDto) } returns Result.success(Unit)

        viewModel = FilmDetailViewModel(repository, savedStateHandle())
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(
            "Expected Success after API fetch and cache, got: ${state::class.simpleName}",
            state is FilmDetailUiState.Success
        )
        coVerify(exactly = 1) { repository.saveFilmToCache(sampleFilmDto) }
    }

    @Test
    fun filmNotInDbAndNotFoundInApiProducesErrorState() = runTest {
        coEvery { repository.getFilmById("1") } returns null
        coEvery { repository.getFilmFromApiById("1") } returns null

        viewModel = FilmDetailViewModel(repository, savedStateHandle())
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(
            "Expected Error when film not found anywhere, got: ${state::class.simpleName}",
            state is FilmDetailUiState.Error
        )
        assertEquals("Film not found", (state as FilmDetailUiState.Error).message)
    }

    @Test
    fun apiErrorProducesErrorState() = runTest {
        coEvery { repository.getFilmById("1") } returns null
        coEvery { repository.getFilmFromApiById("1") } throws RuntimeException("Network error")

        viewModel = FilmDetailViewModel(repository, savedStateHandle())
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(
            "Expected Error on API exception, got: ${state::class.simpleName}",
            state is FilmDetailUiState.Error
        )
        assertEquals("Network error", (state as FilmDetailUiState.Error).message)
    }

    @Test
    fun toggleFavoriteUpdatesFilmState() = runTest {
        val updatedFilm = sampleFilm.copy(isFavorite = true)
        coEvery { repository.getFilmById("1") } returnsMany listOf(sampleFilm, updatedFilm)
        coEvery { repository.toggleFavorite("1") } returns Result.success(Unit)

        viewModel = FilmDetailViewModel(repository, savedStateHandle())
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        val state = viewModel.uiState as FilmDetailUiState.Success
        assertTrue("Film should be marked as favourite", state.film.isFavorite)
        coVerify(exactly = 1) { repository.toggleFavorite("1") }
    }

    @Test
    fun toggleFavoriteOnNonSuccessStateDoesNothing() = runTest {
        coEvery { repository.getFilmById("1") } returns null
        coEvery { repository.getFilmFromApiById("1") } returns null

        viewModel = FilmDetailViewModel(repository, savedStateHandle())
        advanceUntilIdle()

        assertTrue(viewModel.uiState is FilmDetailUiState.Error)

        viewModel.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.toggleFavorite(any()) }
    }

    @Test
    fun missingFilmIdProducesErrorState() = runTest {
        coEvery { repository.getFilmById("") } returns null
        coEvery { repository.getFilmFromApiById("") } returns null

        viewModel = FilmDetailViewModel(repository, savedStateHandle(filmId = ""))
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(
            "Expected Error for missing filmId, got: ${state::class.simpleName}",
            state is FilmDetailUiState.Error
        )
    }
}