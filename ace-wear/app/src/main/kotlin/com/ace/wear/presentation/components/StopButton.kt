// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/components/StopButton.kt

package com.ace.wear.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Boton DETENER para finalizar la sesion de ejercicio.
 */
@Composable
fun StopButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.primaryButtonColors(
            backgroundColor = MaterialTheme.colors.error
        )
    ) {
        Text(
            text = "DETENER",
            style = MaterialTheme.typography.button
        )
    }
}