package com.ace.wear.presentation.theme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ace.wear.presentation.session.SessionScreen
import com.ace.wear.presentation.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity principal del reloj Wear OS.
 *
 * Responsabilidades:
 * - Instalar splash screen
 * - Inyectar SessionViewModel via Hilt
 * - Inicializar el repositorio de salud (WearHealthRepository)
 * - Renderizar SessionScreen con Compose
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Inicializar el repositorio de salud (escuchar comandos del movil)
        viewModel.initialize()

        setContent {
            WearTheme {
                SessionScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        viewModel.dispose()  // <-- Usar dispose(), no onCleared()
        super.onDestroy()
    }
}