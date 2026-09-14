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
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
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
    modifier: Modifier = Modifier
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
    val startX = -sweep + phase * (widthPx + sweep * 2f)
    val style = MaterialTheme.typography.displaySmall.copy(
        brush = Brush.horizontalGradient(
            colors = listOf(Gold, Color(0xFFFFF7CF), Gold),
            startX = startX,
            endX = startX + sweep,
            tileMode = androidx.compose.ui.graphics.TileMode.Clamp
        )
    )
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier.onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) },
        style = style
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
        progress.animateTo(1f, tween(1200))
        onFinished()
    }
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.5f
        val p = progress.value
        repeat(42) { i ->
            val angle = i * 2.39996f // زاویه طلایی (رادیان)
            val speed = 0.55f + ((i * 29) % 45) / 100f
            val dist = p * maxR * speed
            val x = cx + cos(angle) * dist
            val y = cy + sin(angle) * dist * 0.8f + p * p * 120f
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

// ════════════════════════ تم سلطنتی بنفش (نسخه ۱٫۵٫۰) ════════════════════════

/** گرادیان سطح سلطنتی بنفش برای کارت‌ها و جدول‌ها. */
val RoyalSurfaceBrush = Brush.verticalGradient(listOf(Color(0xFF1E1133), Color(0xFF130B20)))

/** قاب سلطنتی: حاشیه گرادیانی بنفش → یاسی → طلایی. */
fun Modifier.royalBorder(shape: Shape = RoundedCornerShape(22.dp)): Modifier = this.border(
    width = 1.5.dp,
    brush = Brush.linearGradient(listOf(NeonPurple, Color(0xFFE3BFFF), Gold)),
    shape = shape
)

/** سرتیتر سلطنتی بخش‌ها — آیکون در گوی بنفش + متن گرادیانی درخشان. */
@Composable
fun RoyalHeader(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonPurple, NeonPurpleDark)))
                .border(1.dp, Color(0x55FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.horizontalGradient(listOf(Color(0xFFF3E8FF), NeonPurple, Gold))
            )
        )
    }
}

/**
 * قاب «شفق قطبی» دور تصویر کالا: حاشیه گرادیانی متحرک (بنفش ↔ طلایی ↔ سبز)
 * با هاله نرم بیرونی — بسیار چشم‌نواز ولی سبک و روان.
 */
@Composable
fun Modifier.auroraFrame(shape: Shape = RoundedCornerShape(16.dp)): Modifier {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    return this.drawWithContent {
        drawContent()
        val t = (1f + kotlin.math.sin(phase * 2f * Math.PI.toFloat())) / 2f
        val colors = listOf(
            lerp(NeonPurple, Gold, t),
            lerp(Gold, NeonGreen, t),
            lerp(NeonGreen, NeonPurple, t)
        )
        val stroke = 3.dp.toPx()
        val inset = stroke / 2f
        drawRoundRect(
            brush = Brush.linearGradient(colors),
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(stroke)
        )
        // هاله نرم بیرونی قاب
        drawRoundRect(
            brush = Brush.linearGradient(colors),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(20.dp.toPx()),
            style = Stroke(stroke * 3f),
            alpha = 0.16f
        )
    }
}
