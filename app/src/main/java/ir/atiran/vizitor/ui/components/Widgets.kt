/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ویجت‌های مشترک رابط کاربری (نسخه لاکچری)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  دکمه‌های لاکچری با حلقه طلایی، براق استودیویی، جاروب نور و مقیاس لمسی.
 *  همه جلوه‌ها لایه‌ای و سبک هستند (بدون blur/shadow) — روان روی هر گوشی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.GoldDark
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.TextSecondary

/**
 * دکمه اکشن اصلی لاکچری — گرادیان بنفش سلطنتی + حلقه طلایی + جاروب نور.
 * (جایگزین سبک قبلی؛ تمام فراخوان‌های قبلی بدون تغییر امضا استفاده می‌شوند)
 */
@Composable
fun NeonPurpleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(20.dp)
    LuxurySurface(
        modifier = modifier
            .height(58.dp)
            .pressScale()
            .shineSweep()
            .border(1.dp, Gold.copy(alpha = if (enabled) 0.65f else 0.15f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        fillBrush = Brush.verticalGradient(
            colors = if (enabled)
                listOf(Color(0xFFB06CFF), Color(0xFF7A2FE0), Color(0xFF4A1799))
            else
                listOf(Color(0xFF2A2F3A), Color(0xFF232733))
        ),
        enabled = enabled
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            )
        }
    }
}

/**
 * دکمه ثانویه لاکچری — گرادیان زمردی/سبز نئونی با حلقه طلایی.
 */
@Composable
fun NeonGreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(18.dp)
    LuxurySurface(
        modifier = modifier
            .height(50.dp)
            .pressScale()
            .shineSweep()
            .border(1.dp, Color(0xFF063D24).copy(alpha = if (enabled) 0.9f else 0.2f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        fillBrush = Brush.verticalGradient(
            colors = if (enabled)
                listOf(Color(0xFF8CFFCB), Color(0xFF2BFF88), Color(0xFF0CB35C))
            else
                listOf(Color(0xFF2A2F3A), Color(0xFF232733))
        ),
        enabled = enabled
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

/** عنوان هر بخش از صفحه با نشان طلایی. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** برچسب وضعیت کوچک رنگی با حلقه ظریف هم‌رنگ. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

/**
 * ═ فوتر کپی‌رایت — الزام هویتی پروژه ═
 * متن با گرادیان طلایی و جداکننده درخشان.
 */
@Composable
fun MilanoFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color(0xFFFFF6CC), Gold, Color.Transparent))
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Developed by Milano Technical Team, Milad Yaghoobi",
            color = Gold,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
    }
}
