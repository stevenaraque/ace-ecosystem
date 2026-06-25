package com.ace.mobile.feature.wear.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity de diagnostico para verificar conexion Wear OS.
 *
 * Uso temporal: lanzar desde Android Studio cambiando el android:name en manifest
 * o agregar un boton oculto en SessionScreen para debug.
 *
 * BORRAR cuando la comunicacion Wear OS funcione correctamente.
 */
@AndroidEntryPoint
class WearDiagnosticActivity : ComponentActivity() {

    private val viewModel: WearDiagnosticViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "WearDiagnosticActivity iniciada")

        setContent {
            MaterialTheme {
                WearDiagnosticScreen(
                    viewModel = viewModel,
                    onClose = { finish() }
                )
            }
        }

        viewModel.refreshStatus()
    }

    companion object {
        private const val TAG = "WearDiagnosticActivity"
    }
}