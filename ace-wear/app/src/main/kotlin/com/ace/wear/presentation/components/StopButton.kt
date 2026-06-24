package com.ace.wear.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.theme.AceRed
import com.ace.wear.presentation.theme.UnifrakturMaguntia

@Composable
fun StopButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.primaryButtonColors(
            backgroundColor = AceRed
        )
    ) {
        Text(
            text = "DETENER",
            fontFamily = UnifrakturMaguntia,
            style = MaterialTheme.typography.button
        )
    }
}