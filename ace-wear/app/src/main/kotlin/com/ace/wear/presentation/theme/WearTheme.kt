// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/theme/WearTheme.kt

package com.ace.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/**
 * Tema de A.C.E para Wear OS.
 *
 * Usa MaterialTheme por defecto de Wear Compose.
 * En fases futuras se pueden personalizar colores, tipografia, etc.
 */
@Composable
fun WearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}