/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۱: پیشخوان من (Smart Dashboard) v1.7.0
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  بازطراحی کامل با تم سلطنتی یکدست:
 *    ▸ همه بخش‌ها: کارت شیشه‌ای + قاب سلطنتی (royalBorder) + RoyalHeader
 *    ▸ چارت‌های سه‌بعدی هماهنگ با تم (دونات عمق‌دار، ستونی سه‌بعدی،
 *      نشان‌های رتبه طلایی/نقره‌ای/برنزی و نوارهای پیشرفت عمق‌دار)
 *    ▸ ترتیب بخش‌ها: تارگت ویزیتور ← بدهی مشتریان ← صف ارسال ← فروش هفتگی
 *      ← پرفروش‌ترین‌ها ← مشتریان نیازمند پیگیری
 *    ▸ مقیاس یکسان: فاصله ۱۶dp بین بخش‌ها، ۱۶dp پدینگ داخلی، تایپوگرافی تم
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.local.TopProduct
import ir.atiran.vizitor.ui.components.DepthBar
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonDonutChart
import ir.atiran.vizitor.ui.components.RankBadge3D
import ir.atiran.vizitor.ui.components.RoyalBarChart
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.RoyalSurfaceBrush
import ir.atiran.vizitor.ui.components.RoyalTable
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.StatusDot
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.GoldDark
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.ui.theme.TextPrimary
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

/** گرادیان هشدار بدهی — سرخ تم با هایلایت ملایم. */
private val DebtBarColors = listOf(Color(0xFFFF4D6D), Color(0xFFFF8FA3))

/** حاشیه ظریف یکسان برای جدول‌های سلطنتی داخل کارت‌ها — از رنگ اصلی تم. */
private val RoyalTableBorder: Color @Composable get() = NeonPurple.copy(alpha = 0.20f)

/** گرادیان سلطنتی (رنگ اصلی→طلایی) برای سهم فروش کالاها — از پالت تم. */
private val ProductBarColors: List<Color> @Composable get() = listOf(NeonPurple, Gold)

