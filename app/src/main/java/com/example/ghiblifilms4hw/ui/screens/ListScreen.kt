package com.example.ghiblifilms4hw.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ghiblifilms4hw.model.Film
import com.example.ghiblifilms4hw.ui.state.FilmListUiState
import com.example.ghiblifilms4hw.ui.viewmodel.FilmListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    navController: NavController,
    viewModel: FilmListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio Ghibli Films") },
                actions = {
                    IconButton(onClick = { navController.navigate("favourites") }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favourites")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is FilmListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading...")
                        }
                    }
                }
                is FilmListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is FilmListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No films available")
                    }
                }
                is FilmListUiState.Success -> {
                    SuccessContent(
                        uiState = state,
                        onFilmClick = { filmId ->
                            navController.navigate("detail/$filmId")
                        },
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onDirectorFilterChange = { viewModel.updateDirectorFilter(it) },
                        onToggleFilters = { viewModel.toggleFilters() },
                        onResetFilters = { viewModel.resetFilters() },
                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    uiState: FilmListUiState.Success,
    onFilmClick: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onDirectorFilterChange: (String?) -> Unit,
    onToggleFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search...") },
        singleLine = true
    )

    Button(
        onClick = onToggleFilters,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (uiState.showFilters) "Hide Filters" else "Show Filters")
    }

    if (uiState.showFilters) {
        Column {
            Text("Filter by Director:", fontWeight = FontWeight.Medium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedDirector == null,
                        onClick = { onDirectorFilterChange(null) },
                        label = { Text("All") }
                    )
                }
                items(uiState.availableDirectors) { director ->
                    FilterChip(
                        selected = uiState.selectedDirector == director,
                        onClick = { onDirectorFilterChange(director) },
                        label = { Text(director) }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (!uiState.hasFilteredResults) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No results found")
                if (uiState.hasActiveFilters) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onResetFilters) { Text("Reset Filters") }
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.filteredFilms, key = { it.id }) { film ->
                FilmCard(
                    film = film,
                    onClick = { onFilmClick(film.id) },
                    onFavoriteClick = { onFavoriteClick(film.id) }
                )
            }
        }
    }
}

@Composable
private fun FilmCard(
    film: Film,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = film.image,
                contentDescription = film.title,
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = film.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Director: ${film.director ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Year: ${film.releaseDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Score: ${film.rtScore ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (film.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (film.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (film.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}