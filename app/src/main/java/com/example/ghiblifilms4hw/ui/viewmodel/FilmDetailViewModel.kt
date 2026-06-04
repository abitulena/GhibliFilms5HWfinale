package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.ui.state.FilmDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilmDetailViewModel @Inject constructor(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val filmId: String = savedStateHandle["filmId"] ?: ""

    private val _uiState = MutableStateFlow<FilmDetailUiState>(FilmDetailUiState.Loading)
    val uiState: StateFlow<FilmDetailUiState> = _uiState

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadFilmDetail()
    }

    fun loadFilmDetail() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = FilmDetailUiState.Loading

            val filmFromDb = repository.getFilmById(filmId)
            if (filmFromDb != null) {
                _uiState.value = FilmDetailUiState.Success(filmFromDb)
            } else {
                val filmFromApi = repository.getFilmFromApiById(filmId)
                if (filmFromApi != null) {
                    repository.saveFilmToCache(filmFromApi)
                    val savedFilm = repository.getFilmById(filmId)
                    if (savedFilm != null) {
                        _uiState.value = FilmDetailUiState.Success(savedFilm)
                    } else {
                        _uiState.value = FilmDetailUiState.Error("Failed to save film")
                    }
                } else {
                    _uiState.value = FilmDetailUiState.Error("Film not found")
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(filmId)
            val updatedFilm = repository.getFilmById(filmId)
            if (updatedFilm != null) {
                _uiState.value = FilmDetailUiState.Success(updatedFilm)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}