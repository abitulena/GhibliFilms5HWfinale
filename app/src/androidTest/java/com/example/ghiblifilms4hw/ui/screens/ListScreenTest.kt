package com.example.ghiblifilms4hw.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import com.example.ghiblifilms4hw.ui.viewmodel.FilmListViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ListScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @MockK
    private lateinit var viewModel: FilmListViewModel

    private val testFilms = listOf(
        Film(id = "1", title = "Spirited Away", director = "Miyazaki"),
        Film(id = "2", title = "Totoro", director = "Miyazaki")
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun errorStateShowsRetryButton() {
        val errorState = FilmListUiState.Error("Network error")
        val stateFlow = MutableStateFlow<FilmListUiState>(errorState)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            val navController = rememberNavController()
            ListScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun successStateShowsListOfFilms() {
        val successState = FilmListUiState.Success(films = testFilms)
        val stateFlow = MutableStateFlow<FilmListUiState>(successState)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            val navController = rememberNavController()
            ListScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
        composeTestRule.onNodeWithText("Totoro").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsNoFilmsMessage() {
        val emptyState = FilmListUiState.Empty
        val stateFlow = MutableStateFlow<FilmListUiState>(emptyState)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            val navController = rememberNavController()
            ListScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("No films available").assertIsDisplayed()
    }

    @Test
    fun loadingStateShowsProgressIndicator() {
        val stateFlow = MutableStateFlow<FilmListUiState>(FilmListUiState.Loading)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            val navController = rememberNavController()
            ListScreen(navController = navController, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }
}