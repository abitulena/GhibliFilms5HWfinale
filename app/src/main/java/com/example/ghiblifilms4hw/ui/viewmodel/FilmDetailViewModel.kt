package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.ui.state.FilmDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilmDetailViewModel @Inject constructor(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filmId: String = savedStateHandle["filmId"] ?: ""

    var uiState by mutableStateOf<FilmDetailUiState>(FilmDetailUiState.Loading)
        private set

    private var loadJob: Job? = null

    init {
        loadFilmDetail()
    }

    fun loadFilmDetail() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                uiState = FilmDetailUiState.Loading
                val filmFromDb = repository.getFilmById(filmId)
                if (filmFromDb != null) {
                    uiState = FilmDetailUiState.Success(filmFromDb)
                } else {
                    val filmFromApi = repository.getFilmFromApiById(filmId)
                    if (filmFromApi != null) {
                        repository.saveFilmToCache(filmFromApi)
                        val savedFilm = repository.getFilmById(filmId)
                        if (savedFilm != null) {
                            uiState = FilmDetailUiState.Success(savedFilm)
                        } else {
                            uiState = FilmDetailUiState.Error("Failed to save film")
                        }
                    } else {
                        uiState = FilmDetailUiState.Error("Film not found")
                    }
                }
            } catch (e: Exception) {
                uiState = FilmDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleFavorite() {
        val currentState = uiState as? FilmDetailUiState.Success ?: return
        viewModelScope.launch {
            try {
                repository.toggleFavorite(filmId)
                val updatedFilm = repository.getFilmById(filmId)
                if (updatedFilm != null) {
                    uiState = FilmDetailUiState.Success(updatedFilm)
                }
            } catch (e: Exception) {
                uiState = FilmDetailUiState.Error(e.message ?: "Failed to update favourite")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}