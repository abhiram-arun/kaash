package dev.expensetracker.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Binding palette (PRODUCT.md): dark slate/navy surfaces, mint primary, teal secondary. Dark-only by decision.
private val Kaash = darkColorScheme(
    primary = Color(0xFFA6EF9C), onPrimary = Color(0xFF0F3318),
    primaryContainer = Color(0xFF2C4A37), onPrimaryContainer = Color(0xFFC6F8BF),
    secondary = Color(0xFF1DE2C0), onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0E4A44), onSecondaryContainer = Color(0xFFA6FFEC),
    tertiary = Color(0xFF8CC8FF), onTertiary = Color(0xFF00325A),
    tertiaryContainer = Color(0xFF15324F), onTertiaryContainer = Color(0xFFD3E8FF),
    background = Color(0xFF1C1F26), onBackground = Color(0xFFE6EBF2),
    surface = Color(0xFF1C1F26), onSurface = Color(0xFFE6EBF2),
    surfaceVariant = Color(0xFF2C3E55), onSurfaceVariant = Color(0xFFAAB7C7),
    surfaceContainerLowest = Color(0xFF16191F), surfaceContainerLow = Color(0xFF232A35),
    surfaceContainer = Color(0xFF2B3C52), surfaceContainerHigh = Color(0xFF34485F),
    surfaceContainerHighest = Color(0xFF3E546D),
    outline = Color(0xFF5F7189), outlineVariant = Color(0xFF3A4B61),
    error = Color(0xFFFF8A80), onError = Color(0xFF4A0E0B),
    errorContainer = Color(0xFF5C1F1C), onErrorContainer = Color(0xFFFFDAD6),
)

private val base = Typography()
private val KaashType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val KaashShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KaashTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Kaash, typography = KaashType, shapes = KaashShapes, content = content)

/** Deep navy for the summary cards at the top of a screen (the reference's app-bar navy). */
val HeroSurface = Color(0xFF15324F)

@Suppress("UnusedReceiverParameter") val ColorScheme.positive get() = Color(0xFFA6EF9C)
@Suppress("UnusedReceiverParameter") val ColorScheme.warning get() = Color(0xFFFFC56B)

/** Category colour lifted toward white so it holds contrast on the slate surfaces. */
@Suppress("UnusedReceiverParameter")
fun ColorScheme.accent(argb: Long): Color = lerp(Color(argb), Color.White, 0.38f)
