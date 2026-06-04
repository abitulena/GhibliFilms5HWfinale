package com.example.ghiblifilms4hw.ui.state

import com.example.ghiblifilms4hw.model.Film

sealed class FilmListUiState {
    object Loading : FilmListUiState()
    data class Error(val message: String) : FilmListUiState()
    object Empty : FilmListUiState()
    data class Success(
        val films: List<Film>,
        val searchQuery: String = "",
        val selectedDirector: String? = null,
        val showFilters: Boolean = false
    ) : FilmListUiState() {
        val filteredFilms: List<Film>
            get() {
                var result = films
                if (searchQuery.isNotBlank()) {
                    result = result.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                (it.description?.contains(searchQuery, ignoreCase = true) == true)
                    }
                }
                if (selectedDirector != null) {
                    result = result.filter { it.director == selectedDirector }
                }
                return result
            }
        val hasFilteredResults: Boolean get() = filteredFilms.isNotEmpty()
        val availableDirectors: List<String> get() = films.mapNotNull { it.director }.distinct().sorted()
        val hasActiveFilters: Boolean get() = searchQuery.isNotBlank() || selectedDirector != null
    }
}

sealed class FilmDetailUiState {
    object Loading : FilmDetailUiState()
    data class Error(val message: String) : FilmDetailUiState()
    data class Success(val film: Film) : FilmDetailUiState()
}

sealed class FavouritesUiState {
    object Loading : FavouritesUiState()
    object Empty : FavouritesUiState()
    data class Success(val films: List<Film>) : FavouritesUiState()
    data class Error(val message: String) : FavouritesUiState()
}