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
import com.ace.mobile.presentation.home.HomeScreen
import com.ace.mobile.presentation.profile.ProfileScreen
import com.ace.mobile.presentation.profile.ProfileViewModel
import com.ace.mobile.presentation.ranking.RankingScreen
import com.ace.mobile.data.local.database.dao.UserDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                val currentUser by userDao.observeCurrentUser().collectAsState(initial = null)
                val isLoggedIn = currentUser?.accessToken?.isNotEmpty() == true

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    // Si hay sesión activa, empieza en Home. Si no, en Login.
                    startDestination = if (isLoggedIn) "home_screen_route" else "login_screen_route",
                    modifier = Modifier.padding(innerPadding)
                ) {

                    // RUTA A: Login
                    composable("login_screen_route") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home_screen_route") {
                                    popUpTo("login_screen_route") { inclusive = true }
                                }
                            }
                        )
                    }

                    // RUTA B: Home (NUEVA)
                    composable("home_screen_route") {
                        HomeScreen(navController = navController)
                    }

                    // RUTA C: Sesión de Ejercicio
                    composable("session_screen_route") {
                        val currentUserId = currentUser?.userId
                        if (currentUserId != null) {
                            SessionScreen(userId = currentUserId)
                        } else {
                            navController.navigate("login_screen_route") {
                                popUpTo("session_screen_route") { inclusive = true }
                            }
                        }
                    }

                    // RUTA D: Ranking (NUEVA)
                    composable("ranking_screen_route") {
                        RankingScreen()
                    }

                    // RUTA E: Perfil (solo logout)
                    composable("profile_screen_route") {
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