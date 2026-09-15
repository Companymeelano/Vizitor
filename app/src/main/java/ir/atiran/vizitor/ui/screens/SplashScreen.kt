/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحه ورود لوکس (Welcome Splash) v2.5.0
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  عنوان شاخص برند با ورود متحرک پلکانی، دکمه ورود همگام با تم انتخابی،
 *  و سطر امضای میلانو در فوتر — همه سازگار با چرم و پالت زنده.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

/** قاب متحرک با پیشرفت یک‌باره — آلفا و سرعت رندر میان ابتدا→پایان بازه. */
private fun segmentAlpha(progress: Float, start: Float, span: Float): Float =
    ((progress - start) / span).coerceIn(0f, 1f)

@Composable
fun SplashScreen(onEnter: () -> Unit) {
    val p = vizitorPalette

    // انیمیشن ورود یک‌باره (بدون حلقه دائمی — ضد لگ استارتاپ)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1400)) }
    val t = enter.value

    val titleA = segmentAlpha(t, 0.05f, 0.30f)
    val brandA = segmentAlpha(t, 0.20f, 0.30f)
    val dividerA = segmentAlpha(t, 0.40f, 0.25f)
    val btnA = segmentAlpha(t, 0.55f, 0.30f)
    val footA = segmentAlpha(t, 0.75f, 0.25f)

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ══ نام شاخص پخش — قهرمان صفحه ══
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(titleA)
                    .offset(y = ((1f - titleA) * -26).dp)
            ) {
                ShimmerGoldText("پخش عمده آجیل")
                ShimmerGoldText("و خشکبار درخشان ✨")
            }

            Spacer(Modifier.height(10.dp))

            // ══ خط مدیریت — مکمل شاخص، نه جایگزین ══
            Text(
                "با مدیریت سرکار خانم حمدانی 💎",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                ),
                color = p.accentText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(brandA)
                    .offset(y = ((1f - brandA) * 18).dp)
            )

            Spacer(Modifier.height(22.dp))

            // ══ تیگره طلایی دو سویینج ══
            Box(
                Modifier
                    .alpha(dividerA)
                    .width(140.dp)
                    .height(1.4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, p.gold.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(26.dp))

            // ══ دکمه ورود — گرادیان جادویی تم انتخابی کاربر + هستیک مگنتیک ══
            NeonPurpleButton(
                text = "ورود به ویزیتور ✨",
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .alpha(btnA)
                    .graphicsLayer {
                        val s = 0.9f + btnA * 0.1f
                        scaleX = s; scaleY = s
                    }
            )

            Spacer(Modifier.height(14.dp))

            // ══ خط راهنمای مختصر — بدون رقابت با نام پخش ══
            Text(
                "برنامه اختصاصی ویزیتورها",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                modifier = Modifier.alpha(btnA)
            )
        }

        // ══ فوتر امضا — کوچک و ظریف، در پایین صفحه ══
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(footA)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(90.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Gold.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "طراحی و توسعه: گروه فنی و مهندسی میلانو",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Text(
                "ایده‌پرداز و نویسنده: Milad Yaghoobi",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = TextSecondary.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}
