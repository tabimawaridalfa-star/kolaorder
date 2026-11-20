package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KalaOrderLightColorScheme = lightColorScheme(
    primary = PrimaryEmerald,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryDeep,
    secondary = SecondarySlate,
    onSecondary = SurfaceWhite,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryBlue,
    onTertiary = SurfaceWhite,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = ErrorCrimson,
    onError = SurfaceWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BgCanvas,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = FillSubtle,
    onSurfaceVariant = TextSecondary,
    outline = BorderStrong,
    outlineVariant = BorderSubtle,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = BgCanvas,
    surfaceContainer = FillSubtle,
    surfaceContainerHigh = ColorTokens.SurfaceContainerHigh,
    surfaceContainerHighest = ColorTokens.SurfaceContainerHighest,
)

private object ColorTokens {
    val SurfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
    val SurfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
}

object KalaOrderShapes {
    val Small = RoundedCornerShape(6.dp)
    val Medium = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(14.dp)
    val Sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val Pill = RoundedCornerShape(9999.dp)
}

private val KalaOrderMaterialShapes = Shapes(
    extraSmall = KalaOrderShapes.Small,
    small = KalaOrderShapes.Small,
    medium = KalaOrderShapes.Medium,
    large = KalaOrderShapes.Card,
    extraLarge = KalaOrderShapes.Sheet,
)

@Composable
fun KalaOrderTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KalaOrderLightColorScheme,
        typography = KalaOrderTypography,
        shapes = KalaOrderMaterialShapes,
        content = content,
    )
}
