package com.ace.wear.presentation.session

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.AceLogo
import com.ace.wear.presentation.components.AceRingBackground
import com.ace.wear.presentation.theme.AceRed
import com.ace.wear.presentation.theme.CinzelDecorative
import com.ace.wear.presentation.theme.UnifrakturMaguntia

/**
 * Pantalla de lanzamiento con anillos animados.
 * Duracion: 2 segundos, luego auto-transiciona a IDLE.
 */
@Composable
fun SplashScreen() {
    // Animacion de pulso sutil para el logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Fondo: anillos con opacidad maxima
        AceRingBackground(animated = true)

        // Centro: Logo + texto
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo A.C.E con pulso sutil
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            ) {
                AceLogo(size = 80.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Texto A.C.E
            Text(
                text = "A.C.E",
                fontFamily = CinzelDecorative,
                fontSize = 20.sp,
                color = AceRed,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitulo
            Text(
                text = "ACTIVE CARDIAC EFFORT",
                fontFamily = UnifrakturMaguntia,
                fontSize = 8.sp,
                color = AceRed.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}
