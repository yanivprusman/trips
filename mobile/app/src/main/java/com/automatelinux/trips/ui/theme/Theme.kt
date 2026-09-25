package com.automatelinux.trips.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.trips.R

@OptIn(ExperimentalTextApi::class)
private fun heebo(weight: Int, fw: FontWeight) =
    Font(R.font.heebo, fw, variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
val Heebo = FontFamily(
    heebo(400, FontWeight.Normal),
    heebo(500, FontWeight.Medium),
    heebo(600, FontWeight.SemiBold),
    heebo(700, FontWeight.Bold),
    heebo(800, FontWeight.ExtraBold),
)

private val TripsTypography = Typography(
    displayLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val scheme = lightColorScheme(
    primary = Tp.Terracotta,
    onPrimary = Color.White,
    primaryContainer = Tp.TerracottaSoft,
    onPrimaryContainer = Tp.Ink,
    secondary = Tp.Sky,
    onSecondary = Color.White,
    background = Tp.Sand,
    onBackground = Tp.Ink,
    surface = Tp.Paper,
    onSurface = Tp.Ink,
    surfaceVariant = Tp.SandDeep,
    onSurfaceVariant = Tp.Muted,
    outline = Tp.Line,
    error = Tp.TrailRed,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = TripsTypography, shapes = shapes, content = content)
}
