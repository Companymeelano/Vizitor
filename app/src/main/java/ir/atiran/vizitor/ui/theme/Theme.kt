/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تم تاریک سازمانی (Dark Slate Theme)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PremiumDarkColors = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = NeonPurpleDark,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    tertiary = Gold,
    background = DarkSlate,
    onBackground = TextPrimary,
    surface = DarkSlateElevated,
    onSurface = TextPrimary,
    surfaceVariant = DarkSlateDeep,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White
)

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

@Composable
fun VizitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // اپ همیشه تاریک است؛ صرفاً برای سازگاری
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColors,
        typography = PremiumTypography,
        content = content
    )
}
