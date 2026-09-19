/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحهٔ «اتصال مستقیم SQL Server (۱۴۳۳)»
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  همان مسیری که نصب‌کنندهٔ ویندوز آماده کرده، در سه کارت:
 *    ۱) کارت اتصال نصب‌کننده  →  C:\Vizitor\setup\android-connect.txt / .json / QR
 *    ۲) سرور و دیتابیس         →  تست پورت ۱۴۳۳، فهرست دیتابیس‌ها، انتخاب یا تایپ دستی
 *    ۳) ورود ویزیتور           →  جدول واقعی dbo.sys_users + خواندن جدول/ستون ویزیتورها
 *  طرح و فونت، همان طرح خودِ برنامه است (هیچ تغییری در ظاهر بخش‌های دیگر ندادیم).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.atiran.vizitor.sqldirect.DirectSqlViewModel
import ir.atiran.vizitor.sqldirect.DirectUiState

@Composable
fun DirectSqlScreen(
    onBack: () -> Unit,
    viewModel: DirectSqlViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // ── نوار بالا ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت", tint = palette.onBackground)
            }
            Icon(Icons.Filled.Storage, contentDescription = null, tint = palette.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "اتصال مستقیم SQL Server (پورت ۱۴۳۳)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.onBackground,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── ۱) کارت اتصال نصب‌کننده ─────────────────────────────────────
            item {
                CardBlock("کارت اتصال نصب‌کننده", icon = Icons.Filled.Download) {
                    Text(
                        "متن کارت را از فایل setup\\android-connect.txt بخوانید و این‌جا بچسبانید؛ " +
                            "یا محتوای فایل android-connect.json را بچسبانید. " +
                            "کارت فقط نشانی سرور، پورت، نام دیتابیس و نام کاربر محدود را دارد و رمزی در آن نیست.",
                        style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.cardText,
                        onValueChange = viewModel::onCardText,
                        label = { Text("vizitor://c?h=…&p=1433&d=…&u=…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        maxLines = 4,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::applyCard, modifier = Modifier.fillMaxWidth()) {
                        Text("خواندن کارت اتصال")
                    }
                }
            }

            // ── ۲) سرور و دیتابیس + کاربر محدود SQL ─────────────────────────
            item {
                CardBlock("سرور و دیتابیس", icon = Icons.Filled.Dns) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.host,
                            onValueChange = viewModel::onHost,
                            label = { Text("آدرس سرور (IP داخلی شبکه)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.port,
                            onValueChange = viewModel::onPort,
                            label = { Text("پورت") },
                            modifier = Modifier.width(96.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.publicHost,
                        onValueChange = viewModel::onPublicHost,
                        label = { Text("آی‌پی اختصاصی/اینترنتی (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = state.usePublicHost, onCheckedChange = viewModel::onUsePublicHost)
                        Spacer(Modifier.width(6.dp))
                        Text("اتصال از بیرون شبکه (آی‌پی اختصاصی)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = state.useEncryption, onCheckedChange = viewModel::onUseEncryption)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "رمزنگاری TLS (اگر سرور قدیمی است خاموش کنید)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onSurfaceVariant,
                        )
                    }

                    var dbMenu by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.database,
                            onValueChange = viewModel::onDatabase,
                            label = { Text("نام دیتابیس حسابداری (قابل تایپ دستی)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        if (state.databases.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            OutlinedButton(onClick = { dbMenu = true }) { Text("فهرست") }
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
                        "کاربر محدود دیتابیس (همان که نصب‌کننده ساخته — از کارت اتصال پر می‌شود؛ " +
                            "رمز آن در کارت نیست و فقط یک‌بار این‌جا وارد می‌شود)",
                        style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.dbUser,
                            onValueChange = viewModel::onDbUser,
                            label = { Text("نام کاربر دیتابیس") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        )
                    }
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
                                    contentDescription = "نمایش/پنهان رمز",
                                )
                            }
                        },
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::fetchDatabases, modifier = Modifier.weight(1f)) {
                            Text("تست اتصال و فهرست دیتابیس‌ها")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::connectDatabase, modifier = Modifier.weight(1f)) {
                            Text(if (state.connected) "اتصال برقرار است ✅" else "اتصال به دیتابیس")
                        }
                        OutlinedButton(onClick = viewModel::disconnect) { Text("قطع") }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButtonLike("پاک کردن رمز ذخیره‌شده روی این گوشی") { viewModel.clearStored() }
                }
            }

            // ── ۳) ورود ویزیتور با جدول واقعی کاربران ───────────────────────
            item {
                CardBlock("ورود ویزیتور (dbo.sys_users)", icon = Icons.Filled.Person) {
                    Text(
                        "همان نام کاربری و کلمهٔ عبوری که با آن به سامانهٔ آتیران وارد می‌شوید. " +
                            "این رمز روی گوشی ذخیره نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.erpUser,
                        onValueChange = viewModel::onErpUser,
                        label = { Text("نام کاربری شما در سامانه") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    var showErpPass by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.erpPassword,
                        onValueChange = viewModel::onErpPassword,
                        label = { Text("کلمهٔ عبور شما در سامانه") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showErpPass) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            IconButton(onClick = { showErpPass = !showErpPass }) {
                                Icon(
                                    if (showErpPass) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                    contentDescription = "نمایش/پنهان رمز",
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::loginAndLoad, modifier = Modifier.weight(1f)) {
                            Text("ورود و بارگذاری ویزیتورها")
                        }
                        if (state.loggedIn) {
                            OutlinedButton(onClick = viewModel::refreshVisitors) { Text("به‌روزرسانی") }
                        }
                    }
                }
            }

            item { StatusCard(state) }

            // ── ویزیتورها ───────────────────────────────────────────────────
            if (state.visitors.isNotEmpty()) {
                item {
                    Text(
                        "ویزیتورهای زیرمجموعهٔ شما (${state.visitorsOfUser} از ${state.visitorCount} ویزیتور سامانه)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(state.visitors) { v -> VisitorCard(v) }
            }

            // ── ستون‌های واقعی جدول ─────────────────────────────────────────
            if (state.columns.isNotEmpty()) {
                item {
                    var open by remember { mutableStateOf(false) }
                    CardBlock("ستون‌های جدول dbo.visitors (${state.columns.size} ستون)", icon = Icons.Filled.Dns) {
                        Text(
                            "این فهرست مستقیم از sys.columns خوانده شده (ستون رمز خوانده و نمایش داده نمی‌شود).",
                            style = MaterialTheme.typography.bodySmall, color = palette.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButtonLike(if (open) "بستن فهرست ستون‌ها" else "نمایش فهرست ستون‌ها") { open = !open }
                        if (open) {
                            Spacer(Modifier.height(6.dp))
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                state.columns.forEach { c ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Text(c.name, modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            c.typeName + if (c.maxLength > 0) "(${c.maxLength})" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── بررسی سلامت مسیر پیش‌فاکتور (همان چیزی که نصب‌کننده آماده کرد) ──
            if (state.health.isNotEmpty()) {
                item {
                    CardBlock("بررسی سلامت مسیر پیش‌فاکتور", icon = Icons.Filled.CloudDone) {
                        state.health.forEach { (key, ok) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(if (ok) "✔" else "✖",
                                    color = if (ok) Color(0xFF2E7D32) else Color(0xFFC62828))
                                Spacer(Modifier.width(8.dp))
                                Text(key, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "دسترسی این اتصال همان چیزی است که نصب‌کننده ساخته: خواندن با db_datareader " +
                        "و ثبت پیش‌فاکتور فقط با پروسیجرهای مجاز. هیچ رمزی در متن یا گزارش چاپ نمی‌شود و " +
                        "رمز کاربر دیتابیس فقط رمزنگاری‌شده (AES-GCM + Android Keystore) روی همین گوشی می‌ماند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CardBlock(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, body: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            body()
        }
    }
}

@Composable
private fun StatusCard(state: DirectUiState) {
    val color = when (state.statusKind) {
        1 -> Color(0xFF2E7D32)
        2 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (state.statusKind == 2) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                        contentDescription = null, tint = color,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("وضعیت اتصال", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                state.status.ifBlank { "هنوز تلاشی انجام نشده است." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.statusKind == 0) MaterialTheme.colorScheme.onSurface else color,
            )
            if (state.serverInfo.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(state.serverInfo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.loggedInUserId != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "کاربر سامانه: ${state.loggedInName.ifBlank { state.loggedInUser }} " +
                        "(${state.loggedInUser} — شناسه ${state.loggedInUserId}) — " +
                        "مجاز: ${state.allowedCustomers} مشتری، ${state.allowedProducts} کالا، " +
                        "${state.allowedWarehouses} انبار",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VisitorCard(v: ir.atiran.vizitor.sqldirect.DirectVisitorRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    v.name.ifBlank { "ویزیتور #${v.rdf}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (v.active.equals("t", true)) "فعال" else "غیرفعال",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (v.active.equals("t", true)) Color(0xFF2E7D32) else Color(0xFFC62828),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("کد ویزیتور: ${v.rdf}" + (if (v.supervisor) " • سرپرست" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (v.cell.isNotBlank()) Text("موبایل: ${v.cell}", style = MaterialTheme.typography.bodySmall)
            if (v.tell.isNotBlank()) Text("تلفن: ${v.tell}", style = MaterialTheme.typography.bodySmall)
            if (v.address.isNotBlank()) Text("نشانی: ${v.address}", style = MaterialTheme.typography.bodySmall)
            val geo = listOfNotNull(
                v.city?.let { "شهر: $it" },
                v.region?.let { "منطقه: $it" },
            ).joinToString(" • ")
            if (geo.isNotBlank()) Text(geo, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            val money = listOfNotNull(
                v.credit?.let { "اعتبار: $it" },
                v.percentCash?.let { "٪نقدی: $it" },
                v.percentCheque?.let { "٪چک: $it" },
                v.allowedInvoicesLeft?.let { "فاکتور مجاز مانده: $it" },
            ).joinToString(" • ")
            if (money.isNotBlank()) Text(money, style = MaterialTheme.typography.bodySmall)
            Text(
                "دامنهٔ دسترسی: ${v.allowedCustomers} مشتری • ${v.allowedProducts} کالا • ${v.allowedWarehouses} انبار",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
