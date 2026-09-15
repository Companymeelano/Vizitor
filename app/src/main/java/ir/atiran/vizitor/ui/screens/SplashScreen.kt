/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحه ورود چند‌نقشه لوکس (Welcome Splash) v2.7.0
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  عنوان شاخص پخش با تایپوگرافی کالیبره‌شده + ۶ کارت نقش با آواتارهای
 *  کاراکتریِ شغلی (هم‌سیاق شخصیت‌های آجیل) + فوتر هوشمند با امضای میلانو.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.R
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

/** قاب متحرک با پیشرفت یک‌باره — آلفا بین ابتدا→پایان پنجره. */
private fun segmentAlpha(progress: Float, start: Float, span: Float): Float =
    ((progress - start) / span).coerceIn(0f, 1f)

/** نقش‌های ورود با آواتار کاراکتریِ شغلی — ویزیتور فعال؛ بقیه «به‌زودی». */
private data class Role(val title: String, val avatarRes: Int, val active: Boolean)

private val Roles = listOf(
    Role("مامور فروش (ویزیتور)", R.drawable.nut_visitor, true),
    Role("مدیریت", R.drawable.nut_manager, false),
    Role("مدیر فروش", R.drawable.nut_sales, false),
    Role("حسابداری", R.drawable.nut_accountant, false),
    Role("انبار و پخش", R.drawable.nut_warehouse, false),
    Role("کاربر فروشگاه", R.drawable.role_shopkeeper, false)
)

@Composable
fun SplashScreen(
    onEnter: () -> Unit,
    onSoon: (String) -> Unit
) {
    val p = vizitorPalette

    // انیمیشن ورود یک‌باره (بدون حلقه دائمی — ضد لگ استارتاپ)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1500)) }
    val t = enter.value

    val titleA = segmentAlpha(t, 0.02f, 0.22f)
    val brandA = segmentAlpha(t, 0.14f, 0.22f)
    val rolesH = segmentAlpha(t, 0.32f, 0.20f)
    val rolesA = segmentAlpha(t, 0.42f, 0.30f)
    val footA = segmentAlpha(t, 0.74f, 0.26f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))

        // ══ عنوان شاخص پخش — تایپوگرافی کالیبره‌شده با گرادیان طلای تم ══
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(titleA)
                .offset(y = ((1f - titleA) * -20).dp)
        ) {
            Text(
                "پخش عمده آجیل و خشکبار درخشان",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    letterSpacing = 0.2.sp,
                    brush = Brush.horizontalGradient(
                        listOf(p.gold, p.goldHighlight, p.gold)
                    )
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(8.dp))

        // ══ خط مدیریت — کالیبره‌شده و در اندازه دقیق ══
        Text(
            "با مدیریت سرکار خانم حمدانی 💎",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                letterSpacing = 0.3.sp
            ),
            color = p.accentText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(brandA)
                .offset(y = ((1f - brandA) * 12).dp)
        )

        Spacer(Modifier.height(22.dp))

        // ══ عنوان «انتخاب نقش ورود» با دو خط طلایی دو سویینج ══
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(rolesH)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, p.gold.copy(alpha = 0.7f)))
                    )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "انتخاب نقش ورود",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Gold
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(listOf(p.gold.copy(alpha = 0.7f), Color.Transparent))
                    )
            )
        }

        Spacer(Modifier.height(14.dp))

        // ══ شش کارت نقش (۲ ستون × ۳ ردیف، ترتیب دقیق) ══
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(rolesA),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Roles.chunked(2).forEach { rowRoles ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowRoles.forEach { role ->
                        RoleCard(
                            role = role,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (role.active) onEnter()
                                else onSoon("بخش «» ${role.title} » به‌زودی فعال می‌شود 🚀")
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ══ فوتر هوشمند — امضای میلانو + نسخه، در قاب مینیمال ══
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(footA)
        ) {
            Box(
                Modifier
                    .width(130.dp)
                    .height(1.2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, p.gold.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "سامانه هوشمند ویزیت، ویترین و سفارش‌گیری — نسخه ۲٫۱۳٫۴",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = Gold.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "طراحی و توسعه: گروه فنی و مهندسی میلانو • ایده‌پرداز و نویسنده: Milad Yaghoobi",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 12.sp),
                color = TextSecondary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** کارت نقش — آواتار کاراکتری شغلی با حلقه گرادیان تم + عنوان + وضعیت. */
@Composable
private fun RoleCard(
    role: Role,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = vizitorPalette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x14FFFFFF))
            .border(
                BorderStroke(
                    1.dp,
                    if (role.active) p.gold.copy(alpha = 0.65f) else Color(0x22FFFFFF)
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    Brush.linearGradient(
                        if (role.active) listOf(p.gold, p.primary)
                        else listOf(Color(0x55FFFFFF), Color(0x22FFFFFF))
                    ),
                    CircleShape
                )
                .padding(2.dp)
        ) {
            Image(
                painter = painterResource(role.avatarRes),
                contentDescription = role.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            if (!role.active) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x7A0B1220)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "به‌زودی",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            role.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            ),
            color = if (role.active) p.accentText else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (role.active) {
                Icon(
                    Icons.Filled.Login,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("ورود", style = MaterialTheme.typography.labelSmall, color = Gold)
            } else {
                Text("به‌زودی", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}
