package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.ui.state.FavouritesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavouritesUiState>(FavouritesUiState.Loading)
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    private var collectJob: kotlinx.coroutines.Job? = null

    init {
        loadFavourites()
    }

    fun loadFavourites() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.getFavoriteFilms()
                .catch { e ->
                    _uiState.value = FavouritesUiState.Error(e.message ?: "Unknown error")
                }
                .collect { films ->
                    _uiState.value = if (films.isEmpty()) {
                        FavouritesUiState.Empty
                    } else {
                        FavouritesUiState.Success(films)
                    }
                }
        }
    }

    fun removeFromFavourites(filmId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(filmId)
            loadFavourites()
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}