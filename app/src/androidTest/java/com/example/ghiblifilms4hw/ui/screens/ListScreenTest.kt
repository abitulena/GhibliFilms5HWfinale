package com.example.ghiblifilms4hw.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import com.example.ghiblifilms4hw.ui.theme.GhibliFilmsTheme
import com.example.ghiblifilms4hw.ui.viewmodel.FilmListViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @MockK
    private lateinit var viewModel: FilmListViewModel

    private val testFilms = listOf(
        Film(id = "1", title = "Spirited Away", director = "Hayao Miyazaki"),
        Film(id = "2", title = "My Neighbor Totoro", director = "Hayao Miyazaki")
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun errorStateShowsRetryButton() {
        val stateFlow = MutableStateFlow<FilmListUiState>(
            FilmListUiState.Error("Network error")
        )
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun successStateShowsListOfFilms() {
        val stateFlow = MutableStateFlow<FilmListUiState>(
            FilmListUiState.Success(films = testFilms)
        )
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Neighbor Totoro").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsNoFilmsMessage() {
        val stateFlow = MutableStateFlow<FilmListUiState>(FilmListUiState.Empty)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("No films available").assertIsDisplayed()
    }

    @Test
    fun loadingStateShowsProgressIndicator() {
        val stateFlow = MutableStateFlow<FilmListUiState>(FilmListUiState.Loading)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun errorThenRetrySuccessShouldTransitionStates() {
        val stateFlow = MutableStateFlow<FilmListUiState>(
            FilmListUiState.Error("Network error")
        )
        coEvery { viewModel.uiState } returns stateFlow
        coEvery { viewModel.retry() } answers {
            stateFlow.value = FilmListUiState.Success(films = testFilms)
        }

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()

        verify(exactly = 1) { viewModel.retry() }

        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Neighbor Totoro").assertIsDisplayed()
    }

    @Test
    fun filmCardClickShouldNavigateToDetailScreen() {
        val stateFlow = MutableStateFlow<FilmListUiState>(
            FilmListUiState.Success(films = testFilms)
        )
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "list"
                    ) {
                        composable("list") {
                            ListScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable(
                            route = "detail/{filmId}",
                            arguments = listOf(navArgument("filmId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val filmId = backStackEntry.arguments?.getString("filmId") ?: ""
                            DetailScreenStub(filmId = filmId)
                        }
                    }
                }
            }
        }

        composeTestRule.apply {

            onNodeWithText("Spirited Away").assertIsDisplayed()
            onNodeWithText("Spirited Away").performClick()
            waitForIdle()
            onNodeWithText("Detail Screen for film 1").assertIsDisplayed()
        }
    }

    @Test
    fun retryAfterErrorShouldCallViewModelRetry() {
        val stateFlow = MutableStateFlow<FilmListUiState>(
            FilmListUiState.Error("Network error")
        )
        coEvery { viewModel.uiState } returns stateFlow
        coEvery { viewModel.retry() } answers {
            stateFlow.value = FilmListUiState.Success(films = testFilms)
        }

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        verify(exactly = 1) { viewModel.retry() }
    }

    @Test
    fun searchQueryShouldFilterFilms() {
        val initialState = FilmListUiState.Success(
            films = testFilms,
            searchQuery = "",
            selectedDirector = null,
            showFilters = false
        )
        val stateFlow = MutableStateFlow<FilmListUiState>(initialState)
        coEvery { viewModel.uiState } returns stateFlow

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    ListScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Neighbor Totoro").assertIsDisplayed()

        val searchState = FilmListUiState.Success(
            films = testFilms,
            searchQuery = "Spirited",
            selectedDirector = null,
            showFilters = false
        )
        stateFlow.value = searchState
        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Neighbor Totoro").assertDoesNotExist()
    }
}

@Composable
private fun DetailScreenStub(filmId: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "Detail Screen for film $filmId",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}