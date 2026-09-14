/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | معماری کارت‌های لاکچری (تم‌پذیر)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کارت‌ها و پنل‌ها با سطح جامد + هاله‌های نورِ پالت فعال + خط نور استودیویی.
 *  تمام رنگ‌ها از LocalVizitorPalette خوانده می‌شوند؛ با تعویض تم (تیره
 *  لاکچری ⇄ روشن لاکچری) ظاهر کارت‌ها خودکار هماهنگ می‌ماند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.ui.theme.DarkSlate
import ir.atiran.vizitor.ui.theme.DarkSlateElevated
import ir.atiran.vizitor.ui.theme.GlassBorder
import ir.atiran.vizitor.ui.theme.GlassFill
import ir.atiran.vizitor.ui.theme.GlassHighlight
import ir.atiran.vizitor.ui.theme.vizitorPalette

/**
 * پس‌زمینه کارت لاکچری: سطح جامدِ تم + هاله رنگی گوشه بالا + هاله ثانویه
 * گوشه پایین + خط نور استودیویی بالا + حاشیه شیشه‌ایِ پالت.
 */
@Composable
fun Modifier.glassPanel(
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp
): Modifier {
    val surface = DarkSlateElevated
    val halo1 = vizitorPalette.halo1
    val halo2 = vizitorPalette.halo2
    val streak = GlassHighlight
    return this
        .clip(shape)
        .background(surface, shape)
        .drawBehind {
            // هاله اصلی (گوشه بالا)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(halo1, Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.02f),
                    radius = size.width * 0.95f
                )
            )
            // هاله ثانویه (گوشه پایین)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(halo2, Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 1.05f),
                    radius = size.width * 0.85f
                )
            )
            // خط نور استودیویی بالای سطح
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(streak, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.35f
                )
            )
        }
        .border(borderWidth, borderColor, shape)
}

/** کارت لاکچری آماده استفاده با پدینگ داخلی استاندارد. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderColor: Color = GlassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassPanel(shape = shape, borderColor = borderColor)
            .padding(16.dp),
        content = content
    )
}

/** درخشش نئونی دور کارت (برای موارد مهم). */
fun Modifier.neonGlow(color: Color, radius: Dp = 18.dp): Modifier = this
    .shadow(radius, RoundedCornerShape(22.dp), ambientColor = color, spotColor = color)

/** حاشیه طلایی مخصوص مشتریان/کالاهای VIP. */
fun Modifier.goldBorder(shape: RoundedCornerShape = RoundedCornerShape(22.dp)): Modifier =
    this.border(1.5.dp, Color(0xB3FFD166), shape)

/** پس‌زمینه تمام صفحه با گرادیان پس‌زمینه تم + هاله‌های نور ملایم. */
@Composable
fun Modifier.dashboardBackdrop(): Modifier {
    val bg = DarkSlate
    val halo1 = vizitorPalette.halo1
    val halo2 = vizitorPalette.halo2
    val dust = GlassFill
    return drawBehind {
        drawRect(bg)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(halo1, Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.08f),
                radius = size.width * 0.9f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(halo2, Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.95f),
                radius = size.width * 0.8f
            )
        )
        drawRoundRect(
            color = dust,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius.Zero
        )
    }
}
