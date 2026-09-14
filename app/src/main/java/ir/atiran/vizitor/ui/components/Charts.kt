/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نمودارهای سفارشی (نمودار حلقوی تارگت فروش)
 *  Developed by Milano Technical Team, Milad Yaghoobi
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonGreenGlow
import ir.atiran.vizitor.ui.theme.TextSecondary
import kotlin.math.min

/**
 * نمودار دایره‌ای (حلقوی) تارگت فروش روزانه با رنگ سبز نئونی و درخشش.
 * @param progress مقدار پیشرفت بین 0 تا 1
 */
@Composable
fun NeonDonutChart(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 150.dp,
    trackColor: Color = Color(0xFF1C2330),
    progressBrush: Brush = Brush.sweepGradient(
        colors = listOf(Color(0xFF0FBF62), NeonGreen, Color(0xFFB8FFD9), NeonGreen)
    )
) {
    val animated by produceState(initialValue = 0f, key1 = progress) {
        var current = 0f
        val step = (progress - current) / 40f
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
            val pad = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            // ریل خاکستری
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // هاله سبز نئونی
            drawArc(
                brush = progressBrush,
                startAngle = -90f,
                sweepAngle = 360f * animated.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(stroke + 10f, cap = StrokeCap.Round),
                alpha = 0.18f
            )
            // قوس اصلی پیشرفت
            drawArc(
                brush = progressBrush,
                startAngle = -90f,
                sweepAngle = 360f * animated.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
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

/** نشانگر کوچک رنگی وضعیت (سبز/قرمز) برای اعتبار مشتری. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        drawCircle(color = color.copy(alpha = 0.25f), radius = this.size.minDimension / 2f)
        drawCircle(color = color, radius = this.size.minDimension / 4f)
    }
}
