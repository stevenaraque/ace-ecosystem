// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/components/HeartRateDisplay.kt

package com.ace.wear.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Muestra la frecuencia cardiaca en vivo.
 *
 * - Si hay valor: muestra BPM grande
 * - Si no hay valor: muestra "--"
 */
@Composable
fun HeartRateDisplay(
    bpm: Double?
) {
    val displayText = bpm?.let { String.format("%.0f", it) } ?: "--"
    val unitText = if (bpm != null) "BPM" else ""

    Text(
        text = displayText,
        style = MaterialTheme.typography.display1,
        textAlign = TextAlign.Center
    )

    if (unitText.isNotEmpty()) {
        Text(
            text = unitText,
            style = MaterialTheme.typography.caption2,
            textAlign = TextAlign.Center
        )
    }
}