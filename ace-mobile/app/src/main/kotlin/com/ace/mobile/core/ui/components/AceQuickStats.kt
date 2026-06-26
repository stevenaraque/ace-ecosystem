// Location: com/ace/mobile/core/ui/components/AceQuickStats.kt
package com.ace.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography

@Composable
fun AceQuickStats(
    totalXp: String,
    globalPosition: String,
    totalSessions: String,
    modifier: Modifier = Modifier
) {
    AceCard(
        modifier = modifier.fillMaxWidth(),
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = totalXp, label = "XP TOTAL", modifier = Modifier.weight(1f))
            VerticalDividerDim()
            StatItem(value = globalPosition, label = "POSICIÓN", modifier = Modifier.weight(1f))
            VerticalDividerDim()
            StatItem(value = totalSessions, label = "SESIONES", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = AceTypography.H3.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AceColors.TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = AceTypography.Micro.copy(
                color = AceColors.NeonRed.copy(alpha = 0.75f)
            )
        )
    }
}

@Composable
private fun VerticalDividerDim() {
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(AceColors.BorderDim)
    )
}