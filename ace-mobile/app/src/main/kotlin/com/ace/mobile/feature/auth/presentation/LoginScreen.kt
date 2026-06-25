package com.ace.mobile.feature.auth.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sena.adso.ace_mobile.R

// ─── Colores ──────────────────────────────────────────────────────────────────
private val NeonRed   = Color(0xFFFF1744)
private val BgBlack   = Color(0xFF050505)
private val CardBg    = Color(0xFF0D0D0D)
private val BorderDim = Color(0xFF2A2A2A)

// ─── Fuentes ──────────────────────────────────────────────────────────────────
private val UnifrakturMaguntia = FontFamily(
    Font(resId = R.font.unifrakturmaguntia_regular, weight = FontWeight.Normal)
)
private val CinzelDecorative = FontFamily(
    Font(resId = R.font.cinzeldecorative_regular, weight = FontWeight.Normal)
)

// ─── Logo ACE ─────────────────────────────────────────────────────────────────
@Composable
private fun AceLogoSmall(sizeDp: Float = 72f, pulseScale: Float = 1f) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val w = size.width; val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonRed.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w / 2f, h / 2f), radius = w * 0.7f
            ),
            radius = w * 0.7f, center = Offset(w / 2f, h / 2f)
        )
        drawCircle(
            color = NeonRed.copy(alpha = 0.15f), radius = w / 2f - 1.dp.toPx(),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = NeonRed, radius = w / 2f - 2.dp.toPx(),
            style = Stroke(width = 2.5.dp.toPx())
        )
        val sw = 4.5.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.55f)
            lineTo(w * 0.31f, h * 0.55f)
            lineTo(w * 0.37f, h * 0.65f)
            lineTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.63f, h * 0.76f)
            lineTo(w * 0.69f, h * 0.55f)
            lineTo(w * 0.82f, h * 0.55f)
        }
        drawPath(path, NeonRed.copy(alpha = 0.20f), style = Stroke(sw * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color.White, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val bar = Path().apply {
            moveTo(w * 0.42f, h * 0.50f); lineTo(w * 0.58f, h * 0.50f)
        }
        drawPath(bar, NeonRed, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ─── TextField estilizado ─────────────────────────────────────────────────────
@Composable
private fun AceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = NeonRed,
            unfocusedBorderColor    = BorderDim,
            focusedLabelColor       = NeonRed,
            unfocusedLabelColor     = Color(0xFF555555),
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color(0xFFCCCCCC),
            cursorColor             = NeonRed,
            focusedContainerColor   = Color(0xFF100808),
            unfocusedContainerColor = Color(0xFF0A0A0A),
        )
    )
}

// ─── LoginScreen ──────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navegación via eventos (patrón unificado con ProfileScreen)
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                LoginEvent.NavigateToHome -> onLoginSuccess()
            }
        }
    }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "LoginLoop")
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation  = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "LogoPulse"
    )
    val lineGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "LineGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack),
        contentAlignment = Alignment.Center
    ) {
        // ── Fondo animado compartido ─────────────────────────────────────
        AuthBackground()

        // ── Contenido ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.graphicsLayer { scaleX = logoPulse; scaleY = logoPulse },
                contentAlignment = Alignment.Center
            ) {
                AceLogoSmall(sizeDp = 80f, pulseScale = logoPulse)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "A.C.E",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp,
                fontFamily = CinzelDecorative
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(modifier = Modifier.width(60.dp).height(2.dp)) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, NeonRed.copy(alpha = lineGlow), Color.Transparent)
                    ),
                    start = Offset(0f, 1.dp.toPx()),
                    end   = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ACTIVE CARDIAC EFFORT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NeonRed.copy(alpha = 0.75f),
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Tarjeta ────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(NeonRed.copy(alpha = 0.35f), Color.Transparent)
                    )
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        text = "Iniciar sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp),
                        fontFamily = UnifrakturMaguntia
                    )

                    // Email
                    AceTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Correo electrónico"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password
                    AceTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Contraseña",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { }) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                fontSize = 12.sp,
                                color = NeonRed.copy(alpha = 0.80f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón login
                    Button(
                        onClick = viewModel::login,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Ingresar",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                fontFamily = UnifrakturMaguntia
                            )
                        }
                    }

                    // Error
                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error,
                            color = NeonRed,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDim, thickness = 0.5.dp)
                        Text(text = "  ó  ", fontSize = 11.sp, color = Color(0xFF444444))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDim, thickness = 0.5.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ← CABLEADO: Botón CREAR CUENTA
                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.50f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
                    ) {
                        Text(
                            text = "CREAR CUENTA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = NeonRed,
                            fontFamily = CinzelDecorative
                        )
                    }
                }
            }
        }
    }
}