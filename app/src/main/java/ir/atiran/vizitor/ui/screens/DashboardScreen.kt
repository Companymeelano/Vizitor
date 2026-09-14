/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۱: پیشخوان من (Smart Dashboard)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
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
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.BarChart
import ir.atiran.vizitor.ui.components.RoyalBarChart
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.RoyalTable
import ir.atiran.vizitor.ui.components.royalBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val followUp by viewModel.followUpCustomers.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()

    val invoices by viewModel.invoices.collectAsState()
    val debtorsAll by viewModel.customers.collectAsState()
    val weekly = remember(invoices, todaySales) { buildWeekly(invoices, todaySales) }
    val debtors = remember(debtorsAll) { debtorsAll.sortedByDescending { it.debt }.take(3) }

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

        // ── ردیف تارگت روزانه ───────────────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(200.dp)
                        .royalBorder()
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
                    // ویجت فاکتورهای در انتظار سینک
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .royalBorder()
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

        // ── چارت ستونی سه‌بعدی فروش هفتگی ──────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
                Column {
                    RoyalHeader(text = "فروش هفتگی (نمودار سه‌بعدی)", icon = Icons.Filled.BarChart)
                    Spacer(Modifier.height(6.dp))
                    RoyalBarChart(
                        data = weekly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    RoyalTable(data = weekly)
                }
            }
        }

        // ── سه مشتری با بیشترین بدهکاری ────────────────────────────────────
        item { DebtorsCard(debtors) }

        // ── ویجت پرفروش‌ترین‌ها (از فاکتورهای محلی / سال مالی) ────────────────
        item {
            TopSellersCard(viewModel.topProducts.collectAsState().value)
        }

        // ── لیست هوشمند مشتریان نیازمند پیگیری (افت خرید) ───────────────────
        item {
            RoyalHeader(
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
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(
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

/** ساخت داده هفتگی چارت ستونی (۷ روز اخیر؛ در نبود داده، دمو). */
private fun buildWeekly(
    invoices: List<ir.atiran.vizitor.data.local.InvoiceEntity>,
    todaySales: Long
): List<Pair<String, Long>> {
    val labels = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val out = mutableListOf<Pair<String, Long>>()
    for (back in 6 downTo 0) {
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(java.util.Calendar.DAY_OF_YEAR, -back)
        }
        val start = (c.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 86_400_000L
        var sum = invoices.filter { it.createdAt in start until end }.sumOf { it.finalAmount }
        if (sum == 0L && back == 0) sum = todaySales
        out += labels[c.get(java.util.Calendar.DAY_OF_WEEK) % 7] to sum
    }
    return if (out.all { it.second == 0L }) {
        // داده نمایشی برای پیش‌نمایش جذاب چارت
        listOf(32, 45, 28, 61, 52, 74, 40).mapIndexed { i, v -> out[i].first to v * 1_000_000L }
    } else out
}

/**
 * کارت سه مشتری با بیشترین بدهکاری — حاشیه قرمز/طلایی و مبالغ برجسته.
 */
@Composable
private fun DebtorsCard(debtors: List<ir.atiran.vizitor.data.local.CustomerEntity>) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .royalBorder()
    ) {
        Column {
            RoyalHeader(text = "هشدار بدهی — بیشترین مانده حساب", icon = Icons.Filled.WarningAmber)
            Spacer(Modifier.height(8.dp))
            if (debtors.all { it.debt <= 0 }) {
                Text(
                    "هیچ مشتری بدهکاری وجود ندارد 🎉",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            debtors.filter { it.debt > 0 }.forEachIndexed { i, c ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (i + 1).toFaNumber(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = DangerRed
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            c.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Text(
                        c.debt.toFaPrice(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed
                        )
                    )
                }
            }
        }
    }
}
