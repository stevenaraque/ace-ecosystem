package com.ace.wear.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.theme.AceTextSecondary

@Composable
fun TimerDisplay(elapsedSeconds: Long) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val displayText = String.format("%02d:%02d", minutes, seconds)

    Text(
        text = displayText,
        style = MaterialTheme.typography.title2.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = AceTextSecondary
        ),
        textAlign = TextAlign.Center
    )
}