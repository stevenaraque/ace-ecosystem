package com.ace.wear.presentation.session

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.AceRingBackground
import com.ace.wear.presentation.theme.*

/**
 * Pantalla de reposo - Versión de Pruebas de Estado y Permisos.
 * Mantiene el centro geométrico bloqueado sin usar el componente de logo.
 */
@Composable
fun IdleScreen(
    isConnected: Boolean,
    nodeCount: Int,
    lastError: String?,
    hasSensorPermission: Boolean,
    permissionDenied: Boolean
) {
    // Animación sincronizada para los efectos neón
    val infiniteTransition = rememberInfiniteTransition(label = "idleLayout")
    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "synchronizedNeonPulse"
    )

    // Rojo brillante de alta saturación
    val SaturatedNeonRed = Color(0xFFFF0018)

    // 1. LÓGICA DE CONEXIÓN: Integra el estado ("Conectado"/"Buscando") junto a los Nodos
    val connectionText: String
    val connectionColor: Color

    when {
        isConnected && nodeCount > 0 -> {
            connectionColor = AceGreen
            connectionText = "Conectado ($nodeCount Nodos)"
        }
        !isConnected && lastError != null -> {
            connectionColor = SaturatedNeonRed
            connectionText = "Error"
        }
        else -> {
            connectionColor = AceOrange
            connectionText = "Buscando ($nodeCount Nodos)"
        }
    }

    // 2. LÓGICA OPERACIONAL CORREGIDA: Permite evaluar fallos de permisos en paralelo a la conexión
    val operationalText: String
    val operationalColor: Color

    when {
        permissionDenied -> {
            // Forzado: Se muestra inmediatamente si falta el permiso, ignorando el estado de red
            operationalText = "Permiso denegado"
            operationalColor = SaturatedNeonRed
        }
        !hasSensorPermission -> {
            operationalText = "Permiso requerido"
            operationalColor = AceTextSecondary
        }
        else -> {
            operationalText = "Esperando START"
            operationalColor = Color.White.copy(alpha = 0.65f)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Fondo estático de anillos góticos
        AceRingBackground(
            modifier = Modifier.fillMaxSize(),
            animated = false,
            showCenterDot = false
        )

        // ESTRUCTURA SIMÉTRICA DE CONTROL DE EJES
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // MITAD SUPERIOR: Controla la zona de Conexión + Nodos
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp) // Separación limpia del título
                ) {
                    Text(
                        text = "●",
                        fontSize = 5.sp,
                        color = connectionColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = connectionText.uppercase(),
                        fontFamily = CinzelDecorative,
                        fontSize = 7.sp, // Tamaño compacto ideal para textos compuestos con variables
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(
                                color = connectionColor.copy(alpha = neonAlpha),
                                offset = Offset(0f, 0f),
                                blurRadius = 6f
                            )
                        ),
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                }
            }

            // ── EL CENTRO GEOMÉTRICO ABSOLUTO (X, Y) ──
            Text(
                text = "A.C.E WEAR",
                fontFamily = CinzelDecorative,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = SaturatedNeonRed.copy(alpha = neonAlpha),
                        offset = Offset(0f, 0f),
                        blurRadius = 15f
                    )
                ),
                textAlign = TextAlign.Center,
                letterSpacing = 2.5.sp
            )

            // MITAD INFERIOR: Controla la acción actual o alertas de permisos
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = operationalText.uppercase(),
                    fontFamily = CinzelDecorative,
                    fontWeight = FontWeight.Normal,
                    fontSize = 8.sp,
                    color = operationalColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 8.dp) // Separación limpia debajo del título
                )
            }
        }
    }
}