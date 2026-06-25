package com.ace.mobile.feature.auth.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlin.math.*

// ─── Datos compartidos del fondo ───────────────────────────────────────────────
internal data class AuthCube(
    val xPercent: Float, val yPercent: Float,
    val size: Float, val speedX: Float, val speedY: Float, val alpha: Float
)

internal data class AuthProjPoint(val offset: Offset, val depth: Float)

internal val CUBE_VERTICES = listOf(
    floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
    floatArrayOf(1f,  1f, -1f),  floatArrayOf(-1f, 1f, -1f),
    floatArrayOf(-1f, -1f,  1f), floatArrayOf(1f, -1f,  1f),
    floatArrayOf(1f,  1f,  1f),  floatArrayOf(-1f, 1f,  1f)
)

internal val CUBE_EDGES = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 0,
    4 to 5, 5 to 6, 6 to 7, 7 to 4,
    0 to 4, 1 to 5, 2 to 6, 3 to 7
)

internal val AUTH_CUBES = listOf(
    AuthCube(0.08f, 0.12f, 70f,  0.5f,  1.1f,  0.18f),
    AuthCube(0.90f, 0.08f, 95f,  -0.7f, 0.6f,  0.22f),
    AuthCube(0.85f, 0.45f, 65f,  1.1f,  -0.8f, 0.14f),
    AuthCube(0.05f, 0.60f, 85f,  -0.4f, 1.0f,  0.18f),
    AuthCube(0.92f, 0.82f, 110f, 0.8f,  0.5f,  0.22f),
    AuthCube(0.18f, 0.88f, 60f,  0.9f,  -0.9f, 0.14f),
    AuthCube(0.50f, 0.05f, 55f,  -0.6f, 0.7f,  0.12f),
)

internal fun projectVertex(
    vx: Float, vy: Float, vz: Float,
    size: Float, speedX: Float, speedY: Float,
    rotation: Float, cx: Float, cy: Float
): AuthProjPoint {
    val ax = rotation * speedX; val ay = rotation * speedY
    val cosX = cos(ax); val sinX = sin(ax)
    val cosY = cos(ay); val sinY = sin(ay)
    val half = size / 2f
    val x0 = vx * half; val y0 = vy * half; val z0 = vz * half
    val y1 = y0 * cosX - z0 * sinX; val z1 = y0 * sinX + z0 * cosX
    val x2 = x0 * cosY + z1 * sinY; val z2 = -x0 * sinY + z1 * cosY
    val dist = 300f; val p = dist / (dist + z2)
    return AuthProjPoint(Offset(cx + x2 * p, cy + y1 * p), z2)
}

// ─── Fondo animado compartido ──────────────────────────────────────────────────
@Composable
fun AuthBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "AuthBgLoop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "AuthBgRotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val step = 65f
        var gx = 0f
        while (gx < w) {
            drawLine(
                Color(0x08FF1744), Offset(gx, 0f), Offset(gx, h), strokeWidth = 0.5f
            ); gx += step
        }
        var gy = 0f
        while (gy < h) {
            drawLine(
                Color(0x08FF1744), Offset(0f, gy), Offset(w, gy), strokeWidth = 0.5f
            ); gy += step
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
                drawCircle(
                    Color(0xFFFF1744).copy(alpha = pAlpha),
                    1.6f * sc,
                    Offset(ptX, ptY)
                )
            }
        }
        AUTH_CUBES.forEach { cube ->
            val cx = w * cube.xPercent; val cy = h * cube.yPercent
            CUBE_EDGES.forEach { (a, b) ->
                val p1 = projectVertex(
                    CUBE_VERTICES[a][0], CUBE_VERTICES[a][1], CUBE_VERTICES[a][2],
                    cube.size, cube.speedX, cube.speedY, rotation, cx, cy
                )
                val p2 = projectVertex(
                    CUBE_VERTICES[b][0], CUBE_VERTICES[b][1], CUBE_VERTICES[b][2],
                    cube.size, cube.speedX, cube.speedY, rotation, cx, cy
                )
                val df = (((cube.size - (p1.depth + p2.depth) / 2f) / (2f * cube.size)))
                    .coerceIn(0.15f, 1f)
                drawLine(
                    Color(0xFFFF1744).copy(alpha = cube.alpha * df * 0.4f),
                    p1.offset, p2.offset,
                    5.dp.toPx() * df, StrokeCap.Round
                )
                drawLine(
                    lerp(Color(0xFFFF1744), Color.White, 0.25f)
                        .copy(alpha = cube.alpha * df * 1.5f),
                    p1.offset, p2.offset,
                    1.2.dp.toPx() * df, StrokeCap.Round
                )
            }
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF050505).copy(alpha = 0.7f)),
                center = Offset(w / 2f, h / 2f), radius = minOf(w, h) * 0.75f
            ),
            radius = minOf(w, h) * 0.75f, center = Offset(w / 2f, h / 2f)
        )
    }
}