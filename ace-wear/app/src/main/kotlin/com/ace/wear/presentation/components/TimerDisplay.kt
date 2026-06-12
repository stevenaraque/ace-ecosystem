// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/components/TimerDisplay.kt

package com.ace.wear.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Muestra el tiempo transcurrido de la sesion en formato MM:SS.
 */
@Composable
fun TimerDisplay(
    elapsedSeconds: Long
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formatted = String.format("%02d:%02d", minutes, seconds)

    Text(
        text = formatted,
        style = MaterialTheme.typography.title2,
        textAlign = TextAlign.Center
    )
}