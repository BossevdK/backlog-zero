package com.backlogzero.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.backlogzero.app.domain.ItemStatus

val Accent = Color(0xFF8B9BFF)
val AccentTeal = Color(0xFF5ED6C8)
val Gold = Color(0xFFFFC96B)

fun ItemStatus.color(): Color = when (this) {
    ItemStatus.BACKLOG -> Color(0xFF9AA5FF)
    ItemStatus.IN_PROGRESS -> Color(0xFF5ED6C8)
    ItemStatus.PAUSED -> Color(0xFFFFC96B)
    ItemStatus.COMPLETED -> Color(0xFF7BE08F)
}

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF10123A),
    primaryContainer = Color(0xFF2A2F66),
    onPrimaryContainer = Color(0xFFDDE0FF),
    secondary = AccentTeal,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF173B38),
    onSecondaryContainer = Color(0xFFBDF5EE),
    background = Color(0xFF0D0E12),
    onBackground = Color(0xFFE8E9EF),
    surface = Color(0xFF14161C),
    onSurface = Color(0xFFE8E9EF),
    surfaceVariant = Color(0xFF1E212A),
    onSurfaceVariant = Color(0xFFA6A9B8),
    surfaceContainerLowest = Color(0xFF0A0B0E),
    surfaceContainerLow = Color(0xFF111318),
    surfaceContainer = Color(0xFF171A21),
    surfaceContainerHigh = Color(0xFF1E212A),
    surfaceContainerHighest = Color(0xFF262A35),
    outline = Color(0xFF4A4E5C),
    outlineVariant = Color(0xFF2A2D38),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF3A0808),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** De app is bewust altijd donker. */
@Composable
fun BacklogZeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = AppTypography, shapes = AppShapes, content = content)
}
