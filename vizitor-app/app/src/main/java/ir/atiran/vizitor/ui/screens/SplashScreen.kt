/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحهٔ ورود لاکچری (Welcome Splash) v3.0.0
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  ▸ تیتر برند سه‌بعدی روی گوی جواهر + تیتر گرادیانی با عمق
 *  ▸ پنل «اتصال به سرور آتیران» به‌عنوان گام اول ورود (تنظیم → سپس نقش)
 *  ▸ شش کاشی نقش با گوی‌های سه‌بعدی، هالهٔ نور و فرو رفتن هنگام لمس
 *  ▸ ابعاد واکنشی: روی گوشی کوچک/ارزان و بزرگ/پرچمدار هر دو مرتب می‌نشیند
 *  ▸ همه جلوه‌ها با VizitorPerf روی دستگاه ضعیف خودکار ساده می‌شوند (بدون لگ)
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.R
import ir.atiran.vizitor.sqldirect.ServerSession
import ir.atiran.vizitor.ui.components.BrandOrb
import ir.atiran.vizitor.ui.components.GlowChip
import ir.atiran.vizitor.ui.components.GoldDivider
import ir.atiran.vizitor.ui.components.GradientTitle
import ir.atiran.vizitor.ui.components.IconOrb3D
import ir.atiran.vizitor.ui.components.LuxuryTile
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.SatinBackdrop
import ir.atiran.vizitor.ui.components.press3D
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber

/** قاب متحرک با پیشرفت یک‌باره — آلفا بین ابتدا→پایان پنجره. */
private fun segmentAlpha(progress: Float, start: Float, span: Float): Float =
    ((progress - start) / span).coerceIn(0f, 1f)

/** نقش‌های ورود با آواتار کاراکتریِ شغلی — ویزیتور فعال؛ بقیه «به‌زودی». */
private data class Role(
    val title: String,
    val subtitle: String,
    val avatarRes: Int,
    val active: Boolean
)

private val Roles = listOf(
    Role("مامور فروش", "ویزیتور سیار", R.drawable.nut_visitor, true),
    Role("مدیریت", "نظارت کل", R.drawable.nut_manager, false),
    Role("مدیر فروش", "تیم فروش", R.drawable.nut_sales, false),
    Role("حسابداری", "مالی و اسناد", R.drawable.nut_accountant, false),
    Role("انبار و پخش", "موجودی و ارسال", R.drawable.nut_warehouse, false),
    Role("کاربر فروشگاه", "فروش حضوری", R.drawable.role_shopkeeper, false)
)

