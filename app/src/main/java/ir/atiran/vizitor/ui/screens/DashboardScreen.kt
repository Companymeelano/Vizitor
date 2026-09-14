/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۱: پیشخوان من (Smart Dashboard)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  نمودار دایره‌ای تارگت روزانه (سبز نئونی) + پورسانت لحظه‌ای +
 *  لیست هوشمند مشتریان نیازمند پیگیری بر اساس افت خرید
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonDonutChart
import ir.atiran.vizitor.ui.components.SectionTitle
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.StatusDot
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonGreenGlow
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

@Composable
fun DashboardScreen(viewModel: VizitorViewModel) {
    val todaySales by viewModel.todaySales.collectAsState()
    val commission by viewModel.commission.collectAsState()
    val followUp by viewModel.followUpCustomers.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()

    val target = viewModel.dailyTarget
    val progress = if (target > 0) (todaySales.toFloat() / target).coerceIn(0f, 1f) else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("پیشخوان من")
                Text(
                    "نمای هوشمند فروش امروز شما",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // ── بنر مدال عملکرد بازاریاب (طلایی) ────────────────────────────────
        item {
            MedalBanner(progress = progress, remaining = (target - todaySales).coerceAtLeast(0))
        }

        // ── ردیف تارگت + پورسانت ────────────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NeonDonutChart(
                            progress = progress,
                            centerValue = "${(progress * 100).toInt()}٪".toFaDigits(),
                            centerLabel = "تارگت روزانه",
                            size = 128.dp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "فروش: ${todaySales.toFaPrice()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ویجت پورسانت لحظه‌ای
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp)
                            .neonGreenSurface()
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Text("پورسانت لحظه‌ای", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                commission.toFaPrice(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = NeonGreen
                            )
                        }
                    }
                    // ویجت فاکتورهای در انتظار سینک
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp)
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Text("در صف ارسال", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${pending.toFaNumber()} فاکتور",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (pending > 0) Gold else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── ویجت پرفروش‌ترین‌ها (از فاکتورهای محلی / سال مالی) ────────────────
        item {
            TopSellersCard(viewModel.topProducts.collectAsState().value)
        }

        // ── لیست هوشمند مشتریان نیازمند پیگیری (افت خرید) ───────────────────
        item {
            SectionTitle(
                text = "مشتریان نیازمند پیگیری (افت خرید)",
                icon = Icons.Filled.TrendingUp
            )
        }

        if (followUp.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "همه مشتریان در وضعیت پایدار هستند 🎉",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        items(followUp, key = { it.id }) { customer ->
            FollowUpCard(customer) { viewModel.selectCustomer(customer) }
        }

        item { MilanoFooter() }
    }
}

/**
 * بنر مدال عملکرد بازاریاب — حاشیه طلایی، نشان مدال و نوار پیشرفت سبک.
 * بدون انیمیشن دائمی: فقط یک نوار استاتیک برای حفظ روان‌بودن اسکرول.
 */
@Composable
private fun MedalBanner(progress: Float, remaining: Long) {
    val achieved = progress >= 1f
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .goldBorder()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Gold, Gold.copy(alpha = 0.15f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MilitaryTech,
                    contentDescription = "مدال عملکرد",
                    tint = Color(0xFF3A2A00),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (achieved) "مدال طلایی عملکرد فعال شد 🏆" else "مسیر مدال طلایی عملکرد",
                    style = MaterialTheme.typography.titleSmall,
                    color = Gold
                )
                Text(
                    if (achieved) "تارگت امروز کامل شد؛ عملکرد شما طلایی است!"
                    else "${remaining.toFaPrice()} فروش تا نشان طلا",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                // نوار پیشرفت سبک (بدون انیمیشن دائمی)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C2330))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    if (achieved) listOf(Gold, Color(0xFFFFF3C4))
                                    else listOf(NeonGreen, Color(0xFFB8FFD9))
                                )
                            )
                    )
                }
            }
        }
    }
}

/** سطح سبز نئونی ملایم برای ویجت پورسانت. */
private fun Modifier.neonGreenSurface(): Modifier = this.then(
    Modifier.background(
        Brush.linearGradient(listOf(NeonGreenGlow.copy(alpha = 0.10f), Color.Transparent)),
        RoundedCornerShape(22.dp)
    )
)

@Composable
private fun FollowUpCard(customer: CustomerEntity, onPick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (customer.isVip) Modifier.goldBorder() else Modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (customer.creditOk) NeonGreen else DangerRed)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (customer.isVip) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.MilitaryTech,
                            contentDescription = "VIP",
                            tint = Gold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    "آخرین خرید: ${customer.lastPurchaseDaysAgo.toFaNumber()} روز پیش",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            StatusChip(
                text = "افت ${customer.purchaseDropPercent.toFaNumber()}٪",
                color = DangerRed
            )
        }
    }
}

/**
 * ویجت پرفروش‌ترین‌ها — رتبه‌های طلایی بر پایه فاکتورهای محلی/سال مالی.
 */
@Composable
private fun TopSellersCard(tops: List<ir.atiran.vizitor.data.local.TopProduct>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionTitle(
                text = "پرفروش‌ترین‌های شما",
                icon = Icons.Filled.EmojiEvents
            )
            Spacer(Modifier.height(8.dp))
            if (tops.isEmpty()) {
                Text(
                    "پس از اولین فاکتور، پرفروش‌ها اینجا می‌درخشند. ✨",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            tops.forEachIndexed { i, top ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (i + 1).toFaNumber(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = when (i) {
                            0 -> Gold
                            1 -> Color(0xFFD7DEE9)
                            else -> Color(0xFFC98A5B)
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        top.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        "${top.total.toFaNumber()} واحد",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}
