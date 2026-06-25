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
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.AceLogo
import com.ace.wear.presentation.components.AceRingBackground
import com.ace.wear.presentation.theme.AceRed
import com.ace.wear.presentation.theme.CinzelDecorative

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neonPulse"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AceRingBackground(
            modifier = Modifier.fillMaxSize(),
            animated = false,
            showCenterDot = false
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "A.C.E WEAR",
                fontFamily = CinzelDecorative,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = AceRed.copy(alpha = neonAlpha),
                        offset = Offset(0f, 0f),
                        blurRadius = 12f
                    )
                ),
                textAlign = TextAlign.Center,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ACTIVE CARDIAC EFFORT",
                fontFamily = CinzelDecorative,
                fontWeight = FontWeight.Normal,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // AJUSTE: Subimos de 28.dp a 36.dp para mejorar la definición del icono
            AceLogo(size = 36.dp)
        }
    }
}