package com.ace.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.theme.AceBorder
import com.ace.wear.presentation.theme.AceSurface
import com.ace.wear.presentation.theme.AceTextMuted

@Composable
fun DiagLogPanel(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(AceSurface, RoundedCornerShape(6.dp))
            .padding(4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        logs.takeLast(3).forEach { log ->
            Text(
                text = log,
                style = MaterialTheme.typography.caption3,
                color = AceTextMuted,
                textAlign = TextAlign.Start
            )
        }
    }
}