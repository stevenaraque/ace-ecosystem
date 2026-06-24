// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/session/IdleScreen.kt
package com.ace.wear.presentation.session

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.AceLogo
import com.ace.wear.presentation.components.AceRingBackground
import com.ace.wear.presentation.components.ConnectionStatusChip
import com.ace.wear.presentation.theme.AceRed
import com.ace.wear.presentation.theme.AceTextSecondary
import com.ace.wear.presentation.theme.UnifrakturMaguntia

/**
 * Pantalla de reposo. Sin sesion activa, esperando START del movil.
 */
@Composable
fun IdleScreen(
    isConnected: Boolean,
    nodeCount: Int,
    lastError: String?,
    hasSensorPermission: Boolean,
    permissionDenied: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Fondo: anillos estaticos
        AceRingBackground(animated = false)

        // Contenido
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Chip de conexion (arriba)
            ConnectionStatusChip(
                isConnected = isConnected,
                nodeCount = nodeCount,
                lastError = lastError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Logo A.C.E centrado
            AceLogo(size = 70.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Texto "A.C.E"
            Text(
                text = "A.C.E",
                fontFamily = UnifrakturMaguntia,
                fontSize = 16.sp,
                color = AceRed,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Estado de permiso o mensaje de espera
            when {
                permissionDenied && !isConnected -> {
                    Text(
                        text = "Permiso denegado",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center
                    )
                }
                !hasSensorPermission -> {
                    Text(
                        text = "Permiso requerido",
                        style = MaterialTheme.typography.caption2,
                        color = AceTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = "Esperando START...",
                        fontFamily = UnifrakturMaguntia,
                        fontSize = 12.sp,
                        color = AceTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}