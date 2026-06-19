package com.ace.wear.presentation.theme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ace.wear.presentation.session.SessionScreen
import com.ace.wear.presentation.session.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity principal del reloj Wear OS.
 *
 * Responsabilidades:
 * - Instalar splash screen
 * - Solicitar permiso BODY_SENSORS en runtime (solo cuando se necesita)
 * - Inyectar SessionViewModel via Hilt
 * - Inicializar el repositorio de salud
 * - Renderizar SessionScreen con Compose
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
    }

    /**
     * Launcher para solicitar permiso BODY_SENSORS.
     * El resultado se envia al ViewModel para que decida si iniciar la sesion.
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Permiso BODY_SENSORS: ${if (isGranted) "CONCEDIDO" else "DENEGADO"}")
        viewModel.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Verificar si ya tiene permiso (para mostrar estado correcto en UI)
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(hasPermission)

        // Registrar el launcher de permiso en el ViewModel
        // para que lo invoque cuando reciba START
        viewModel.setPermissionLauncher {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }

        // Inicializar el repositorio de salud (escuchar comandos del movil)
        viewModel.initialize()

        setContent {
            WearTheme {
                SessionScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        viewModel.dispose()
        super.onDestroy()
    }
}