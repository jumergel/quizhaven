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
//                    startDestination = "home"
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
                    composable("input") {
                        TextInput(navController)   // pass controller here
                    }
                    composable("survey/{inputId}") { backStackEntry ->
                        val inputId = backStackEntry.arguments?.getString("inputId") ?: return@composable
                        Survey(navController, inputId)
                    }

                    composable("teaching/{inputId}") { backStackEntry ->
                        val inputId = backStackEntry.arguments?.getString("inputId")!!
                        TeachingScreen(navController, inputId)  // second screen
                    }

                    composable("quiz/{inputId}") { backStackEntry ->
                        val inputId = backStackEntry.arguments?.getString("inputId")!!
                        QuizScreen(navController, inputId)
                    }

                }
            }
        }
    }
}