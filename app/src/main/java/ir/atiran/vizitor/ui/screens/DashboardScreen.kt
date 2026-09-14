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
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.ui.components.RoyalSurfaceBrush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Flag
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
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

        // ── تارگت ویزیتور — کارت کامل و هم‌اندازه سایر بخش‌ها ────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonDonutChart(
                        progress = progress,
                        centerValue = "${(progress * 100).toInt()}٪".toFaDigits(),
                        centerLabel = "پیشرفت",
                        size = 150.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        RoyalHeader(text = "تارگت ویزیتور امروز", icon = Icons.Filled.Flag)
                        Spacer(Modifier.height(10.dp))
                        TargetStatRow("هدف روزانه", target.toFaPrice(), Color(0xFFE3BFFF))
                        TargetStatRow("فروش امروز", todaySales.toFaPrice(), NeonGreen)
                        TargetStatRow(
                            "مانده تا هدف",
                            (target - todaySales).coerceAtLeast(0).toFaPrice(),
                            if (todaySales >= target) NeonGreen else Gold
                        )
                        TargetStatRow(
                            "وضعیت",
                            if (progress >= 1f) "هدف محقق شد 🏆"
                            else "${(progress * 100).toInt()}٪ تکمیل".toFaDigits(),
                            if (progress >= 1f) NeonGreen else NeonPurple
                        )
                    }
                }
            }
        }

        // ── فاکتورهای در صف ارسال ──────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
                Column(Modifier.fillMaxWidth()) {
                    Text("در صف ارسال", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${pending.toFaNumber()} فاکتور",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Gold
                    )
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
 * جدول سه‌بعدی هشدار بدهی مشتریان — رتبه، مشتری، نوار بدهی و مبلغ،
 * با سربرگ گرادیانی بنفش و ردیف جمع طلایی.
 */
@Composable
private fun DebtorsCard(debtors: List<ir.atiran.vizitor.data.local.CustomerEntity>) {
    val list = debtors.filter { it.debt > 0 }
    val maxDebt = list.maxOfOrNull { it.debt }?.coerceAtLeast(1L) ?: 1L
    val sum = list.sumOf { it.debt }
    val medals = listOf("🥇", "🥈", "🥉")
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .royalBorder()
    ) {
        Column {
            RoyalHeader(text = "هشدار بدهی مشتریان", icon = Icons.Filled.WarningAmber)
            Spacer(Modifier.height(10.dp))
            if (list.isEmpty()) {
                Text(
                    "هیچ مشتری بدهکاری وجود ندارد 🎉",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoyalSurfaceBrush)
                        .border(1.dp, Color(0x33B04BF8), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(NeonPurple, NeonPurpleDark)))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text("رتبه", Modifier.weight(0.55f), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Text("مشتری", Modifier.weight(1.7f), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Text("مانده بدهی", Modifier.weight(1.15f), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End)
                    }
                    list.forEachIndexed { i, c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (i % 2 == 0) Color(0x10FFFFFF) else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(medals.getOrElse(i) { (i + 1).toFaNumber() }, Modifier.weight(0.55f), fontSize = 15.sp)
                            Column(Modifier.weight(1.7f)) {
                                Text(c.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
                                Text(c.city, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0x1AFFFFFF))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth((c.debt.toFloat() / maxDebt).coerceIn(0.08f, 1f))
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Brush.horizontalGradient(listOf(DangerRed, Gold)))
                                    )
                                }
                            }
                            Text(
                                c.debt.toFaPrice(),
                                Modifier.weight(1.15f),
                                color = DangerRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Gold.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مجموع بدهی این مشتریان", Modifier.weight(1f), color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Text(sum.toFaPrice(), color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** ردیف آمار کارت تارگت — برچسب + مقدار رنگی. */
@Composable
private fun TargetStatRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = color)
    }
}
