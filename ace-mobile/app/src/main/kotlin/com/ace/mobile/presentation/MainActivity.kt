package com.ace.mobile.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ace.mobile.presentation.auth.LoginScreen
import com.ace.mobile.presentation.exercise.SessionScreen
import com.ace.mobile.presentation.profile.ProfileScreen
import com.ace.mobile.presentation.profile.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login_screen_route",
                    modifier = Modifier.padding(innerPadding)
                ) {

                    // RUTA A: Pantalla de Login (existente)
                    composable("login_screen_route") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("profile_screen_route") {
                                    popUpTo("login_screen_route") { inclusive = true }
                                }
                            }
                        )
                    }

                    // RUTA B: Pantalla de Perfil (existente)
                    composable("profile_screen_route") {
                        val profileViewModel: ProfileViewModel = hiltViewModel()

                        ProfileScreen(
                            navController = navController,
                            viewModel = profileViewModel
                        )
                    }

                    // RUTA C: Pantalla de Sesión de Ejercicio (NUEVA)
                    composable("session_screen_route") {
                        // TODO: Reemplazar "user-123" con el userId real del usuario logueado
                        SessionScreen(
                            userId = "user-123" // Temporal hasta integrar auth
                        )
                    }
                }
            }
        }
    }
}