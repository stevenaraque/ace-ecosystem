// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/components/ConnectionStatusChip.kt

package com.ace.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Chip que muestra el estado de conexion con el movil.
 *
 * - Verde: conectado
 * - Rojo: desconectado
 */
@Composable
fun ConnectionStatusChip(
    isConnected: Boolean
) {
    val backgroundColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
    val text = if (isConnected) "Conectado" else "Desconectado"

    Text(
        text = text,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.caption3,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}