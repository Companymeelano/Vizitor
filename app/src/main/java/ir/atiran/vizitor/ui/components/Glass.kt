/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | معماری Glassmorphism
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کارت‌ها و پنل‌های شیشه‌ای مات با حاشیه‌های محو + درخشش نئونی
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.ui.theme.GlassBorder
import ir.atiran.vizitor.ui.theme.GlassFill
import ir.atiran.vizitor.ui.theme.GlassHighlight

/**
 * پس‌زمینه شیشه‌ای مات: لایه نیمه‌شفاف + گرادیان ملایم از بالا + حاشیه محو.
 */
fun Modifier.glassPanel(
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    fill: Color = GlassFill
): Modifier = this
    .clip(shape)
    .background(fill, shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(GlassHighlight, Color.Transparent),
            startY = 0f,
            endY = 220f
        ),
        shape = shape
    )
    .border(borderWidth, borderColor, shape)

/** کارت شیشه‌ای آماده استفاده با پدینگ داخلی استاندارد. */
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

/** درخشش نئونی دور کارت (برای موارد مهم مثل پنل پورسانت). */
fun Modifier.neonGlow(color: Color, radius: Dp = 18.dp): Modifier = this
    .shadow(radius, RoundedCornerShape(22.dp), ambientColor = color, spotColor = color)

/** حاشیه طلایی مخصوص مشتریان/کالاهای VIP. */
fun Modifier.goldBorder(shape: RoundedCornerShape = RoundedCornerShape(22.dp)): Modifier =
    this.border(1.5.dp, Color(0xB3FFD166), shape)

/** پس‌زمینه صفحه با گرادیان اسلیت و هاله بنفش. */
fun Modifier.dashboardBackdrop(): Modifier = this.drawBehind {
    drawRect(Color(0xFF0B0E13))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x2EB04BF8), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.08f),
            radius = size.width * 0.9f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1F2BFF88), Color.Transparent),
            center = Offset(size.width * 0.1f, size.height * 0.95f),
            radius = size.width * 0.8f
        )
    )
    drawRoundRect(
        color = Color(0x0DFFFFFF),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius.Zero
    )
}
