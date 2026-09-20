/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحهٔ «اتصال به سرور آتیران» (SQL Server — ۱۴۳۳)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  طراحی هوشمند و مرتبط با تم برنامه:
 *    • نمای پیش‌فرض «خلاصه» است: وضعیت اتصال + یک دکمهٔ اصلی + همگام‌سازی
 *    • همهٔ مراحل فنی داخل کارت‌های بازشو (آکاردئونی) هستند تا صفحه شلوغ نباشد
 *    • همهٔ رنگ‌ها/آیکن‌ها/دکمه‌ها از تم اصلی (vizitorPalette + اجزای لوکس) می‌آیند
 *  مسیر کار: کارت اتصال نصب‌کننده → سرور/دیتابیس → ورود ویزیتور → همگام‌سازی
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.SectionTitle
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.sqldirect.DirectSqlViewModel
import ir.atiran.vizitor.sqldirect.DirectUiState
import ir.atiran.vizitor.sqldirect.ServerSession
import ir.atiran.vizitor.util.toFaNumber

@Composable
fun DirectSqlScreen(
    viewModel: DirectSqlViewModel,
    onBack: () -> Unit,
    onEnterPanel: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val session by ir.atiran.vizitor.sqldirect.VizitorSession.state.collectAsState()
    val p = vizitorPalette

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── نوار بالا ───────────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت", tint = p.textPrimary)
                }
                Text(
                    "اتصال به سرور آتیران",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = p.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (state.busy || session.syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = p.gold)
                }
            }
        }

        // ── کارت خلاصهٔ وضعیت (نمای اصلی و کوتاه) ───────────────────────────
        item { StatusSummary(state, session, viewModel, onEnterPanel) }

        // ── کارت‌های مرحله‌ای (بازشو — پیش‌فرض بسته تا صفحه شلوغ نباشد) ─────
        item { ConnectionCard(state, viewModel) }
        item { ServerCard(state, viewModel) }
        item { LoginCard(state, viewModel, onEnterPanel) }

        // ── سلامت مسیر پیش‌فاکتور (اگر خوانده شده باشد) ─────────────────────
        if (state.health.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionTitle("بررسی سلامت مسیر پیش‌فاکتور", icon = Icons.Filled.CheckCircle)
                        Spacer(Modifier.height(8.dp))
                        state.health.forEach { (key, ok) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (ok) p.accent else p.danger,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(key, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ── جزئیات ویزیتورها و ستون‌ها (بازشوی اختیاری) ──────────────────────
        if (state.visitors.isNotEmpty() || state.columns.isNotEmpty()) {
            item { DetailsCard(state, viewModel) }
        }

        item { Text(
            "رمز کاربر دیتابیس فقط رمزنگاری‌شده (AES-GCM + Android Keystore) روی همین گوشی می‌ماند؛ " +
                "رمز حساب خودتان تنها با انتخاب «به‌خاطر سپردن» ذخیره می‌شود و هیچ‌گاه در متن یا گزارش چاپ نمی‌گردد.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary,
        ) }
    }
}

// ═══════════════════════ کارت خلاصه ═══════════════════════

@Composable
private fun StatusSummary(
    state: DirectUiState,
    session: ServerSession,
    viewModel: DirectSqlViewModel,
    onEnterPanel: () -> Unit,
) {
    val p = vizitorPalette
    val statusColor = when {
        state.loggedIn -> p.accent
        state.connected -> p.gold
        session.configured -> p.primary
        else -> p.danger
    }
    val statusText = when {
        state.loggedIn -> "وارد شده"
        state.connected -> "وصل به دیتابیس"
        session.configured -> "تنظیم‌شده"
        else -> "تنظیم نشده"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(statusText, statusColor)
                Spacer(Modifier.weight(1f))
                if (state.loggedIn) {
                    Text(
                        state.loggedInName.ifBlank { state.loggedInUser },
                        style = MaterialTheme.typography.labelMedium,
                        color = p.accentText,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            InfoRow(Icons.Filled.Dns, "سرور", session.serverLabel.ifBlank { "—" })
            InfoRow(Icons.Filled.Storage, "دیتابیس", state.database.ifBlank { "—" })
            InfoRow(
                Icons.Filled.Person,
                "کاربر سامانه",
                state.loggedInUser.ifBlank { "وارد نشده" },
            )
            InfoRow(
                Icons.Filled.Sync,
                "آخرین همگام‌سازی",
                if (session.lastSyncAt == 0L) "انجام نشده" else session.lastSyncSummary,
            )

            if (state.loggedIn) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip("کالا", session.productsCount)
                    MiniChip("مشتری", session.customersCount)
                    MiniChip("فاکتور", session.invoicesCount)
                    MiniChip("ویزیتور", state.visitorsOfUser)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.loggedIn) {
                    NeonPurpleButton(
                        text = if (session.configured) "ورود سریع" else "تنظیم اتصال",
                        icon = Icons.Filled.Login,
                        onClick = {
                            if (session.configured) viewModel.quickEnter() else viewModel.toggleSettings()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy,
                    )
                } else {
                    NeonPurpleButton(
                        text = "ورود به پنل",
                        icon = Icons.Filled.ExitToApp,
                        onClick = onEnterPanel,
                        modifier = Modifier.weight(1f),
                    )
                }
                NeonGreenButton(
                    text = "همگام‌سازی",
                    icon = Icons.Filled.Sync,
                    onClick = { viewModel.syncNow() },
                    modifier = Modifier.weight(1f),
                    enabled = state.loggedIn && !state.busy,
                )
            }

            if (state.status.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                MessageLine(state.status, state.statusKind)
            }
        }
    }
}

// ═══════════════════════ مرحله ۱: کارت اتصال نصب‌کننده ═══════════════════════

@Composable
private fun ConnectionCard(state: DirectUiState, viewModel: DirectSqlViewModel) {
    val p = vizitorPalette
    AccordionCard(
        title = "کارت اتصال نصب‌کننده",
        subtitle = "از فایل setup\\android-connect.txt یا QR",
        icon = Icons.Filled.QrCodeScanner,
        expanded = state.cardExpanded,
        onToggle = viewModel::toggleCard,
    ) {
        Text(
            "متن کارت را بچسبانید یا محتوای android-connect.json را وارد کنید؛ " +
                "نشانی سرور، پورت ۱۴۳۳، نام دیتابیس و نام کاربر محدود خودکار پر می‌شود (رمز در کارت نیست).",
            style = MaterialTheme.typography.bodySmall, color = p.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.cardText,
            onValueChange = viewModel::onCardText,
            label = { Text("vizitor://c?h=…&p=1433&d=…&u=…") },
            modifier = Modifier.fillMaxWidth().height(92.dp),
            maxLines = 4,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        NeonGreenButton(
            text = "خواندن کارت اتصال",
            icon = Icons.Filled.QrCodeScanner,
            onClick = { viewModel.applyCard() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ═══════════════════════ مرحله ۲: سرور و دیتابیس ═══════════════════════

@Composable
private fun ServerCard(state: DirectUiState, viewModel: DirectSqlViewModel) {
    val p = vizitorPalette
    AccordionCard(
        title = "سرور و دیتابیس",
        subtitle = if (state.database.isBlank()) "پورت ۱۴۳۳ و کاربر محدود دیتابیس"
        else "${state.database} — ${state.dbUser.ifBlank { "بدون کاربر" }}",
        icon = Icons.Filled.Dns,
        expanded = state.settingsExpanded,
        onToggle = viewModel::toggleSettings,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.host,
                onValueChange = viewModel::onHost,
                label = { Text("آدرس سرور (داخلی)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = fieldColors(),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = state.port,
                onValueChange = viewModel::onPort,
                label = { Text("پورت") },
                modifier = Modifier.width(92.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors(),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.publicHost,
            onValueChange = viewModel::onPublicHost,
            label = { Text("آی‌پی اختصاصی/اینترنتی (اختیاری)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors(),
        )
        ThemeSwitch("اتصال از بیرون شبکه", state.usePublicHost, viewModel::onUsePublicHost)
        ThemeSwitch("رمزنگاری TLS (برای سرور قدیمی خاموش کنید)", state.useEncryption, viewModel::onUseEncryption)

        Spacer(Modifier.height(4.dp))
        var dbMenu by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.database,
                onValueChange = viewModel::onDatabase,
                label = { Text("نام دیتابیس (قابل تایپ دستی)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = fieldColors(),
            )
            if (state.databases.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { dbMenu = true }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "فهرست دیتابیس‌ها", tint = p.gold)
                }
                DropdownMenu(expanded = dbMenu, onDismissRequest = { dbMenu = false }) {
                    state.databases.forEach { db ->
                        DropdownMenuItem(
                            text = { Text(db) },
                            onClick = { viewModel.onDatabase(db); dbMenu = false },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "کاربر محدود دیتابیس (همان که نصب‌کننده ساخت — نام کاربری از کارت پر می‌شود)",
            style = MaterialTheme.typography.bodySmall, color = p.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = state.dbUser,
            onValueChange = viewModel::onDbUser,
            label = { Text("نام کاربر دیتابیس") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        var showDbPass by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.dbPassword,
            onValueChange = viewModel::onDbPassword,
            label = { Text("رمز کاربر دیتابیس") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showDbPass) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                IconButton(onClick = { showDbPass = !showDbPass }) {
                    Icon(
                        if (showDbPass) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = "نمایش/پنهان رمز", tint = p.gold,
                    )
                }
            },
            colors = fieldColors(),
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonGreenButton(
                text = "تست اتصال",
                icon = Icons.Filled.Dns,
                onClick = { viewModel.fetchDatabases() },
                modifier = Modifier.weight(1f),
                enabled = !state.busy,
            )
            NeonPurpleButton(
                text = "ذخیرهٔ امن",
                icon = Icons.Filled.Key,
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { viewModel.clearStored() }, modifier = Modifier.fillMaxWidth()) {
            Text("پاک کردن تنظیمات و رمزهای ذخیره‌شده", color = p.danger, fontSize = 12.sp)
        }
    }
}

// ═══════════════════════ مرحله ۳: ورود ویزیتور ═══════════════════════

@Composable
private fun LoginCard(
    state: DirectUiState,
    viewModel: DirectSqlViewModel,
    onEnterPanel: () -> Unit,
) {
    val p = vizitorPalette
    AccordionCard(
        title = "ورود ویزیتور",
        subtitle = if (state.loggedIn) "وارد شده: ${state.loggedInUser}"
        else "با حساب خودتان در سامانهٔ آتیران",
        icon = Icons.Filled.Person,
        expanded = state.loginExpanded || !state.loggedIn,
        onToggle = viewModel::toggleLogin,
    ) {
        Text(
            "همان نام کاربری و کلمهٔ عبوری که با آن به سامانه وارد می‌شوید (جدول واقعی dbo.sys_users).",
            style = MaterialTheme.typography.bodySmall, color = p.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.erpUser,
            onValueChange = viewModel::onErpUser,
            label = { Text("نام کاربری شما") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        var showPass by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.erpPassword,
            onValueChange = viewModel::onErpPassword,
            label = { Text("کلمهٔ عبور شما") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(
                        if (showPass) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = "نمایش/پنهان رمز", tint = p.gold,
                    )
                }
            },
            colors = fieldColors(),
        )
        ThemeSwitch("به‌خاطر سپردن (ورود سریع در اجرای بعدی)", state.rememberMe, viewModel::onRememberMe)

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonPurpleButton(
                text = if (state.loggedIn) "ورود به پنل" else "ورود و همگام‌سازی",
                icon = Icons.Filled.Login,
                onClick = { if (state.loggedIn) onEnterPanel() else viewModel.loginAndLoad() },
                modifier = Modifier.weight(1f),
                enabled = !state.busy,
            )
            if (state.loggedIn) {
                OutlinedButton(onClick = { viewModel.logout() }) {
                    Text("خروج", color = p.danger, fontSize = 12.sp)
                }
            } else {
                OutlinedButton(onClick = { viewModel.connectDatabase() }, enabled = !state.busy) {
                    Text("اتصال دیتابیس", fontSize = 12.sp, color = p.accentText)
                }
            }
        }
        if (state.loggedIn) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { viewModel.refreshVisitors() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = p.gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("به‌روزرسانی ویزیتورها و ستون‌ها", fontSize = 12.sp, color = p.accentText)
            }
        }
    }
}

// ═══════════════════════ جزئیات (ویزیتورها و ستون‌ها) ═══════════════════════

@Composable
private fun DetailsCard(state: DirectUiState, viewModel: DirectSqlViewModel) {
    val p = vizitorPalette
    AccordionCard(
        title = "جزئیات دادهٔ ویزیتورها",
        subtitle = "${state.visitorsOfUser} ویزیتور • ${state.columns.size} ستون از dbo.visitors",
        icon = Icons.Filled.Storage,
        expanded = state.detailsExpanded,
        onToggle = viewModel::toggleDetails,
    ) {
        if (state.visitors.isNotEmpty()) {
            state.visitors.take(50).forEach { v ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            v.name.ifBlank { "ویزیتور #${v.rdf}" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = p.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(
                            if (v.active.equals("t", true)) "فعال" else "غیرفعال",
                            if (v.active.equals("t", true)) p.accent else p.danger,
                        )
                    }
                    val line = listOfNotNull(
                        "کد ${v.rdf}",
                        v.cell.takeIf { it.isNotBlank() }?.let { "موبایل $it" },
                        v.credit?.let { "اعتبار ${it.toFaNumber()}" },
                        v.allowedInvoicesLeft?.let { "فاکتور مجاز $it" },
                    ).joinToString(" • ")
                    if (line.isNotBlank()) {
                        Text(line, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    }
                    Text(
                        "دسترسی: ${v.allowedCustomers} مشتری • ${v.allowedProducts} کالا • ${v.allowedWarehouses} انبار",
                        style = MaterialTheme.typography.labelSmall, color = p.textSecondary,
                    )
                }
            }
        }
        if (state.columns.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "ستون‌های جدول dbo.visitors (از sys.columns):",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = p.accentText,
            )
            state.columns.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(c.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    Text(
                        c.typeName + if (c.maxLength > 0) "(${c.maxLength})" else "",
                        style = MaterialTheme.typography.labelSmall, color = p.textSecondary.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ═══════════════════════ اجزای مشترک ═══════════════════════

/** کارت بازشو (آکاردئون) همرنگ تم — برای مرتب و کوتاه نگه‌داشتن صفحه. */
@Composable
private fun AccordionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val p = vizitorPalette
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.primary.copy(alpha = 0.16f))
                        .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = p.gold, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = p.textPrimary,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = p.textSecondary)
                    }
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = p.gold,
                    modifier = Modifier.size(22.dp),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) { content() }
            }
        }
    }
}

/** سطر اطلاعات با آیکن تم‌دار. */
@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val p = vizitorPalette
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Icon(icon, contentDescription = null, tint = p.gold.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = p.textPrimary,
        )
    }
}

/** برچسب کوچک شمارشی همرنگ تم. */
@Composable
private fun MiniChip(label: String, count: Int) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(p.accent.copy(alpha = 0.12f))
            .border(1.dp, p.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            "$label ${count.toFaNumber()}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = p.accentText,
        )
    }
}

/** سطر پیام وضعیت با رنگ معنا (موفق/خطا/اطلاع). */
@Composable
private fun MessageLine(message: String, kind: Int) {
    val p = vizitorPalette
    val color = when (kind) {
        1 -> p.accent
        2 -> p.danger
        else -> p.accentText
    }
    Text(message, style = MaterialTheme.typography.bodySmall, color = color)
}

/** کلید روشن/خاموش همرنگ تم. */
@Composable
private fun ThemeSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val p = vizitorPalette
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = p.textSecondary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.onPrimary,
                checkedTrackColor = p.primary,
                uncheckedThumbColor = p.textSecondary,
                uncheckedTrackColor = p.surfaceDeep,
            ),
        )
    }
}

/** رنگ فیلدهای متنی هم‌رنگ تم (طلایی در فوکوس). */
@Composable
private fun fieldColors() = run {
    val p = vizitorPalette
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = p.gold,
        unfocusedBorderColor = p.glassBorder,
        focusedLabelColor = p.gold,
        unfocusedLabelColor = p.textSecondary,
        focusedTextColor = p.textPrimary,
        unfocusedTextColor = p.textPrimary,
        cursorColor = p.gold,
    )
}