@Composable
fun DashboardScreen(viewModel: VizitorViewModel) {
    val todaySales by viewModel.todaySales.collectAsState()
    val followUp by viewModel.followUpCustomers.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val tops by viewModel.topProducts.collectAsState()

    val weekly = remember(invoices, todaySales) { buildWeekly(invoices, todaySales) }
    val debtors = remember(customers) {
        customers.filter { it.debt > 0 }.sortedByDescending { it.debt }.take(3)
    }
    val sentToday = remember(invoices) { countSentToday(invoices) }

    val target = viewModel.dailyTarget
    val progress = if (target > 0) (todaySales.toFloat() / target).coerceIn(0f, 1f) else 0f
    val syncRatio =
        if (pending + sentToday > 0) sentToday.toFloat() / (pending + sentToday).toFloat() else 1f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── سرصفحه ──────────────────────────────────────────────────────────
        item {
            Column {
                ShimmerGoldText("پیشخوان من")
                Text(
                    "نمای هوشمند فروش و عملکرد امروز شما",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // ── ۱) تارگت ویزیتور — دونات سه‌بعدی سبز نئونی ──────────────────────
        item { TargetCard(target, todaySales, progress) }

        // ── ۲) بدهی مشتریان — بلافاصله پس از تارگت ویزیتور ──────────────────
        item { DebtorsCard(debtors) }

        // ── ۳) فاکتورهای در صف ارسال — دونات سه‌بعدی طلایی ──────────────────
        item { PendingCard(pending, sentToday, syncRatio) }

        // ── ۴) فروش هفتگی — چارت ستونی سه‌بعدی + جدول سلطنتی ────────────────
        item { WeeklyCard(weekly) }

        // ── ۵) پرفروش‌ترین‌های شما — نشان‌های رتبه سه‌بعدی ──────────────────
        item { TopSellersCard(tops) }

        // ── ۶) مشتریان نیازمند پیگیری (افت خرید) ────────────────────────────
        item {
            RoyalHeader(
                text = "مشتریان نیازمند پیگیری (افت خرید)",
                icon = Icons.Filled.TrendingUp
            )
        }

        if (followUp.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
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

// ═════════════════════════ بخش‌ها ═════════════════════════

/** کارت تارگت ویزیتور — دونات پیشرفت سه‌بعدی + آمار رنگی هماهنگ. */
@Composable
private fun TargetCard(target: Long, todaySales: Long, progress: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "تارگت ویزیتور امروز", icon = Icons.Filled.Flag)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeonDonutChart(
                    progress = progress,
                    centerValue = "${(progress * 100).toInt()}٪".toFaDigits(),
                    centerLabel = "پیشرفت",
                    size = 148.dp
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    StatRow("هدف روزانه", target.toFaPrice(), NeonPurple)
                    StatRow("فروش امروز", todaySales.toFaPrice(), NeonGreen)
                    StatRow(
                        "مانده تا هدف",
                        (target - todaySales).coerceAtLeast(0).toFaPrice(),
                        if (todaySales >= target) NeonGreen else Gold
                    )
                    StatRow(
                        "وضعیت",
                        if (progress >= 1f) "هدف محقق شد 🏆"
                        else "${(progress * 100).toInt()}٪ تکمیل".toFaDigits(),
                        if (progress >= 1f) NeonGreen else NeonPurple
                    )
                }
            }
        }
    }
}

/**
 * کارت هشدار بدهی مشتریان — جدول سلطنتی سه‌بعدی با نشان رتبه طلایی/نقره‌ای/برنزی،
 * نوار بدهی عمق‌دار سرخ و ردیف جمع طلایی.
 */
@Composable
private fun DebtorsCard(debtors: List<CustomerEntity>) {
    val maxDebt = debtors.maxOfOrNull { it.debt }?.coerceAtLeast(1L) ?: 1L
    val sum = debtors.sumOf { it.debt }
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "هشدار بدهی مشتریان", icon = Icons.Filled.WarningAmber)
            Spacer(Modifier.height(12.dp))
            if (debtors.isEmpty()) {
                Text(
                    "هیچ مشتری بدهکاری وجود ندارد 🎉",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RoyalSurfaceBrush)
                        .border(1.dp, RoyalTableBorder, RoundedCornerShape(16.dp))
                ) {
                    // سربرگ جدول
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(NeonPurple, NeonPurpleDark)))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHead("رتبه", Modifier.weight(0.6f))
                        TableHead("مشتری", Modifier.weight(1.8f))
                        TableHead("مانده بدهی", Modifier.weight(1.2f), TextAlign.End)
                    }
                    // ردیف‌های بدهکار
                    debtors.forEachIndexed { i, c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (i % 2 == 0) Color(0x10FFFFFF) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(0.6f)) { RankBadge3D(i + 1) }
                            Column(Modifier.weight(1.8f)) {
                                Text(
                                    c.name,
                                    style = MaterialTheme.typography.labelLarge
                                        .copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    c.city,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(5.dp))
                                DepthBar(
                                    fraction = c.debt.toFloat() / maxDebt,
                                    fillColors = DebtBarColors
                                )
                            }
                            Text(
                                c.debt.toFaPrice(),
                                Modifier.weight(1.2f),
                                color = DangerRed,
                                style = MaterialTheme.typography.labelMedium
                                    .copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    // ردیف جمع
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Gold.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "مجموع بدهی این مشتریان",
                            Modifier.weight(1f),
                            color = Gold,
                            style = MaterialTheme.typography.labelMedium
                                .copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            sum.toFaPrice(),
                            color = Gold,
                            style = MaterialTheme.typography.labelMedium
                                .copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/** کارت صف ارسال — دونات سه‌بعدی طلایی نسبت ارسال‌شده + آمار صف. */
@Composable
private fun PendingCard(pending: Int, sentToday: Int, syncRatio: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "فاکتورهای در صف ارسال", icon = Icons.Filled.CloudSync)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeonDonutChart(
                    progress = syncRatio,
                    centerValue = pending.toFaNumber(),
                    centerLabel = "در صف",
                    size = 96.dp,
                    centerColor = Gold,
                    progressBrush = Brush.sweepGradient(
                        colors = listOf(GoldDark, Gold, Color(0xFFFFF3D6), Gold)
                    )
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    StatRow("در انتظار ارسال", "${pending.toFaNumber()} فاکتور", Gold)
                    StatRow("ارسال‌شده امروز", "${sentToday.toFaNumber()} فاکتور", NeonGreen)
                    StatRow(
                        "وضعیت",
                        if (pending == 0) "همه ارسال شد ✅" else "در انتظار شبکه ⏳",
                        if (pending == 0) NeonGreen else Gold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "پس از اتصال، فاکتورها خودکار به سرور آتیران ارسال می‌شوند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/** کارت فروش هفتگی — چارت ستونی سه‌بعدی + جدول سلطنتی یکدست. */
@Composable
private fun WeeklyCard(weekly: List<Pair<String, Long>>) {
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "فروش هفتگی (نمودار سه‌بعدی)", icon = Icons.Filled.BarChart)
            Spacer(Modifier.height(10.dp))
            RoyalBarChart(
                data = weekly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(Modifier.height(12.dp))
            RoyalTable(data = weekly)
        }
    }
}

/**
 * کارت پرفروش‌ترین‌ها — جدول سلطنتی سه‌بعدی با نشان رتبه، نوار سهم عمق‌دار
 * بنفش→طلایی و ردیف جمع.
 */
@Composable
private fun TopSellersCard(tops: List<TopProduct>) {
    val maxTotal = tops.maxOfOrNull { it.total }?.coerceAtLeast(1.0) ?: 1.0
    val sumTotal = tops.sumOf { it.total }
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "پرفروش‌ترین‌های شما", icon = Icons.Filled.EmojiEvents)
            Spacer(Modifier.height(12.dp))
            if (tops.isEmpty()) {
                Text(
                    "پس از اولین فاکتور، پرفروش‌ها اینجا می‌درخشند ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RoyalSurfaceBrush)
                        .border(1.dp, RoyalTableBorder, RoundedCornerShape(16.dp))
                ) {
                    // سربرگ جدول
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(NeonPurple, NeonPurpleDark)))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHead("رتبه", Modifier.weight(0.6f))
                        TableHead("کالا", Modifier.weight(1.8f))
                        TableHead("تعداد فروش", Modifier.weight(1.2f), TextAlign.End)
                    }
                    tops.forEachIndexed { i, top ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (i % 2 == 0) Color(0x10FFFFFF) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(0.6f)) { RankBadge3D(i + 1) }
                            Column(Modifier.weight(1.8f)) {
                                Text(
                                    top.productName,
                                    style = MaterialTheme.typography.labelLarge
                                        .copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(5.dp))
                                DepthBar(
                                    fraction = (top.total / maxTotal).toFloat(),
                                    fillColors = ProductBarColors
                                )
                            }
                            Text(
                                "${top.total.toFaNumber()} واحد",
                                Modifier.weight(1.2f),
                                color = NeonGreen,
                                style = MaterialTheme.typography.labelMedium
                                    .copy(fontWeight = FontWeight.ExtraBold),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    // ردیف جمع
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Gold.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "مجموع فروش این کالاها",
                            Modifier.weight(1f),
                            color = Gold,
                            style = MaterialTheme.typography.labelMedium
                                .copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            "${sumTotal.toFaNumber()} واحد",
                            color = Gold,
                            style = MaterialTheme.typography.labelMedium
                                .copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/** کارت مشتری نیازمند پیگیری — نشان اعتبار، نشان VIP و نوار عمق‌دار شدت افت. */
@Composable
private fun FollowUpCard(customer: CustomerEntity, onPick: () -> Unit) {
    val drop = customer.purchaseDropPercent.coerceIn(0, 100)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (customer.isVip) Modifier.goldBorder() else Modifier)
            .clickable(onClick = onPick)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (customer.creditOk) NeonGreen else DangerRed)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            customer.name,
                            style = MaterialTheme.typography.titleSmall
                                .copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1
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
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "آخرین خرید: ${customer.lastPurchaseDaysAgo.toFaNumber()} روز پیش",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusChip(
                    text = "افت ${drop.toFaNumber()}٪",
                    color = DangerRed
                )
            }
            Spacer(Modifier.height(10.dp))
            DepthBar(
                fraction = drop / 100f,
                fillColors = DebtBarColors
            )
        }
    }
}

// ═════════════════════════ اجزای مشترک داخلی ═════════════════════════

/** ردیف آمار یکدست — برچسب خاکستری + مقدار رنگی محکم، مقیاس استاندارد تم. */
@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = color,
            textAlign = TextAlign.End
        )
    }
}

/** سلول سربرگ جدول سلطنتی — متن سفید محکم، مقیاس یکسان در همه جدول‌ها. */
@Composable
private fun TableHead(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        modifier,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
        textAlign = align
    )
}

// ═════════════════════════ توابع داده ═════════════════════════

/** ساخت داده هفتگی چارت ستونی (۷ روز اخیر؛ در نبود داده، دمو). */
private fun buildWeekly(
    invoices: List<InvoiceEntity>,
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

/** شمارش فاکتورهای ارسال‌شده امروز (سینک‌شده با سرور آتیران). */
private fun countSentToday(invoices: List<InvoiceEntity>): Int {
    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return invoices.count { it.createdAt >= todayStart && it.status == InvoiceStatus.SYNCED }
}
