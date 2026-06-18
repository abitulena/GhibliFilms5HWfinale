package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilmListViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FilmListUiState>(FilmListUiState.Loading)
    val uiState: StateFlow<FilmListUiState> = _uiState

    private var dbCollectJob: kotlinx.coroutines.Job? = null
    private var refreshJob: kotlinx.coroutines.Job? = null

    init {
        loadFilmsFromDb()
        refreshIfNeeded()
    }

    private fun loadFilmsFromDb() {
        dbCollectJob?.cancel()
        dbCollectJob = viewModelScope.launch {
            repository.getAllFilms()
                .catch { e ->
                    _uiState.value = FilmListUiState.Error(e.message ?: "Database error")
                }
                .collect { films ->
                    val currentState = _uiState.value
                    if (films.isEmpty() && currentState !is FilmListUiState.Error) {
                        _uiState.value = FilmListUiState.Empty
                    } else if (films.isNotEmpty()) {
                        val searchQuery = if (currentState is FilmListUiState.Success) currentState.searchQuery else ""
                        val selectedDirector = if (currentState is FilmListUiState.Success) currentState.selectedDirector else null
                        val showFilters = if (currentState is FilmListUiState.Success) currentState.showFilters else false
                        _uiState.value = FilmListUiState.Success(
                            films = films,
                            searchQuery = searchQuery,
                            selectedDirector = selectedDirector,
                            showFilters = showFilters
                        )
                    }
                }
        }
    }

    private fun refreshIfNeeded() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            repository.refreshFilms().fold(
                onSuccess = { },
                onFailure = { e ->
                    val currentState = _uiState.value
                    if (currentState is FilmListUiState.Empty || currentState is FilmListUiState.Loading) {
                        _uiState.value = FilmListUiState.Error(e.message ?: "Failed to load films")
                    }
                }
            )
        }
    }
    fun retry() {
        _uiState.value = FilmListUiState.Loading
        refreshIfNeeded()
    }

    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        if (currentState is FilmListUiState.Success) {
            _uiState.value = currentState.copy(searchQuery = query)
        }
    }

    fun updateDirectorFilter(director: String?) {
        val currentState = _uiState.value
        if (currentState is FilmListUiState.Success) {
            _uiState.value = currentState.copy(selectedDirector = director)
        }
    }

    fun toggleFilters() {
        val currentState = _uiState.value
        if (currentState is FilmListUiState.Success) {
            _uiState.value = currentState.copy(showFilters = !currentState.showFilters)
        }
    }

    fun resetFilters() {
        val currentState = _uiState.value
        if (currentState is FilmListUiState.Success) {
            _uiState.value = currentState.copy(searchQuery = "", selectedDirector = null)
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