@Composable
fun SplashScreen(
    onEnter: () -> Unit,
    onSoon: (String) -> Unit,
    serverSession: ServerSession = ServerSession(),
    onOpenServerConfig: () -> Unit = {},
    onQuickEnter: () -> Unit = {},
) {
    val p = vizitorPalette
    // وضعیت اتصال/ورود از والد (منبع واحد: VizitorSession) می‌آید
    val session = serverSession

    // انیمیشن ورود یک‌باره (بدون حلقه دائمی — ضد لگ استارتاپ)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1500)) }
    val t = enter.value

    val headA = segmentAlpha(t, 0.00f, 0.20f)
    val heroA = segmentAlpha(t, 0.10f, 0.22f)
    val panelA = segmentAlpha(t, 0.22f, 0.22f)
    val rolesH = segmentAlpha(t, 0.36f, 0.18f)
    val rolesA = segmentAlpha(t, 0.44f, 0.30f)
    val footA = segmentAlpha(t, 0.74f, 0.26f)

    Box(Modifier.fillMaxSize()) {
        // ══ بک‌گراند ساتن شبانه: هالهٔ نور + کمان‌های طلایی + غبار ══
        SatinBackdrop()

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // ابعاد واکنشی — مناسب همهٔ گوشی‌ها (کوچک، متوسط، بزرگ)
            val compact = maxWidth < 360.dp
            val shortScreen = maxHeight < 700.dp
            val hPad = if (compact) 14.dp else 18.dp
            val avatar = if (compact) 50.dp else 58.dp
            val topGap = if (shortScreen) 10.dp else 18.dp
            val blockGap = if (shortScreen) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = hPad, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(topGap))

                // ══════════ سرصفحهٔ برند — گوی جواهر + نام سامانه ══════════
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(headA)
                        .offset(y = ((1f - headA) * -14).dp)
                ) {
                    BrandOrb(size = if (compact) 50.dp else 58.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        GradientTitle(
                            text = "آتیران ویزیتور",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = if (compact) 19.sp else 21.sp
                            ),
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "سامانهٔ هوشمند ویزیت، ویترین و سفارش‌گیری",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconOrb3D(
                        icon = Icons.Filled.Shield,
                        size = 36.dp,
                        tint = p.gold,
                        glowColor = p.accent
                    )
                }

                Spacer(Modifier.height(blockGap))

                // ══════════ کارت معرفی (هیرو) — تیتر گرادیانی + مدیریت ══════════
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(heroA)
                        .offset(y = ((1f - heroA) * -10).dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    p.primary.copy(alpha = 0.16f),
                                    p.surface.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    p.gold.copy(alpha = 0.55f),
                                    p.primary.copy(alpha = 0.30f),
                                    p.goldDark.copy(alpha = 0.25f)
                                )
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = if (shortScreen) 10.dp else 14.dp)
                ) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = p.gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    GradientTitle(
                        text = "پخش عمدهٔ آجیل و خشکبار درخشان",
                        colors = listOf(p.gold, p.goldHighlight, p.gold),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = if (compact) 18.sp else 21.sp,
                            lineHeight = if (compact) 28.sp else 32.sp
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconOrb3D(icon = Icons.Filled.AutoAwesome, size = 24.dp, cornerRadius = 8.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "با مدیریت سرکار خانم حمدانی",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compact) 11.sp else 12.sp
                            ),
                            color = p.accentText
                        )
                    }
                }

                Spacer(Modifier.height(blockGap))

                // ══════════════════════════════════════════════════════════════
                //  پنل اتصال به سرور — گام اول ورود (تنظیم با ذخیره، سپس نقش)
                // ══════════════════════════════════════════════════════════════
                ServerConnectPanel(
                    session = session,
                    onConfigure = onOpenServerConfig,
                    onQuickEnter = onQuickEnter,
                    compact = compact,
                    modifier = Modifier.alpha(panelA)
                )

                Spacer(Modifier.height(blockGap))

                // ══════════ انتخاب نقش ══════════
                GoldDivider(label = "انتخاب نقش ورود", modifier = Modifier.alpha(rolesH))
                Spacer(Modifier.height(if (shortScreen) 10.dp else 14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(rolesA),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    Roles.chunked(2).forEach { rowRoles ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                        ) {
                            rowRoles.forEach { role ->
                                RoleTile(
                                    role = role,
                                    avatar = avatar,
                                    compact = compact,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (role.active) onEnter()
                                        else onSoon("بخش «${role.title}» به‌زودی فعال می‌شود 🚀")
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(if (shortScreen) 14.dp else 22.dp))

                // ══════════ فوتر امضای میلانو ══════════
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(footA)
                ) {
                    GoldDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "سامانهٔ هوشمند ویزیت، ویترین و سفارش‌گیری — نسخه ۲٫۱۳٫۶ (اتصال مستقیم SQL)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 16.sp),
                        color = p.gold.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconOrb3D(icon = Icons.Filled.Verified, size = 20.dp, cornerRadius = 7.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "طراحی و توسعه: گروه فنی و مهندسی میلانو",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "ایده‌پرداز و نویسنده: Milad Yaghoobi • Meelano Studio Design",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextSecondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ════════════════════════════ کاشی نقش ════════════════════════════

/** کاشی نقش — گوی سه‌بعدی آواتار + رینگ طلایی + عنوان و وضعیت. */
@Composable
private fun RoleTile(
    role: Role,
    avatar: Dp,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = vizitorPalette
    val accent = if (role.active) p.gold else Color(0x55FFFFFF)

    LuxuryTile(
        onClick = onClick,
        modifier = modifier,
        accent = accent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                IconOrb3D(
                    size = avatar,
                    glowColor = if (role.active) p.primary else p.textSecondary,
                    cornerRadius = avatar / 2
                ) {
                    Image(
                        painter = painterResource(role.avatarRes),
                        contentDescription = role.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatar - 8.dp)
                            .clip(CircleShape)
                    )
                }
                if (role.active) {
                    Box(
                        Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(p.accentDark)
                            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Login,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(p.surfaceDeep.copy(alpha = 0.92f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "به‌زودی",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                role.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 11.5.sp else 12.5.sp
                ),
                color = if (role.active) p.textPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (role.active) p.gold.copy(alpha = 0.15f)
                        else p.textSecondary.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    if (role.active) "ورود" else "به‌زودی",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (role.active) p.gold else TextSecondary
                )
            }
        }
    }
}

// ════════════════════════════ پنل اتصال سرور ════════════════════════════

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  پنل «اتصال به سرور آتیران» در صفحهٔ اول (پیش از انتخاب نقش)
 *  ─────────────────────────────────────────────────────────────────────────
 *  هوشمندی این پنل:
 *    • تنظیم‌نشده → دکمهٔ «تنظیم اتصال» (کارت نصب‌کننده/دستی، با ذخیرهٔ امن)
 *    • تنظیم‌شده  → دکمهٔ «ورود سریع» (نام کاربری/رمز ذخیره‌شده، بدون تایپ)
 *    • متصل       → نام دیتابیس/کاربر + شمارش سرویس‌ها + «ورود به پنل»
 *    • وضعیت، سرور و دیتابیس همیشه با چیپ نورانی و آیکن‌های تم دیده می‌شوند
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Composable
private fun ServerConnectPanel(
    session: ServerSession,
    onConfigure: () -> Unit,
    onQuickEnter: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val p = vizitorPalette
    val stateColor = when {
        session.loggedIn -> p.accent
        session.connected -> p.gold
        session.configured -> p.primary
        else -> p.danger
    }
    val stateText = when {
        session.loggedIn -> "متصل و آماده"
        session.connected -> "وصل به دیتابیس"
        session.configured -> "آمادهٔ ورود"
        else -> "تنظیم نشده"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        p.primary.copy(alpha = 0.14f),
                        p.surface.copy(alpha = 0.45f),
                        p.surfaceDeep.copy(alpha = 0.30f)
                    )
                )
            )
            .border(
                1.3.dp,
                Brush.linearGradient(
                    listOf(
                        stateColor.copy(alpha = 0.75f),
                        p.primary.copy(alpha = 0.40f),
                        p.goldDark.copy(alpha = 0.35f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        // ── سرصفحهٔ پنل ──────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconOrb3D(
                icon = Icons.Filled.Dns,
                size = 40.dp,
                tint = Color.White,
                glowColor = stateColor
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "اتصال به سرور آتیران",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (compact) 13.sp else 14.sp
                    ),
                    color = p.textPrimary
                )
                Text(
                    if (session.serverLabel.isBlank())
                        "اول اتصال را تنظیم کنید، بعد نقش خود را بزنید"
                    else session.serverLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            GlowChip(text = stateText, color = stateColor, pulse = session.busy || session.syncing)
        }

        // ── نوار پیشرفت / پیام ───────────────────────────────────────────
        if (session.busy || session.syncing) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = p.gold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    session.message.ifBlank {
                        if (session.syncing) "در حال همگام‌سازی سرویس‌ها…" else "در حال اتصال…"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = p.accentText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (session.loggedIn) {
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CountPill("کالا", session.productsCount, p.primary, Modifier.weight(1f))
                CountPill("مشتری", session.customersCount, p.accent, Modifier.weight(1f))
                CountPill("فاکتور", session.invoicesCount, p.gold, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── دکمه‌های اقدام (یک دکمهٔ اصلی روشن + یک دکمهٔ تنظیمات) ─────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                session.loggedIn -> NeonGreenButton(
                    text = "ورود به پنل",
                    icon = Icons.Filled.Login,
                    onClick = onQuickEnter,
                    modifier = Modifier.weight(1f)
                )
                session.readyForQuickEnter -> NeonPurpleButton(
                    text = "ورود سریع",
                    icon = Icons.Filled.Login,
                    onClick = onQuickEnter,
                    modifier = Modifier.weight(1f)
                )
                else -> NeonPurpleButton(
                    text = "تنظیم اتصال سرور",
                    icon = Icons.Filled.Settings,
                    onClick = onConfigure,
                    modifier = Modifier.weight(1f)
                )
            }
            GhostIconButton(
                icon = Icons.Filled.Sync,
                label = if (session.loggedIn) "همگام‌سازی" else "تنظیمات",
                onClick = if (session.loggedIn) onQuickEnter else onConfigure,
                tint = p.gold
            )
        }

        if (session.loggedIn && session.lastSyncSummary.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "آخرین همگام‌سازی: ${session.lastSyncSummary}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!session.loggedIn && !session.busy && !session.syncing && session.message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                session.message,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (session.configured) p.accentText else p.gold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** شمارندهٔ سرویس‌های داده — پیل شیشه‌ای کوچک. */
@Composable
private fun CountPill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    val p = vizitorPalette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(vertical = 7.dp, horizontal = 4.dp)
    ) {
        Text(
            count.toFaNumber(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            ),
            color = p.textPrimary
        )
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = TextSecondary)
    }
}

/** دکمهٔ شیشه‌ای آیکن‌دار (کنش‌های کم‌اهمیت‌تر) — هم‌رنگ و سه‌بعدی. */
@Composable
private fun GhostIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val p = vizitorPalette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .press3D(depth = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(p.textPrimary.copy(alpha = 0.10f), p.surfaceDeep.copy(alpha = 0.30f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = tint
        )
    }
}
