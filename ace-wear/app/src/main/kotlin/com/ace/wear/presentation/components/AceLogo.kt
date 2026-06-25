package com.ace.wear.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ace.wear.presentation.theme.AceRed

@Composable
fun AceLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    logoColor: Color = AceRed
) {
    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()

        // FACTOR DE ESCALA DINÁMICO:
        // Mantiene la proporción perfecta del grosor de las líneas sin importar el tamaño del logo
        val scale = width / 80.dp.toPx()

        val strokePulse = 6.dp.toPx() * scale
        val strokeBar = 4.dp.toPx() * scale

        // 1. Pulso EKG que forma la "A" (Línea blanca principal)
        val path = Path().apply {
            moveTo(width * 0.20f, height * 0.55f)
            lineTo(width * 0.32f, height * 0.55f)
            lineTo(width * 0.38f, height * 0.65f)
            lineTo(width * 0.50f, height * 0.25f)
            lineTo(width * 0.62f, height * 0.75f)
            lineTo(width * 0.68f, height * 0.55f)
            lineTo(width * 0.80f, height * 0.55f)
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(
                width = strokePulse,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 2. Barra horizontal de la "A" (Línea de color de la marca)
        val barPath = Path().apply {
            moveTo(width * 0.43f, height * 0.50f)
            lineTo(width * 0.57f, height * 0.50f)
        }

        drawPath(
            path = barPath,
            color = logoColor,
            style = Stroke(
                width = strokeBar,
                cap = StrokeCap.Round
            )
        )
    }
}