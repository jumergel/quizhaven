package io.github.jumergel.quizhaven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jumergel.quizhaven.ui.theme.QuizHavenTheme

class MainActivity : ComponentActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizHavenTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(navController)   // pass controller here
                    }

                    composable("login") {
                        Image(
                            painter = painterResource(R.drawable.plant_home),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        LoginScreen(navController, paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp))
                    }

                    composable("signup") {
                        Image(
                            painter = painterResource(R.drawable.plant_home),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        SignUpScreen(navController)
                    }

                    composable(route = "input/{projectId}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                        TextInput(navController, projectId)
                    }

                    composable("survey/{projectId}/{inputId}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")!!
                        val inputId = backStackEntry.arguments?.getString("inputId")!!
                        Survey(navController, projectId, inputId)
                    }

                    composable(route = "teaching/{projectId}/{inputId}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")!!
                        val inputId = backStackEntry.arguments?.getString("inputId")!!
                        TeachingScreen(navController, projectId, inputId)
                    }

                    composable(route = "quiz/{projectId}/{inputId}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")!!
                        val inputId = backStackEntry.arguments?.getString("inputId")!!
                        QuizScreen(navController, projectId, inputId)
                    }

                    composable("projects") {
                        ProjectsScreen(navController)
                    }

                    composable("textInput/{projectId}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                        TextInput(navController, projectId)
                    }
                    composable("stats/{projectId}/{inputId}") { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")!!
                        val inputId = backStackEntry.arguments?.getString("inputId")!!
                        StatsScreen(navController, projectId, inputIdForBackNav = inputId)
                    }
                }
            }
        }
    }
}