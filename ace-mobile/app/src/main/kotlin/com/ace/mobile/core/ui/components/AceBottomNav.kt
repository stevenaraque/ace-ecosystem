// Location: com/ace/mobile/core/ui/components/AceBottomNav.kt
package com.ace.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ace.mobile.core.ui.theme.AceColors

enum class AceTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    EXERCISE("Ejercicio", Icons.Default.PlayArrow),
    RANKING("Ranking", Icons.Default.ThumbUp),
    STATS("Estadísticas", Icons.Default.ArrowDropDown),
    PROFILE("Perfil", Icons.Default.Person)
}

@Composable
fun AceBottomNav(
    selectedTab: AceTab,
    onTabSelected: (AceTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 0.5.dp, color = AceColors.BorderDim)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(AceColors.CardBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AceTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val color = if (isSelected) AceColors.NeonRed else AceColors.TextMuted

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}