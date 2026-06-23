package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilmListViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    var uiState by mutableStateOf<FilmListUiState>(FilmListUiState.Loading)
        private set

    private var dbCollectJob: kotlinx.coroutines.Job? = null
    private var refreshJob: kotlinx.coroutines.Job? = null

    init {
        loadFilmsFromDb()
        refreshIfNeeded()
    }

    private fun loadFilmsFromDb() {
        dbCollectJob?.cancel()
        dbCollectJob = viewModelScope.launch {
            try {
                repository.getAllFilms()
                    .collect { films ->
                        val currentState = uiState
                        if (films.isEmpty() && currentState !is FilmListUiState.Error) {
                            uiState = FilmListUiState.Empty
                        } else if (films.isNotEmpty()) {
                            val searchQuery = if (currentState is FilmListUiState.Success) currentState.searchQuery else ""
                            val selectedDirector = if (currentState is FilmListUiState.Success) currentState.selectedDirector else null
                            val showFilters = if (currentState is FilmListUiState.Success) currentState.showFilters else false
                            uiState = FilmListUiState.Success(
                                films = films,
                                searchQuery = searchQuery,
                                selectedDirector = selectedDirector,
                                showFilters = showFilters
                            )
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = FilmListUiState.Error(e.message ?: "Database error")
            }
        }
    }

    private fun refreshIfNeeded() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            repository.refreshFilms().fold(
                onSuccess = { },
                onFailure = { e ->
                    val currentState = uiState
                    if (currentState is FilmListUiState.Empty || currentState is FilmListUiState.Loading) {
                        uiState = FilmListUiState.Error(e.message ?: "Failed to load films")
                    }
                }
            )
        }
    }

    fun retry() {
        uiState = FilmListUiState.Loading
        refreshIfNeeded()
    }

    fun updateSearchQuery(query: String) {
        val currentState = uiState
        if (currentState is FilmListUiState.Success) {
            uiState = currentState.copy(searchQuery = query)
        }
    }

    fun updateDirectorFilter(director: String?) {
        val currentState = uiState
        if (currentState is FilmListUiState.Success) {
            uiState = currentState.copy(selectedDirector = director)
        }
    }

    fun toggleFilters() {
        val currentState = uiState
        if (currentState is FilmListUiState.Success) {
            uiState = currentState.copy(showFilters = !currentState.showFilters)
        }
    }

    fun resetFilters() {
        val currentState = uiState
        if (currentState is FilmListUiState.Success) {
            uiState = currentState.copy(searchQuery = "", selectedDirector = null)
        }
    }

    fun toggleFavorite(filmId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(filmId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        dbCollectJob?.cancel()
        refreshJob?.cancel()
    }
}