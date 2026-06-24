package com.ace.wear.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.theme.*

@Composable
fun ConnectionStatusChip(
    isConnected: Boolean,
    nodeCount: Int = 0,
    lastError: String? = null
) {
    val (indicatorColor, text) = when {
        isConnected && nodeCount > 0 -> Pair(AceGreen, "Conectado")
        !isConnected && lastError != null -> Pair(AceRed, "Error")
        else -> Pair(AceOrange, "Esperando...")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "●",
            style = MaterialTheme.typography.caption3,
            color = indicatorColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.caption3,
            color = AceTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}