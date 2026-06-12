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

                // 1. Inicializamos el controlador de navegación
                val navController = rememberNavController()

                // 2. Configuramos el NavHost que gestionará el intercambio de pantallas
                NavHost(
                    navController = navController,
                    startDestination = "login_screen_route", // Pantalla de inicio
                    modifier = Modifier.padding(innerPadding) // Aplica los márgenes seguros del Edge-to-Edge
                ) {

                    // RUTA A: Pantalla de Login
                    composable("login_screen_route") {
                        LoginScreen(
                            onLoginSuccess = {
                                // Al iniciar sesión con éxito, navegamos al Perfil
                                navController.navigate("profile_screen_route") {
                                    // Limpiamos el Login del historial para que el usuario no pueda "volver atrás"
                                    popUpTo("login_screen_route") { inclusive = true }
                                }
                            }
                        )
                    }

                    // RUTA B: Pantalla de Perfil (La que acabamos de crear)
                    composable("profile_screen_route") {
                        // Hilt inyecta el LogoutUseCase automáticamente en este ViewModel
                        val profileViewModel: ProfileViewModel = hiltViewModel()

                        ProfileScreen(
                            navController = navController,
                            viewModel = profileViewModel
                        )
                    }
                }
            }
        }
    }
}