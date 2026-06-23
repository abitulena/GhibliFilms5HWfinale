package com.example.ghiblifilms4hw.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghiblifilms4hw.data.Repository
import com.example.ghiblifilms4hw.ui.state.FavouritesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    var uiState by mutableStateOf<FavouritesUiState>(FavouritesUiState.Loading)
        private set

    private var collectJob: kotlinx.coroutines.Job? = null

    init {
        loadFavourites()
    }

    fun loadFavourites() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                repository.getFavoriteFilms()
                    .collect { films ->
                        uiState = if (films.isEmpty()) {
                            FavouritesUiState.Empty
                        } else {
                            FavouritesUiState.Success(films)
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = FavouritesUiState.Error(e.message ?: "Unknown error")
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