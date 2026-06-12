package com.ace.mobile.presentation.auth

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
import sena.adso.ace_mobile.R
import kotlin.math.*

// ─── Colores ──────────────────────────────────────────────────────────────────
private val NeonRed   = Color(0xFFFF1744)
private val BgBlack   = Color(0xFF050505)
private val GridColor = Color(0x08FF1744)
private val CardBg    = Color(0xFF0D0D0D)
private val BorderDim = Color(0xFF2A2A2A)

// ─── Fuentes ──────────────────────────────────────────────────────────────────
private val UnifrakturMaguntia = FontFamily(
    Font(resId = R.font.unifrakturmaguntia_regular, weight = FontWeight.Normal)
)
private val CinzelDecorative = FontFamily(
    Font(resId = R.font.cinzeldecorative_regular, weight = FontWeight.Normal)
)

// ─── Cubos 3D ─────────────────────────────────────────────────────────────────
private data class LoginCube(
    val xPercent: Float, val yPercent: Float,
    val size: Float, val speedX: Float, val speedY: Float, val alpha: Float
)
private data class ProjPoint(val offset: Offset, val depth: Float)

private val CUBE_VERTICES = listOf(
    floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
    floatArrayOf(1f,  1f, -1f),  floatArrayOf(-1f, 1f, -1f),
    floatArrayOf(-1f, -1f,  1f), floatArrayOf(1f, -1f,  1f),
    floatArrayOf(1f,  1f,  1f),  floatArrayOf(-1f, 1f,  1f)
)
private val CUBE_EDGES = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 0,
    4 to 5, 5 to 6, 6 to 7, 7 to 4,
    0 to 4, 1 to 5, 2 to 6, 3 to 7
)
private val LOGIN_CUBES = listOf(
    LoginCube(0.08f, 0.12f, 70f,  0.5f,  1.1f,  0.18f),
    LoginCube(0.90f, 0.08f, 95f,  -0.7f, 0.6f,  0.22f),
    LoginCube(0.85f, 0.45f, 65f,  1.1f,  -0.8f, 0.14f),
    LoginCube(0.05f, 0.60f, 85f,  -0.4f, 1.0f,  0.18f),
    LoginCube(0.92f, 0.82f, 110f, 0.8f,  0.5f,  0.22f),
    LoginCube(0.18f, 0.88f, 60f,  0.9f,  -0.9f, 0.14f),
    LoginCube(0.50f, 0.05f, 55f,  -0.6f, 0.7f,  0.12f),
)

private fun projectVertex(
    vx: Float, vy: Float, vz: Float,
    size: Float, speedX: Float, speedY: Float,
    rotation: Float, cx: Float, cy: Float
): ProjPoint {
    val ax = rotation * speedX; val ay = rotation * speedY
    val cosX = cos(ax); val sinX = sin(ax)
    val cosY = cos(ay); val sinY = sin(ay)
    val half = size / 2f
    val x0 = vx * half; val y0 = vy * half; val z0 = vz * half
    val y1 = y0 * cosX - z0 * sinX; val z1 = y0 * sinX + z0 * cosX
    val x2 = x0 * cosY + z1 * sinY; val z2 = -x0 * sinY + z1 * cosY
    val dist = 300f; val p = dist / (dist + z2)
    return ProjPoint(Offset(cx + x2 * p, cy + y1 * p), z2)
}

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
    onLoginSuccess: () -> Unit
) {
    val uiState = viewModel.uiState

    // Navegar al éxito cuando corresponda
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "LoginLoop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "LoginRotation"
    )
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
        // ── Fondo animado ──────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val step = 65f
            var gx = 0f
            while (gx < w) {
                drawLine(GridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 0.5f); gx += step
            }
            var gy = 0f
            while (gy < h) {
                drawLine(GridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 0.5f); gy += step
            }
            for (i in 0 until 40) {
                val seedX = sin(i * 38.2f); val seedY = cos(i * 71.4f)
                val progressZ = (i.toFloat() / 40 + (rotation / (2 * Math.PI).toFloat())) % 1f
                val pz = 400f * (1f - progressZ) - 80f
                if (pz < -150f) continue
                val dist = 300f; val sc = dist / (dist + pz)
                val ptX = w / 2f + (seedX * w * 0.6f) * sc
                val ptY = h / 2f + (seedY * h * 0.6f) * sc
                if (ptX in 0f..w && ptY in 0f..h) {
                    val pAlpha = (1f - (pz + 80f) / 400f).coerceIn(0.04f, 0.35f)
                    drawCircle(NeonRed.copy(alpha = pAlpha), 1.6f * sc, Offset(ptX, ptY))
                }
            }
            LOGIN_CUBES.forEach { cube ->
                val cx = w * cube.xPercent; val cy = h * cube.yPercent
                CUBE_EDGES.forEach { (a, b) ->
                    val p1 = projectVertex(CUBE_VERTICES[a][0], CUBE_VERTICES[a][1], CUBE_VERTICES[a][2], cube.size, cube.speedX, cube.speedY, rotation, cx, cy)
                    val p2 = projectVertex(CUBE_VERTICES[b][0], CUBE_VERTICES[b][1], CUBE_VERTICES[b][2], cube.size, cube.speedX, cube.speedY, rotation, cx, cy)
                    val df = (((cube.size - (p1.depth + p2.depth) / 2f) / (2f * cube.size))).coerceIn(0.15f, 1f)
                    drawLine(NeonRed.copy(alpha = cube.alpha * df * 0.4f),  p1.offset, p2.offset, 5.dp.toPx() * df, StrokeCap.Round)
                    drawLine(lerp(NeonRed, Color.White, 0.25f).copy(alpha = cube.alpha * df * 1.5f), p1.offset, p2.offset, 1.2.dp.toPx() * df, StrokeCap.Round)
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, BgBlack.copy(alpha = 0.7f)),
                    center = Offset(w / 2f, h / 2f), radius = minOf(w, h) * 0.75f
                ),
                radius = minOf(w, h) * 0.75f, center = Offset(w / 2f, h / 2f)
            )
        }

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

                    // Email — conectado al ViewModel
                    AceTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Correo electrónico"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password — conectado al ViewModel
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

                    // Botón login — conectado al ViewModel
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

                    // Error del ViewModel
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

                    OutlinedButton(
                        onClick = { },
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