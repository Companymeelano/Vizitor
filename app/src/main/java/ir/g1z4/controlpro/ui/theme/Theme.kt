package ir.g1z4.controlpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.g1z4.controlpro.R
import ir.g1z4.controlpro.domain.ThemeId

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

data class Palette(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val stroke: Color,
    val gold: Color,
    val ink: Color,
    val muted: Color,
    val ok: Color,
    val alarm: Color,
    val warn: Color,
    val info: Color,
    val unknown: Color,
    val glow: Color,
    val dark: Boolean
)

val LocalPalette = staticCompositionLocalOf {
    Palette(Color(0xFF070B12), Color(0xFF101826), Color(0xCC152033), Color(0x33E0C48A), Color(0xFFE0C48A), Color(0xFFF6F1E7), Color(0xFFC4BEB2), Color(0xFF3DDC97), Color(0xFFFF4D6A), Color(0xFFFFB020), Color(0xFF6AA4FF), Color(0xFF8E8A84), Color(0x66E0C48A), true)
}

fun paletteOf(id: ThemeId): Palette = when (id) {
    ThemeId.LUXURY_DARK -> Palette(Color(0xFF070B12), Color(0xFF101826), Color(0xE6141C2B), Color(0x33E0C48A), Color(0xFFE0C48A), Color(0xFFF6F1E7), Color(0xFFC8C2B6), Color(0xFF3DDC97), Color(0xFFFF4D6A), Color(0xFFFFB020), Color(0xFF6AA4FF), Color(0xFF9A958C), Color(0x55E0C48A), true)
    ThemeId.MIDNIGHT -> Palette(Color(0xFF04070F), Color(0xFF0A1224), Color(0xE60C1830), Color(0x443EE0FF), Color(0xFF7EE7FF), Color(0xFFE8F4FF), Color(0xFFB7C6D6), Color(0xFF3DDC97), Color(0xFFFF5D73), Color(0xFFFFB020), Color(0xFF3EE0FF), Color(0xFF8FA0B3), Color(0x443EE0FF), true)
    ThemeId.LUXURY_LIGHT -> Palette(Color(0xFFF6F1E8), Color(0xFFFFFCF7), Color(0xF2FFFFFF), Color(0x338C6A32), Color(0xFF8C6A32), Color(0xFF1C1916), Color(0xFF5E584F), Color(0xFF0E8F62), Color(0xFFC4233A), Color(0xFFB86A00), Color(0xFF1D4E89), Color(0xFF8A8378), Color(0x338C6A32), false)
    ThemeId.PURE_MINIMAL -> Palette(Color(0xFFF7F7F8), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE4E4E7), Color(0xFF1F4BFF), Color(0xFF111111), Color(0xFF4B4B4F), Color(0xFF0E8F62), Color(0xFFD21F3C), Color(0xFFC56A00), Color(0xFF1F4BFF), Color(0xFF8A8A8E), Color(0x141F4BFF), false)
}

private val type = Typography(
    headlineMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = Vazir, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun G1Theme(id: ThemeId, content: @Composable () -> Unit) {
    val palette = paletteOf(id)
    val scheme = if (palette.dark) {
        darkColorScheme(primary = palette.gold, onPrimary = Color(0xFF1A1408), background = palette.bg, surface = palette.bg2, onBackground = palette.ink, onSurface = palette.ink, error = palette.alarm)
    } else {
        lightColorScheme(primary = palette.gold, onPrimary = Color.White, background = palette.bg, surface = palette.bg2, onBackground = palette.ink, onSurface = palette.ink, error = palette.alarm)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
