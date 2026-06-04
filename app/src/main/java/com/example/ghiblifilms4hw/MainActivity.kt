package com.example.ghiblifilms4hw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ghiblifilms4hw.ui.screens.DetailScreen
import com.example.ghiblifilms4hw.ui.screens.FavouritesScreen
import com.example.ghiblifilms4hw.ui.screens.ListScreen
import com.example.ghiblifilms4hw.ui.theme.GhibliFilmsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhibliFilmsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "list"
                    ) {
                        composable("list") {
                            ListScreen(navController = navController)
                        }
                        composable("favourites") {
                            FavouritesScreen(navController = navController)
                        }
                        composable(
                            route = "detail/{filmId}",
                            arguments = listOf(navArgument("filmId") { type = NavType.StringType })
                        ) { _ ->
                            DetailScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}