/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نمودارهای سه‌بعدی لاکچری
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  دونات با عمق و سایه (حس سه‌بعدی) + چارت ستونی استوانه‌ای با بازتاب.
 *  همه‌چیز یک‌لایه و استاتیک/کم‌فریم — بدون کوچک‌ترین لگ.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import kotlin.math.min

/**
 * نمودار حلقوی سه‌بعدی تارگت فروش: لایه عمق تاریک + ریل + قوس گرادیانی
 * سبز نئونی + هایلایت استودیویی + تیک‌های طلایی دور.
 */
@Composable
fun NeonDonutChart(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    trackColor: Color = Color(0xFF1C2330),
    progressBrush: Brush = Brush.sweepGradient(
        colors = listOf(Color(0xFF0FBF62), NeonGreen, Color(0xFFB8FFD9), NeonGreen)
    )
) {
    val animated by produceState(initialValue = 0f, key1 = progress) {
        var current = 0f
        val step = progress / 40f
        while (current < progress) {
            current = min(current + kotlin.math.abs(step), progress)
            value = current
            kotlinx.coroutines.delay(12)
        }
        value = progress
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val pad = stroke / 2f + 6f
            val arcSize = Size(this.size.width - stroke - 12f, this.size.height - stroke - 12f)
            val tl = Offset(pad, pad)
            val sweep = 360f * animated.coerceIn(0f, 1f)

            // ۱) سایه عمق (حس سه‌بعدی) — حلقه تاریک کمی پایین‌تر
            drawArc(
                color = Color(0xFF04060A),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(pad, pad + 7f), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۲) ریل خاکستری
            drawArc(
                color = trackColor,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۳) تیک‌های طلایی دور نمودار (۱۲ نشان)
            repeat(12) { i ->
                val a = Math.toRadians((i * 30 - 90).toDouble())
                val r1 = this.size.width / 2f - 3f
                val r2 = this.size.width / 2f - 9f
                val cx = this.size.width / 2f
                drawLine(
                    Gold.copy(alpha = 0.55f),
                    Offset(cx + kotlin.math.cos(a).toFloat() * r1, cx + kotlin.math.sin(a).toFloat() * r1),
                    Offset(cx + kotlin.math.cos(a).toFloat() * r2, cx + kotlin.math.sin(a).toFloat() * r2),
                    strokeWidth = 2.5f
                )
            }
            // ۴) هاله نئونی پیشرفت
            drawArc(
                brush = progressBrush,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(stroke + 10f, cap = StrokeCap.Round),
                alpha = 0.16f
            )
            // ۵) قوس اصلی پیشرفت
            drawArc(
                brush = progressBrush,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۶) هایلایت استودیویی باریک روی قوس
            if (sweep > 4f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(tl.x + stroke * 0.28f, tl.y + stroke * 0.28f),
                    size = Size(arcSize.width - stroke * 0.56f, arcSize.height - stroke * 0.56f),
                    style = Stroke(2.5f, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = NeonGreen
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * چارت ستونی سه‌بعدی هفتگی: ستون‌های گرادیانی طلا→بنفش با درپوش بیضوی
 * براق (حس استوانه) و بازتاب ملایم زیر خط مبنا + برچسب روزها.
 */
@Composable
fun RoyalBarChart(
    data: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val base = h - 40f
        val max = data.maxOf { it.second }.coerceAtLeast(1L).toFloat()
        val bw = w / data.size
        val barW = bw * 0.44f

        // خط مبنا طلایی
        drawLine(Gold.copy(alpha = 0.7f), Offset(8f, base), Offset(w - 8f, base), 2f)

        data.forEachIndexed { i, entry ->
            val (label, v) = entry
            val hVal = (v / max) * (base - 40f)
            val x = i * bw + (bw - barW) / 2f
            val y = base - hVal

            // بازتاب زیر مبنا
            drawRoundRect(
                color = NeonPurple.copy(alpha = 0.12f),
                topLeft = Offset(x, base + 5f),
                size = Size(barW, min(hVal * 0.22f, 16f)),
                cornerRadius = CornerRadius(6f)
            )
            // بدنه ستون (گرادیان طلا → بنفش)
            if (hVal > 4f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFE9A8), Gold, NeonPurple.copy(alpha = 0.85f)),
                        startY = y, endY = base
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barW, hVal),
                    cornerRadius = CornerRadius(10f)
                )
                // درپوش بیضوی براق (حس استوانه سه‌بعدی)
                drawOval(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFF7CF), Gold)
                    ),
                    topLeft = Offset(x, y - 7f),
                    size = Size(barW, 14f)
                )
            }
            // برچسب روز
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barW / 2f,
                h - 12f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9AA3B2")
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

/** نشانگر کوچک رنگی وضعیت (سبز/قرمز) برای اعتبار مشتری. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        drawCircle(color = color.copy(alpha = 0.25f), radius = this.size.minDimension / 2f)
        drawCircle(color = color, radius = this.size.minDimension / 4f)
    }
}
