// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/components/TimerDisplay.kt

package com.ace.wear.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Muestra el tiempo transcurrido de la sesion en formato mm:ss.
 */
@Composable
fun TimerDisplay(
    elapsedSeconds: Long
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val displayText = String.format("%02d:%02d", minutes, seconds)

    Text(
        text = displayText,
        style = MaterialTheme.typography.title3,
        textAlign = TextAlign.Center
    )
}