/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحه ورود چند‌نقشه لوکس (Welcome Splash) v2.6.0
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کارت اطلاعات برند روی زمینه چرم تم + انتخاب نقش هوشمند با آواتارها:
 *  ویزیتور (فعال) | مدیریت / انبار و پخش / حسابداری (به‌زودی) — همه همگام
 *  با پالت انتخابی کاربر، بدون نمایش منوی پایین.
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.R
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

/** قاب متحرک با پیشرفت یک‌باره — آلفا بین ابتدا→پایان پنجره. */
private fun segmentAlpha(progress: Float, start: Float, span: Float): Float =
    ((progress - start) / span).coerceIn(0f, 1f)

/** نقش‌های ورود و آواتارشان — ویزیتور فعال است؛ بقیه «به‌زودی». */
private data class Role(val title: String, val avatarRes: Int, val active: Boolean)

private val Roles = listOf(
    Role("مامور فروش (ویزیتور)", R.drawable.avatar_m_visitor, true),
    Role("مدیریت", R.drawable.avatar_f_manager, false),
    Role("انبار و پخش", R.drawable.avatar_m_warehouse, false),
    Role("حسابداری", R.drawable.avatar_f_visitor, false)
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

    val titleA = segmentAlpha(t, 0.02f, 0.24f)
    val brandA = segmentAlpha(t, 0.16f, 0.26f)
    val infoA = segmentAlpha(t, 0.30f, 0.26f)
    val rolesH = segmentAlpha(t, 0.46f, 0.20f)
    val rolesA = segmentAlpha(t, 0.56f, 0.28f)
    val footA = segmentAlpha(t, 0.78f, 0.22f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(14.dp))

        // ══ خط کوچک برند بالای صفحه ══
        Text(
            "Milano • آتیران ویزیتور",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = Gold.copy(alpha = 0.85f),
            modifier = Modifier.alpha(titleA)
        )
        Spacer(Modifier.height(6.dp))

        // ══ عنوان شاخص پخش — قهرمان صفحه ══
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(titleA)
                .offset(y = ((1f - titleA) * -22).dp)
        ) {
            ShimmerGoldText("پخش عمده آجیل")
            ShimmerGoldText("و خشکبار درخشان ✨")
        }

        Spacer(Modifier.height(6.dp))
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
                .offset(y = ((1f - brandA) * 14).dp)
        )

        Spacer(Modifier.height(16.dp))

        // ══ کارت اطلاعات برند — شیشه‌ای با خط طلایی ══
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(infoA)
                .offset(y = ((1f - infoA) * 14).dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "سامانه هوشمند ویزیت، ویترین و سفارش‌گیری",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = p.accentText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .width(120.dp)
                        .height(1.2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, p.gold.copy(alpha = 0.9f), Color.Transparent)
                            )
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "طراحی و توسعه: گروه فنی و مهندسی میلانو",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "ایده‌پرداز و نویسنده: Milad Yaghoobi",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = TextSecondary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(18.dp))

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

        Spacer(Modifier.height(12.dp))

        // ══ چهار کارت نقش — ویزیتور فعال؛ بقیه «به‌زودی» ══
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

        // ══ فوتر امضا ══
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(footA)
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
            Spacer(Modifier.height(8.dp))
            Text(
                "نسخه ۲٫۶٫۰ — ساخته‌شده برای ویزیتورها با ❤️",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextSecondary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** کارت نقش — آواتار سه‌بعدی با حلقه تم + عنوان + وضعیت فعال‌بودن. */
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
        // آواتار نقش با حلقه گرادیان تم
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
                fontSize = 11.5.sp
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
