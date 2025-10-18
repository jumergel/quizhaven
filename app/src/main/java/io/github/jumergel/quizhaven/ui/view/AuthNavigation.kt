package io.github.jumergel.quizhaven.ui.view

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost

@Composable
fun AppNavigator(){
    val navController = rememberNavController()
    NavHost(navController, startDestination = "login"){
        composable(route = "login"){}
        composable(route = "register"){}
    }
}