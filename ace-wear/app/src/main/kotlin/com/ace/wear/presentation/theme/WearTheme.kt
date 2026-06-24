// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/theme/WearTheme.kt
package com.ace.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val AceWearColors = Colors(
    primary = AceRed,
    primaryVariant = AceRedDark,
    secondary = AceRedGlow,
    background = AceBlack,
    surface = AceSurface,
    error = AceRed,
    onPrimary = AceBlack,
    onSecondary = AceTextPrimary,
    onBackground = AceTextPrimary,
    onSurface = AceTextPrimary,
    onError = AceTextPrimary
)

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = AceWearColors, content = content)
}