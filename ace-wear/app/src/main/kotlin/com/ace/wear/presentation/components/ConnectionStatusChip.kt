package com.ace.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Chip que muestra el estado de conexion con el movil con diagnostico detallado.
 *
 * Estados:
 * - CONECTADO (verde): Hay nodos conectados y listener activo
 * - DESCONECTADO (rojo): No hay nodos o fallo de listener
 * - ESPERANDO (amarillo): Listener activo pero sin nodos aun
 */
@Composable
fun ConnectionStatusChip(
    isConnected: Boolean,
    nodeCount: Int = 0,
    lastError: String? = null
) {
    val (backgroundColor, text, subtext) = when {
        isConnected && nodeCount > 0 -> Triple(
            Color(0xFF4CAF50),
            "Conectado",
            "$nodeCount nodo(s)"
        )
        !isConnected && lastError != null -> Triple(
            Color(0xFFF44336),
            "Error",
            lastError
        )
        else -> Triple(
            Color(0xFFFFA000),
            "Esperando...",
            "Sin nodos"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption3,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        if (subtext.isNotEmpty()) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.caption3,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}