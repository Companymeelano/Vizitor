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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.awaitEachGesture
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
            awaitEachGesture {
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

/**
 * عنوان طلایی با درخشش متحرک آرام (Shimmer) — یک لایه متن سبک.
 */
@Composable
fun ShimmerGoldText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle =
        androidx.compose.material3.MaterialTheme.typography.displaySmall
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    val transition = rememberInfiniteTransition(label = "titleShine")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "titlePhase"
    )
    val sweep = widthPx * 0.35f
    val startX = -sweep + phase * (widthPx + sweep * 2)
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier.onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) },
        style = style,
        color = {
            Brush.linearGradient(
                colors = listOf(Gold, Color(0xFFFFF7CF), Gold),
                start = Offset(startX, 0f),
                end = Offset(startX + sweep, 0f)
            )
        }
    )
}

/**
 * انفجار ذرات طلایی — جلوه یک‌باره (۱٫۲ ثانیه) هنگام صدور موفق فاکتور.
 * بدون انیمیشن دائمی: پس از پایان کاملاً از ترکیب خارج می‌شود (ضدهنگ).
 */
@Composable
fun GoldBurstOverlay(active: Boolean, onFinished: () -> Unit) {
    if (!active) return
    val progress = remember(active) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(active) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            tween(1200, easing = androidx.compose.animation.core.FastOutSlowIn)
        )
        onFinished()
    }
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.5f
        val p = progress.value
        repeat(42) { i ->
            val angle = (i * 137.5f) * (kotlin.math.PI / 180.0) // زاویه طلایی
            val speed = 0.55f + ((i * 29) % 45) / 100f
            val dist = p * maxR * speed
            val x = cx + kotlin.math.cos(angle) * dist
            val y = cy + kotlin.math.sin(angle) * dist * 0.8f + p * p * 120f
            val sz = ((3 + (i % 4) * 2).dp.toPx()) * (1f - p * 0.5f)
            val color = when (i % 3) {
                0 -> Gold
                1 -> ir.atiran.vizitor.ui.theme.NeonPurple
                else -> Color(0xFFFFF7CF)
            }
            drawRect(color = color, topLeft = Offset(x, y), size = Size(sz, sz), alpha = 1f - p)
        }
    }
}
