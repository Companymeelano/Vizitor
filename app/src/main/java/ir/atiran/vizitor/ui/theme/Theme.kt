/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | موتور تم چندگانه (نسخه ۱٫۸٫۰)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  پنج تم لاکچری (۳ تیره + ۲ روشن) — پالت از Themes.kt خوانده می‌شود و
 *  هم به MaterialTheme و هم به LocalVizitorPalette تزریق می‌گردد.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PremiumTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

/**
 * ترکیب‌بندی تم بر اساس شناسه انتخابی کاربر.
 * colorScheme متریال و پالت سفارشی هر دو از یک منبع واحد تغذیه می‌شوند تا
 * همه بخش‌ها (متن، دکمه، کارت، نمودار) کاملاً همرنگ باشند.
 */
@Composable
fun VizitorTheme(
    themeId: String = RoyalDarkPalette.id,
    content: @Composable () -> Unit
) {
    val palette = paletteById(themeId)

    val darkOnTertiary = Color(0xFF2A1E04)
    val scheme = if (palette.isDark) darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryDark,
        secondary = palette.accent,
        onSecondary = palette.onAccent,
        tertiary = palette.gold,
        onTertiary = darkOnTertiary,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceDeep,
        onSurfaceVariant = palette.textSecondary,
        error = palette.danger,
        onError = Color.White
    ) else lightColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryDark,
        secondary = palette.accent,
        onSecondary = palette.onAccent,
        tertiary = palette.gold,
        onTertiary = Color.White,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceDeep,
        onSurfaceVariant = palette.textSecondary,
        error = palette.danger,
        onError = Color.White
    )

    CompositionLocalProvider(LocalVizitorPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PremiumTypography,
            content = content
        )
    }
}
