package com.example.reproductor

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
import com.example.reproductor.screens.VideoListScreen
import com.example.reproductor.screens.VideoPlayerScreen
import com.example.reproductor.ui.theme.ReproductorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReproductorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "videoList"
                    ) {
                        composable("videoList") {
                            VideoListScreen(navController = navController)
                        }
                        
                        composable(
                            route = "player/{videoId}",
                            arguments = listOf(
                                navArgument("videoId") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                            VideoPlayerScreen(
                                videoId = videoId,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}