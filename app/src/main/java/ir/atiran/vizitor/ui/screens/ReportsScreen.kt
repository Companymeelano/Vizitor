/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۵: گزارشات و تنظیمات (Reports & Sync)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تاریخچه فاکتورها + پیکربندی سرور (IP و پورت 1433) +
 *  مدیریت همگام‌سازی + درباره ما (لایسنس و تیم توسعه)
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.repository.ServerConfig
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.SectionTitle
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.theme.AllPalettes
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.DonutTrack
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.ThemeManager
import ir.atiran.vizitor.ui.theme.VizitorPalette
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDate
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import ir.atiran.vizitor.util.toFaTime

@Composable
fun ReportsScreen(viewModel: VizitorViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val config by viewModel.config.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val context = LocalContext.current
    var shareTarget by remember { mutableStateOf<InvoiceEntity?>(null) }

    // فرم پیکربندی سرور
    var ip by remember(config.serverIp) { mutableStateOf(config.serverIp) }
    var httpPort by remember(config.httpPort) { mutableStateOf(config.httpPort.toString()) }
    var dbPort by remember(config.dbPort) { mutableStateOf(config.dbPort.toString()) }
    var apiPath by remember(config.apiPath) { mutableStateOf(config.apiPath) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var workerUrl by remember(config.workerUrl) { mutableStateOf(config.workerUrl) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonPurple,
        unfocusedBorderColor = Color(0x33FFFFFF),
        focusedLabelColor = NeonPurple,
        cursorColor = NeonPurple
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("گزارشات و تنظیمات")
                Text(
                    "تاریخچه فروش، پیکربندی سرور و همگام‌سازی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // ── پوسته و تم — ۵ تم لاکچری (۳ تیره + ۲ روشن) ──────────────────────
        item { SectionTitle(text = "پوسته و تم (تیره و روشن لاکچری)", icon = Icons.Filled.Palette) }

        item { ThemePickerCard() }

        // ── تاریخچه فاکتورها ────────────────────────────────────────────────
        item { SectionTitle(text = "تاریخچه فاکتورها", icon = Icons.Filled.History) }

        if (invoices.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "هنوز فاکتوری صادر نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        items(invoices.take(30), key = { it.id }) { invoice ->
            InvoiceHistoryRow(invoice) { shareTarget = invoice }
        }

        // ── کارنامه عملکرد ماهانه با رتبه مدال ─────────────────────────────
        item { MonthlyPerformanceCard(invoices, viewModel.dailyTarget) }

        // ── پیکربندی سرور ───────────────────────────────────────────────────
        item { SectionTitle(text = "پیکربندی سرور آتیران", icon = Icons.Filled.Dns) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ip, onValueChange = { ip = it },
                        label = { Text("آدرس IP سرور") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = httpPort, onValueChange = { httpPort = it.filter(Char::isDigit) },
                            label = { Text("پورت وب‌سرویس") },
                            singleLine = true, colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dbPort, onValueChange = { dbPort = it.filter(Char::isDigit) },
                            label = { Text("پورت SQL Server") },
                            singleLine = true, colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = apiPath, onValueChange = { apiPath = it },
                        label = { Text("مسیر API (مثال: vizitor)") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        label = { Text("کلید API") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workerUrl, onValueChange = { workerUrl = it },
                        label = { Text("آدرس پراکسی هوش مصنوعی (Cloudflare Worker)") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row {
                        NeonPurpleButton(
                            text = "ذخیره پیکربندی",
                            onClick = {
                                viewModel.saveConfig(
                                    config.copy(
                                        serverIp = ip.trim(),
                                        httpPort = httpPort.toIntOrNull() ?: 8080,
                                        dbPort = dbPort.toIntOrNull() ?: 1433,
                                        apiPath = apiPath.trim(),
                                        apiKey = apiKey.trim(),
                                        workerUrl = workerUrl.trim()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        NeonGreenButton(text = "تست اتصال", onClick = { viewModel.testConnection() })
                    }
                }
            }
        }

        // ── مدیریت همگام‌سازی ───────────────────────────────────────────────
        item { SectionTitle(text = "مدیریت همگام‌سازی", icon = Icons.Filled.CloudSync) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("همگام‌سازی خودکار", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "سینک خودکار فاکتورها و کاتالوگ پس از اتصال به شبکه",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = config.autoSync,
                            onCheckedChange = { viewModel.saveConfig(config.copy(autoSync = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonGreen)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (config.lastSyncAt > 0)
                            "آخرین همگام‌سازی: ${config.lastSyncAt.toFaDate()} — ${config.lastSyncAt.toFaTime()}"
                        else "هنوز همگام‌سازی انجام نشده است",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    NeonGreenButton(
                        text = if (syncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون",
                        icon = Icons.Filled.Sync,
                        enabled = !syncing,
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (syncing) {
                        Spacer(Modifier.height(10.dp))
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }
                }
            }
        }

        // ── درباره ما ───────────────────────────────────────────────────────
        item { SectionTitle(text = "درباره ما", icon = Icons.Filled.Info) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "آتیران ویزیتور — نسخه ۲٫۱٫۰",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "سامانه فروش و ویزیت هوشمند ویژه مجموعه پخش آجیل و خشکبار آتیران، " +
                                "متصل به دیتابیس حسابداری (SQL Server) با معماری آفلاین-اول، " +
                                "همگام‌سازی خودکار و دستیار فروش مبتنی بر هوش مصنوعی.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Speed,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "موتور گرافیک سازگار: «${VizitorPerf.level.faLabel}» — تنظیم خودکار بر اساس قدرت گوشی شما (بدون هنگ)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "لایسنس: تجاری — تمامی حقوق برای مجموعه آتیران محفوظ است. © ۱۴۰۴",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        item { MilanoFooter() }
    }

    shareTarget?.let { inv ->
        ShareInvoiceDialog(inv, viewModel) { shareTarget = null }
    }
}

@Composable
private fun InvoiceHistoryRow(invoice: InvoiceEntity, onShare: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "فاکتور ${if (invoice.serverId != null) invoice.serverId else "#" + invoice.id}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${invoice.customerName} | ${invoice.createdAt.toFaDate()} — ${invoice.createdAt.toFaTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    invoice.finalAmount.toFaPrice(),
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonGreen
                )
                if (invoice.discount > 0) {
                    Text(
                        "کسورات: ${invoice.discount.toFaPrice()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold
                    )
                }
            }
            StatusChip(
                text = when (invoice.status) {
                    InvoiceStatus.SYNCED -> "سینک شده"
                    InvoiceStatus.PENDING -> "در صف"
                    InvoiceStatus.FAILED -> "خطا"
                },
                color = when (invoice.status) {
                    InvoiceStatus.SYNCED -> NeonGreen
                    InvoiceStatus.PENDING -> Gold
                    InvoiceStatus.FAILED -> DangerRed
                }
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "اشتراک فاکتور",
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * کارنامه عملکرد ماهانه — رتبه‌بندی مدال برنزی/نقره‌ای/طلایی.
 */
@Composable
private fun MonthlyPerformanceCard(invoices: List<InvoiceEntity>, dailyTarget: Long) {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val monthSales = invoices
        .filter { it.createdAt >= cal.timeInMillis && it.status != InvoiceStatus.FAILED }
        .sumOf { it.finalAmount }
    val target = dailyTarget * 26
    val ratio = if (target > 0) monthSales.toFloat() / target else 0f
    val rank = when {
        ratio >= 1f -> "مدال طلایی 🥇"
        ratio >= 0.5f -> "مدال نقره‌ای 🥈"
        ratio >= 0.25f -> "مدال برنزی 🥉"
        else -> "در مسیر کسب مدال 💪"
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionTitle(text = "کارنامه عملکرد ماهانه", icon = Icons.Filled.MilitaryTech)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank, style = MaterialTheme.typography.titleMedium, color = Gold)
                Spacer(Modifier.weight(1f))
                Text(
                    monthSales.toFaPrice(),
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonGreen
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DonutTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFC98A5B), Color(0xFFD7DEE9), Gold)
                            )
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "هدف ماهانه: ${target.toFaPrice()} — ${(ratio * 100).toInt().toFaNumber()}٪ محقق شده",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/** دیالوگ اشتراک فاکتور: PDF / Word / تصویر / متن. */
@Composable
private fun ShareInvoiceDialog(
    invoice: InvoiceEntity,
    viewModel: ir.atiran.vizitor.VizitorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اشتراک فاکتور ${invoice.serverId ?: ("#" + invoice.id)}") },
        text = {
            Column {
                ShareOption("📄 فایل PDF") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.sharePdf(context, invoice, items)
                    }
                }
                ShareOption("📝 فایل Word") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.shareWord(context, invoice, items)
                    }
                }
                ShareOption("🖼️ تصویر PNG") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.shareImage(context, invoice, items)
                    }
                }
                ShareOption("📨 متن پیام") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.shareText(context, invoice, items)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun ShareOption(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = NeonGreen, modifier = Modifier.fillMaxWidth())
    }
}

// ════════════════════ انتخابگر پوسته و تم (نسخه ۱٫۸٫۰) ════════════════════

/**
 * کارت انتخاب تم — پنج تم لاکچری (۳ تیره + ۲ روشن) با سواچ رنگی زنده.
 * انتخاب بلافاصله کل برنامه را بازرنگ می‌کند و ماندگار ذخیره می‌شود.
 */
@Composable
private fun ThemePickerCard() {
    val context = LocalContext.current
    val currentThemeId by ThemeManager.themeId.collectAsState()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AllPalettes.forEach { palette ->
                ThemeRow(
                    palette = palette,
                    selected = palette.id == currentThemeId,
                    onSelect = { ThemeManager.setTheme(context, palette.id) }
                )
            }
        }
    }
}

/** یک ردیف انتخاب تم: سواچ سه‌رنگ (پس‌زمینه/اصلی/طلایی) + نام + نشان انتخاب. */
@Composable
private fun ThemeRow(
    palette: VizitorPalette,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) vizitorPalette.primary.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) vizitorPalette.gold.copy(alpha = 0.8f) else vizitorPalette.glassBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // سواچ سه‌تایی رنگ اصلی تم
        Box {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.background)
                    .border(1.dp, vizitorPalette.glassBorder, CircleShape)
            )
            Box(
                Modifier
                    .offset(x = (-9).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.primary)
                    .border(1.dp, vizitorPalette.glassBorder, CircleShape)
            )
            Box(
                Modifier
                    .offset(x = (-18).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.gold)
                    .border(1.dp, vizitorPalette.glassBorder, CircleShape)
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                palette.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (palette.isDark) "تم تیره لاکچری" else "تم روشن لاکچری",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "انتخاب شده",
                tint = NeonGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
