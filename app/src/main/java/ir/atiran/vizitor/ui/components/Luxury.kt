/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | اجزای لاکچری (Luxury UI Kit)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  حلقه طلایی گرادیانی، جاروب نور (Shine Sweep)، مقیاس فشردنی و سطح براق.
 *  تمامی جلوه‌ها با drawWithContent لایه‌ای و بدون blur/shadow سنگین پیاده‌
 *  شده‌اند تا روی گوشی‌های میان‌رده هم کاملاً روان اجرا شوند (ضدهنگ).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.GoldDark

/**
 * حلقه طلایی گرادیانی دور سطوح — امضای بصری نسخه لاکچری.
 * (به‌جای shadow سنگین، فقط یک stroke گرادیانی؛ هزینه GPU ناچیز)
 */
fun Modifier.goldRing(
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    width: Dp = 1.6.dp
): Modifier = this.border(
    width,
    Brush.linearGradient(
        colors = listOf(GoldDark, Gold, Color(0xFFFFF6CC), Gold, GoldDark),
    ),
    shape
)

/**
 * مقیاس نرم هنگام لمس — حس دکمه فیزیکی لوکس.
 */
@Composable
fun Modifier.pressScale(target: Float = 0.96f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) target else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            while (true) {
                // بدون consume تا clickable دکمه همچنان کار کند
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * جاروب نور لاکچری: یک باریکه نور مورب که هر چند ثانیه یک‌بار از روی
 * سطح عبور می‌کند. فقط یک لایه draw سبک — بدون recomposition سنگین.
 */
@Composable
fun Modifier.shineSweep(
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    periodMs: Int = 3400,
    highlightAlpha: Float = 0.22f
): Modifier {
    val transition = rememberInfiniteTransition(label = "shine")
    val progress by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineProgress"
    )
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val bandW = w * 0.45f
        val x = progress * (w + bandW * 2) - bandW
        drawRoundRectInto(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = highlightAlpha), Color.Transparent),
                start = Offset(x - bandW / 2, 0f),
                end = Offset(x + bandW / 2, h)
            )
        )
    }.clip(shape)
}

/** رسم سطح روی شکل گرد بدون تخصیص اضافی. */
private fun DrawScope.drawRoundRectInto(brush: Brush) {
    drawRect(brush = brush, size = Size(size.width, size.height))
}

/**
 * لایه براق شیشه‌ای بالای سطح (رفلکس نور استودیویی) — یک گرادیان ثابت و سبک.
 */
fun Modifier.topGloss(shape: RoundedCornerShape = RoundedCornerShape(20.dp)): Modifier =
    this.background(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f), Color.Transparent),
            startY = 0f,
            endY = 260f
        ),
        shape = shape
    )

/** محتوای دکمه لاکچری: Box مرکزی با حلقه طلایی + براق + جاروب نور. */
@Composable
fun LuxurySurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    fillBrush: Brush,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (enabled) Modifier.shineSweep(shape) else Modifier)
            .background(fillBrush, shape)
            .topGloss(shape)
            .goldRing(shape, if (enabled) 1.6.dp else 1.dp),
        content = content
    )
}
