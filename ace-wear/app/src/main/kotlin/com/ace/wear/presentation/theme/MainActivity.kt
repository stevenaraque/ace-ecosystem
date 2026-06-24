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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Permiso BODY_SENSORS: ${if (isGranted) "CONCEDIDO" else "DENEGADO"}")
        viewModel.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(hasPermission)

        viewModel.setPermissionLauncher {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }

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