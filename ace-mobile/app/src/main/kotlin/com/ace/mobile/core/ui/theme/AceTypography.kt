// Location: com/ace/mobile/core/ui/theme/AceTypography.kt
package com.ace.mobile.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import sena.adso.ace_mobile.R

object AceTypography {
    private val UnifrakturMaguntia = FontFamily(
        Font(resId = R.font.unifrakturmaguntia_regular, weight = FontWeight.Normal)
    )
    private val CinzelDecorative = FontFamily(
        Font(resId = R.font.cinzeldecorative_regular, weight = FontWeight.Normal)
    )
    private val SystemFamily = FontFamily.Default

    val DisplayBrand = TextStyle(
        fontFamily = CinzelDecorative,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = 2.sp,
        color = AceColors.TextPrimary
    )

    val H1 = TextStyle(
        fontFamily = UnifrakturMaguntia,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        color = AceColors.TextPrimary
    )

    val H2 = TextStyle(
        fontFamily = CinzelDecorative,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.sp,
        color = AceColors.TextPrimary
    )

    val H3 = TextStyle(
        fontFamily = SystemFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = AceColors.TextPrimary
    )

    val Body = TextStyle(
        fontFamily = SystemFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = AceColors.TextSecondary
    )

    val Caption = TextStyle(
        fontFamily = SystemFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = AceColors.TextMuted
    )

    val Micro = TextStyle(
        fontFamily = SystemFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = AceColors.TextMuted
    )
}