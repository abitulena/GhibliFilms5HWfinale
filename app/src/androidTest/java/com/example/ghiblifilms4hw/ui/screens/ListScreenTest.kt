package com.example.ghiblifilms4hw.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import com.example.ghiblifilms4hw.ui.theme.GhibliFilmsTheme
import com.example.ghiblifilms4hw.ui.viewmodel.FilmListViewModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    fun successStateShowsListOfFilms() {
        every { viewModel.uiState } returns FilmListUiState.Success(films = testFilms)

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ListScreen(navController = rememberNavController(), viewModel = viewModel)
                }
            }
        }

        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Neighbor Totoro").assertIsDisplayed()
    }
    @Test
    fun errorThenRetrySuccessShouldTransitionStates() {
        var state by mutableStateOf<FilmListUiState>(FilmListUiState.Error("Network error"))
        every { viewModel.uiState } answers { state }
        every { viewModel.retry() } answers { state = FilmListUiState.Success(films = testFilms) }

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ListScreen(navController = rememberNavController(), viewModel = viewModel)
                }
            }
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        verify(exactly = 1) { viewModel.retry() }
        composeTestRule.onNodeWithText("Spirited Away").assertIsDisplayed()
    }
    @Test
    fun filmCardClickShouldNavigateToDetailScreenWithCorrectId() {
        every { viewModel.uiState } returns FilmListUiState.Success(films = testFilms)

        composeTestRule.setContent {
            GhibliFilmsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            ListScreen(navController = navController, viewModel = viewModel)
                        }
                        composable(
                            route = "detail/{filmId}",
                            arguments = listOf(navArgument("filmId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            DetailScreenStub(filmId = backStackEntry.arguments?.getString("filmId") ?: "")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Spirited Away").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Detail Screen for film 1").assertIsDisplayed()
    }
}

@Composable
private fun DetailScreenStub(filmId: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Text(text = "Detail Screen for film $filmId", style = MaterialTheme.typography.headlineMedium)
    }
}