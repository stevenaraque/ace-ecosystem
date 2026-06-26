// Location: com/ace/mobile/core/ui/components/AceButton.kt
package com.ace.mobile.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography

@Composable
fun AceButtonFilled(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(52.dp),
    isLoading: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = AceTypography.H1.copy(
        fontSize = 14.sp,
        letterSpacing = 3.sp,
        color = Color.White
    )
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AceColors.NeonRed,
            disabledContainerColor = AceColors.NeonRed.copy(alpha = 0.4f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                style = textStyle
            )
        }
    }
}

@Composable
fun AceButtonOutlined(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(50.dp),
    enabled: Boolean = true,
    textStyle: TextStyle = AceTypography.H2.copy(
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        color = AceColors.NeonRed
    )
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AceColors.NeonRed.copy(alpha = 0.50f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AceColors.NeonRed)
    ) {
        Text(
            text = text,
            style = textStyle
        )
    }
}

@Composable
fun AceButtonText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = AceTypography.Body.copy(
                fontSize = 12.sp,
                color = AceColors.NeonRed.copy(alpha = 0.80f)
            )
        )
    }
}