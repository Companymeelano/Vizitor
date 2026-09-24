package ir.g1z4.controlpro.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ir.g1z4.controlpro.data.settings.SettingsStore
import ir.g1z4.controlpro.domain.ArmState
import ir.g1z4.controlpro.domain.CommandKind
import ir.g1z4.controlpro.domain.Device
import ir.g1z4.controlpro.domain.LinkPhase
import ir.g1z4.controlpro.domain.PowerState
import ir.g1z4.controlpro.domain.SirenState
import ir.g1z4.controlpro.domain.ThemeId
import ir.g1z4.controlpro.domain.ZoneState
import ir.g1z4.controlpro.protocol.FaFormat
import ir.g1z4.controlpro.ui.Backdrop
import ir.g1z4.controlpro.ui.Body
import ir.g1z4.controlpro.ui.GlassCard
import ir.g1z4.controlpro.ui.Headline
import ir.g1z4.controlpro.ui.Icons3d
import ir.g1z4.controlpro.ui.LocalBiometric
import ir.g1z4.controlpro.ui.Medallion
import ir.g1z4.controlpro.ui.PremiumButton
import ir.g1z4.controlpro.ui.SectionTitle
import ir.g1z4.controlpro.ui.StatusDot
import ir.g1z4.controlpro.ui.Unsupported
import ir.g1z4.controlpro.ui.theme.LocalPalette
import ir.g1z4.controlpro.ui.theme.Vazir
import ir.g1z4.controlpro.ui.vm.AppModel
import ir.g1z4.controlpro.ui.vm.writeText

@Composable
fun SplashGate() {
    Backdrop {
        Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Medallion(Icons3d.shield, "نشان برنامه", 140.dp, pulse = true)
            Spacer(Modifier.height(22.dp))
            Text("G1 Z4", color = LocalPalette.current.gold, fontFamily = Vazir, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Text("CONTROL PRO", color = LocalPalette.current.ink, fontFamily = Vazir, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Body("کنترل هوشمند سیستم امنیتی", muted = true)
        }
    }
}

@Composable
fun AuthScreen(setup: Boolean, model: AppModel) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (setup) 1 else 0) }
    var error by remember { mutableStateOf<String?>(null) }
    val bio = LocalBiometric.current
    val settings by model.settings.collectAsStateWithLifecycle()
    Backdrop {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(36.dp))
            Medallion(Icons3d.lock, "قفل", 96.dp)
            Spacer(Modifier.height(16.dp))
            Headline(if (setup && step == 1) "رمز ورود بسازید" else if (setup) "رمز را تکرار کنید" else "ورود")
            Body(if (setup) "رمز فقط روی همین گوشی و به‌صورت درهم ذخیره می‌شود." else "رمز برنامه، رمز تلفن‌کننده نیست.", muted = true)
            Spacer(Modifier.height(20.dp))
            Text(FaFormat.digits(if (step == 2) again else pin).padEnd(4, '•'), color = LocalPalette.current.gold, fontFamily = Vazir, fontSize = 28.sp)
            error?.let { Body(it) }
            Spacer(Modifier.height(12.dp))
            PinPad(onDigit = {
                error = null
                if (model.session.lockedUntil > System.currentTimeMillis()) {
                    error = "چند لحظه بعد دوباره تلاش کنید."
                    return@PinPad
                }
                if (step == 2) {
                    if (again.length < 6) again += it
                } else if (pin.length < 6) pin += it
            }, onDelete = { if (step == 2) again = again.dropLast(1) else pin = pin.dropLast(1) })
            Spacer(Modifier.height(12.dp))
            PremiumButton(if (setup && step == 1) "ادامه" else "تأیید", "تأیید رمز", {
                if (setup && step == 1) {
                    if (pin.length < 4) error = "حداقل چهار رقم." else step = 2
                } else if (setup) {
                    if (pin != again) error = "تکرار رمز یکسان نیست." else model.setupPin(pin, settings.biometric)
                } else if (!model.checkPin(pin)) error = "رمز نادرست است."
            }, Modifier.fillMaxWidth())
            if (!setup && settings.biometric) {
                Spacer(Modifier.height(8.dp))
                PremiumButton("ورود با اثر انگشت", "ورود با اثر انگشت", { bio("ورود به G1 Z4") { model.session.unlocked.value = true; model.touch() } }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "حذف", "0", "")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) Spacer(Modifier.weight(1f))
                    else PremiumButton(FaFormat.digits(key), key, { if (key == "حذف") onDelete() else onDigit(key) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(model: AppModel, nav: NavHostController, action: String?) {
    val device = active(model)
    val p = LocalPalette.current
    var confirm by remember { mutableStateOf<CommandKind?>(null) }
    val context = LocalContext.current
    val bio = LocalBiometric.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(action, device?.id) {
        when (action) {
            "arm" -> confirm = CommandKind.ARM
            "disarm" -> confirm = CommandKind.DISARM
            "status" -> confirm = CommandKind.QUERY_IO
        }
    }
    Backdrop {
        if (device == null) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Medallion(Icons3d.device, "دستگاه")
                Spacer(Modifier.height(12.dp))
                Headline("دستگاهی ثبت نشده")
                Body("شماره سیم‌کارت G1 و رمز تلفن‌کننده را وارد کنید. تا دریافت پاسخ واقعی، وضعیت نامشخص می‌ماند.")
                Spacer(Modifier.height(16.dp))
                PremiumButton("افزودن دستگاه", "افزودن دستگاه", { nav.navigate("device/new") }, Modifier.fillMaxWidth())
            }
            return@Backdrop
        }
        val snap by model.snapshot(device.id).collectAsStateWithLifecycle(ir.g1z4.controlpro.domain.Snapshot.unknown(device.id))
        val events by model.events(device.id).collectAsStateWithLifecycle(emptyList())
        val commands by model.commands(device.id).collectAsStateWithLifecycle(emptyList())
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Spacer(Modifier.height(8.dp)); Headline(device.name); Body(device.dialer.title + " · " + device.panel.title, muted = true) }
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val icon = when (snap.arm) {
                            ArmState.ARMED -> Icons3d.lock
                            ArmState.DISARMED -> Icons3d.unlock
                            ArmState.PART_SET -> Icons3d.home
                            ArmState.UNKNOWN -> Icons3d.shield
                        }
                        Medallion(icon, "وضعیت سیستم", pulse = snap.siren == SirenState.SOUNDING)
                        Column {
                            StatusDot(colorOf(snap.arm, snap.siren), labelOf(snap.arm, snap.siren))
                            Body(if (snap.armAt == null) "هنوز گزارش تأییدشده‌ای نرسیده است." else "آخرین گزارش: ${FaFormat.dateTime(snap.armAt)}", muted = true)
                        }
                    }
                }
            }
            item {
                GlassCard {
                    SectionTitle("وضعیت ارتباط")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Medallion(Icons3d.signal, "ارتباط", 52.dp)
                        Column {
                            StatusDot(linkColor(snap.link), linkLabel(snap.link))
                            Body(snap.linkDetail ?: "ارتباط پیامکی دائمی نیست. فقط پاسخ واقعی نمایش داده می‌شود.", muted = true)
                            commands.firstOrNull()?.let { c ->
                                Body(c.summary, muted = false)
                                Body(c.errorFa ?: linkLabel(c.phase), muted = true)
                            }
                            snap.lastResponseAt?.let { Body("آخرین پاسخ: ${FaFormat.dateTime(it)}", muted = true) }
                        }
                    }
                }
            }
            item {
                PremiumButton("فعال‌سازی", "فعال‌سازی سیستم", { confirm = CommandKind.ARM }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                PremiumButton("غیرفعال‌سازی", "غیرفعال‌سازی سیستم", { confirm = CommandKind.DISARM }, Modifier.fillMaxWidth(), danger = true)
                Spacer(Modifier.height(8.dp))
                GlassCard {
                    Unsupported("نیمه‌فعال فقط با دکمه C ریموت انجام می‌شود. پیامک و اپلیکیشن این حالت را پشتیبانی نمی‌کنند.")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mini(Modifier.weight(1f), Icons3d.power, "برق", when (snap.power) {
                        PowerState.MAINS_ON -> "برق شهری وصل"
                        PowerState.MAINS_OFF -> "قطع برق"
                        PowerState.UNKNOWN -> "نامشخص"
                    })
                    Mini(Modifier.weight(1f), Icons3d.battery, "باتری", snap.batteryMention ?: "سطح باتری پنل پشتیبانی نمی‌شود")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mini(Modifier.weight(1f), Icons3d.siren, "آژیر", when (snap.siren) {
                        SirenState.SOUNDING -> "گزارش آژیر"
                        SirenState.QUIET -> "گزارش قطع آژیر"
                        SirenState.UNKNOWN -> "نامشخص"
                    }, { nav.navigate("siren") })
                    Mini(Modifier.weight(1f), Icons3d.signal, "شبکه", when (snap.phoneLineCut) {
                        true -> "قطع خط تلفن گزارش شد"
                        false -> "نامشخص"
                        null -> "آنتن و اپراتور از پیامک قابل استعلام نیست"
                    }, { nav.navigate("comm") })
                }
            }
            item {
                SectionTitle("رویدادهای اخیر")
                if (events.isEmpty()) Body("هنوز رویدادی از این دستگاه دریافت نشده است.", muted = true)
                events.take(4).forEach { e ->
                    Body("${FaFormat.clock(e.at)}  ${e.title}")
                }
                Spacer(Modifier.height(8.dp))
                PremiumButton("استعلام ورودی و خروجی", "استعلام", { confirm = CommandKind.QUERY_IO }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                PremiumButton("خروجی‌ها", "خروجی‌ها", { nav.navigate("outputs") }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                PremiumButton("مرکز هشدار", "مرکز هشدار", { nav.navigate("alerts") }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(88.dp))
            }
        }
        confirm?.let { kind ->
            val title = when (kind) {
                CommandKind.ARM -> "آیا از فعال‌سازی سیستم اطمینان دارید؟"
                CommandKind.DISARM -> "آیا از غیرفعال‌سازی سیستم اطمینان دارید؟"
                else -> "استعلام به دستگاه ارسال شود؟"
            }
            AlertDialog(
                onDismissRequest = { confirm = null },
                title = { Text(title, fontFamily = Vazir) },
                text = { Text("موفقیت فقط پس از پاسخ واقعی G1 ثبت می‌شود. پالس خروجی، به‌تنهایی به معنی تغییر وضعیت پنل نیست.", fontFamily = Vazir) },
                confirmButton = {
                    PremiumButton("تأیید", "تأیید", {
                        val send = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                ask.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                            }
                            model.send(device.id, model.command(kind, star = device.panelQueryStar))
                            confirm = null
                        }
                        if (model.settings.value.biometric && kind != CommandKind.QUERY_IO) bio("تأیید فرمان") { send() } else send()
                    })
                },
                dismissButton = { PremiumButton("لغو", "لغو", { confirm = null }) }
            )
        }
    }
}

@Composable
private fun Mini(modifier: Modifier, icon: Int, title: String, value: String, onClick: (() -> Unit)? = null) {
    GlassCard(modifier.then(if (onClick != null) Modifier else Modifier)) {
        Medallion(icon, title, 44.dp)
        Spacer(Modifier.height(8.dp))
        SectionTitle(title)
        Body(value, muted = true)
        if (onClick != null) PremiumButton("جزئیات", title, onClick, Modifier.fillMaxWidth())
    }
}

@Composable
fun ZonesScreen(model: AppModel, nav: NavHostController, key: String?) {
    val device = active(model)
    Page("زون‌ها", nav) {
        if (device == null) { Body("ابتدا دستگاه را اضافه کنید."); return@Page }
        if (!device.dialer.hasLog) Unsupported("گزارش نام زون فقط با LOG و G1 Pro یا G1 Ultra می‌آید. روی این مدل، وضعیت زون نامشخص می‌ماند مگر پیامک دیگری برسد.")
        val zones by model.zones(device.id).collectAsStateWithLifecycle(emptyList())
        val selected = zones.firstOrNull { it.key == key }
        if (selected != null) {
            GlassCard {
                Medallion(Icons3d.zone, selected.localName, 72.dp)
                Headline(selected.localName)
                Body("شماره ${FaFormat.digits(selected.number)} · ${selected.kind.name}")
                StatusDot(if (selected.state == ZoneState.TRIGGERED) LocalPalette.current.alarm else LocalPalette.current.unknown, if (selected.state == ZoneState.TRIGGERED) "تحریک‌شده" else "نامشخص")
                selected.lastTriggerAt?.let { Body("آخرین تحریک: ${FaFormat.dateTime(it)}", muted = true) }
                Unsupported("بایپس زون از پیامک G1 پشتیبانی نمی‌شود.")
                var name by remember(selected.key) { mutableStateOf(selected.localName) }
                Field(name) { name = it }
                PremiumButton("ذخیره نام محلی", "ذخیره نام", { model.renameZone(device.id, selected.key, name) }, Modifier.fillMaxWidth())
                Body("این نام فقط در گوشی است و به دستگاه ارسال نمی‌شود.", muted = true)
            }
        } else {
            if (zones.isEmpty()) Body("زون‌ها پس از ذخیره دستگاه ساخته می‌شوند و تا پیامک واقعی نامشخص‌اند.", muted = true)
            zones.forEach { z ->
                GlassCard(Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Medallion(Icons3d.zone, z.localName, 48.dp)
                        Column(Modifier.weight(1f)) {
                            Body(z.localName)
                            Body(if (z.state == ZoneState.TRIGGERED) "تحریک‌شده" else "نامشخص", muted = true)
                        }
                        PremiumButton("جزئیات", z.localName, { nav.navigate("zone/${z.key}") })
                    }
                }
            }
        }
    }
}

@Composable
fun EventsScreen(model: AppModel, nav: NavHostController) {
    val device = active(model)
    Page("تاریخچه", nav) {
        Medallion(Icons3d.reports, "گزارش‌ها", 56.dp)
        Spacer(Modifier.height(8.dp))
        if (device == null) return@Page
        val events by model.events(device.id).collectAsStateWithLifecycle(emptyList())
        if (events.isEmpty()) Body("رویدادی ثبت نشده. حافظه ۲۰۰تایی خود G1 با فرمان منتشرشده قابل دانلود نیست.", muted = true)
        var day = ""
        events.forEach { e ->
            val title = FaFormat.dayTitle(e.at)
            if (title != day) { day = title; SectionTitle(title) }
            GlassCard(Modifier.padding(bottom = 8.dp)) {
                Body("${FaFormat.clock(e.at)}  ${e.title}")
                Body(e.detail, muted = true)
                if (e.synthetic) Body("آزمایشی", muted = true)
            }
        }
    }
}

@Composable
fun AlertsScreen(model: AppModel, nav: NavHostController) {
    var filter by remember { mutableStateOf("همه") }
    val alerts by model.alerts.collectAsStateWithLifecycle()
    Page("مرکز هشدار", nav) {
        Medallion(Icons3d.bell, "هشدار", 56.dp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("همه", "بحرانی", "هشدار", "اطلاعات").forEach { f ->
                PremiumButton(f, f, { filter = f })
            }
        }
        Spacer(Modifier.height(8.dp))
        val shown = alerts.filter {
            filter == "همه" || (filter == "بحرانی" && it.severity.name == "CRITICAL") || (filter == "هشدار" && it.severity.name == "WARNING") || (filter == "اطلاعات" && it.severity.name == "INFO")
        }
        if (shown.isEmpty()) Body("هشداری نیست.", muted = true)
        shown.forEach { a ->
            GlassCard(Modifier.padding(bottom = 8.dp)) {
                Body(a.title)
                Body(FaFormat.dateTime(a.at), muted = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PremiumButton(if (a.read) "خوانده شد" else "خواندم", "خوانده‌شده", { model.markRead(a.id) })
                    PremiumButton("حذف", "حذف هشدار", { model.deleteAlert(a.id) }, danger = true)
                }
            }
        }
        PremiumButton("پاک کردن همه", "پاک کردن", { model.clearAlerts() }, Modifier.fillMaxWidth(), danger = true)
    }
}

@Composable
fun DevicesScreen(model: AppModel, nav: NavHostController, editId: String?) {
    val devices by model.devices.collectAsStateWithLifecycle()
    if (editId == null) {
        Page("دستگاه‌ها", nav, showBack = false) {
            devices.forEach { d ->
                GlassCard(Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Medallion(Icons3d.device, d.name, 52.dp)
                        Column(Modifier.weight(1f)) {
                            Body(d.name)
                            Body(FaFormat.digits(d.phoneRaw), muted = true)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PremiumButton("انتخاب", "انتخاب دستگاه", { model.setActive(d.id) }, Modifier.fillMaxWidth())
                    PremiumButton("ویرایش", "ویرایش", { nav.navigate("device/${d.id}") }, Modifier.fillMaxWidth())
                }
            }
            PremiumButton("افزودن دستگاه", "افزودن", { nav.navigate("device/new") }, Modifier.fillMaxWidth())
        }
    } else DeviceEditor(model, nav, devices.firstOrNull { it.id == editId })
}

@Composable
private fun DeviceEditor(model: AppModel, nav: NavHostController, existing: Device?) {
    var name by remember { mutableStateOf(existing?.name ?: "دزدگیر منزل") }
    var phone by remember { mutableStateOf(existing?.phoneRaw ?: "") }
    var password by remember { mutableStateOf("") }
    var panel by remember { mutableStateOf(existing?.panel?.name ?: "Z4_ULTRA") }
    var dialer by remember { mutableStateOf(existing?.dialer?.name ?: "G1_ULTRA") }
    var wiring by remember { mutableStateOf(existing?.wiring?.name ?: "STANDARD_A2_D3") }
    var recharge by remember { mutableStateOf(existing?.rechargeVariant ?: "irancell_hash") }
    var panelQuery by remember { mutableStateOf(if (existing?.optionalPanelQuery == true) "1" else "0") }
    var error by remember { mutableStateOf<String?>(null) }
    Page(if (existing == null) "دستگاه جدید" else "ویرایش", nav) {
        Body("نام محلی", muted = true)
        Field(name) { name = it }
        Body("شماره سیم‌کارت G1", muted = true)
        Field(phone) { phone = it }
        Body(if (existing == null) "رمز چهاررقمی تلفن‌کننده" else "رمز جدید؛ خالی بماند اگر عوض نمی‌شود", muted = true)
        Field(password, secret = true) { password = it }
        Body("رمز کارخانه ۱۲۳۴ است، فقط اگر روی دستگاه عوض نشده باشد.", muted = true)
        PremiumButton("پنل: ${panelLabel(panel)}", "مدل پنل", { panel = cycle(panel, listOf("Z4", "Z4_ULTRA")) }, Modifier.fillMaxWidth())
        PremiumButton("تلفن‌کننده: ${dialerLabel(dialer)}", "مدل تلفن‌کننده", {
            dialer = cycle(dialer, listOf("G1", "G1_PLUS", "G1_PRO", "G1_ULTRA"))
        }, Modifier.fillMaxWidth())
        PremiumButton(if (wiring == "STANDARD_A2_D3") "سیم‌بندی استاندارد A به خروجی ۲ و D به خروجی ۳" else "سیم‌بندی سفارشی", "سیم‌بندی", {
            wiring = cycle(wiring, listOf("STANDARD_A2_D3", "CUSTOM"))
        }, Modifier.fillMaxWidth())
        Body("در سیم‌بندی استاندارد، فعال‌سازی پالس خروجی ۲ و غیرفعال‌سازی پالس خروجی ۳ است.", muted = true)
        PremiumButton("فرمول شارژ: ${rechargeLabel(recharge)}", "فرمول شارژ", {
            recharge = cycle(recharge, listOf("irancell_hash", "irancell_star", "mci_hash", "mci_star"))
        }, Modifier.fillMaxWidth())
        Body("هیچ‌کدام از # یا * برای افزایش اعتبار هنوز با منبع رسمی تأیید نشده است.", muted = true)
        PremiumButton(if (panelQuery == "1") "استعلام اختیاری *22 روشن" else "استعلام اختیاری *22 خاموش", "استعلام اختیاری", {
            panelQuery = if (panelQuery == "1") "0" else "1"
        }, Modifier.fillMaxWidth())
        Body("*22 در جدول رسمی پیامک G1 نیست و فرمان پیش‌فرض وضعیت نیست.", muted = true)
        error?.let { Body(it) }
        PremiumButton("ذخیره", "ذخیره دستگاه", {
            model.saveDevice(existing?.id, mapOf(
                "name" to name,
                "phone" to phone,
                "password" to password,
                "panel" to panel,
                "dialer" to dialer,
                "wiring" to wiring,
                "recharge" to recharge,
                "operator" to if (recharge.startsWith("mci")) "MCI" else "IRANCELL",
                "ussd" to if (recharge.startsWith("mci")) "*140*11#" else "*141*1#",
                "panelQuery" to panelQuery,
                "panelStar" to if (existing?.panelQueryStar == true) "1" else "0",
                "arm" to (existing?.armOutput ?: 2).toString(),
                "disarm" to (existing?.disarmOutput ?: 3).toString(),
                "wired" to (existing?.wiredZones ?: 4).toString(),
                "wireless" to (existing?.wireless ?: 0).toString(),
                "expander" to (existing?.expanderZones ?: 0).toString(),
                "timeout" to (existing?.timeoutSec ?: 90).toString(),
                "retry" to if (existing?.queryRetry == false) "0" else "1",
                "out4" to if (dialer == "G1_PRO" || dialer == "G1_ULTRA") "1" else "0"
            )) {
                it.onSuccess { nav.popBackStack() }.onFailure { e -> error = e.message }
            }
        }, Modifier.fillMaxWidth())
        if (existing != null) PremiumButton("حذف دستگاه", "حذف", { model.deleteDevice(existing.id); nav.popBackStack() }, Modifier.fillMaxWidth(), danger = true)
    }
}

private fun cycle(current: String, options: List<String>): String {
    val index = options.indexOf(current).let { if (it < 0) 0 else it }
    return options[(index + 1) % options.size]
}

private fun panelLabel(id: String) = if (id == "Z4") "Classic Z4" else "Classic Z4 Ultra"
private fun dialerLabel(id: String) = when (id) {
    "G1" -> "G1"
    "G1_PLUS" -> "G1+"
    "G1_PRO" -> "G1 Pro"
    else -> "G1 Ultra"
}
private fun rechargeLabel(id: String) = when (id) {
    "irancell_star" -> "ایرانسل با *"
    "mci_hash" -> "همراه اول با #"
    "mci_star" -> "همراه اول با *"
    else -> "ایرانسل با #"
}

@Composable
fun OutputsScreen(model: AppModel, nav: NavHostController) {
    val device = active(model) ?: return
    var pending by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val outputs by model.outputs(device.id).collectAsStateWithLifecycle(emptyList())
    Page("خروجی‌ها", nav) {
        Medallion(Icons3d.output, "خروجی", 56.dp)
        Spacer(Modifier.height(8.dp))
        Body("خروجی ۲ و ۳ در سیم‌بندی استاندارد برای فعال و غیرفعال کردن پنل رزرو شده‌اند.", muted = true)
        outputs.forEach { o ->
            GlassCard(Modifier.padding(vertical = 6.dp)) {
                Medallion(Icons3d.power, o.localName, 48.dp)
                Body(o.localName)
                o.reservedFor?.let { Body("رزرو: $it", muted = true) }
                Body(if (!o.stateKnown) "آخرین گزارش: نامشخص" else if (o.on == true) "آخرین گزارش: روشن" else "آخرین گزارش: خاموش", muted = true)
                if (o.reservedFor != null) Unsupported("کنترل مستقیم این خروجی همان فرمان پنل است. از دکمه‌های فعال‌سازی استفاده کنید.")
                else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumButton("روشن", "روشن کردن خروجی", { pending = o.number to true })
                        PremiumButton("خاموش", "خاموش کردن خروجی", { pending = o.number to false }, danger = true)
                    }
                }
            }
        }
        pending?.let { (n, on) ->
            AlertDialog(
                onDismissRequest = { pending = null },
                title = { Text("تغییر خروجی ${FaFormat.digits(n)}؟", fontFamily = Vazir) },
                text = { Text("اگر خروجی لحظه‌ای باشد، حدود ۱٫۵ ثانیه فعال می‌شود.", fontFamily = Vazir) },
                confirmButton = { PremiumButton("تأیید", "تأیید", { model.send(device.id, model.command(if (on) CommandKind.OUTPUT_ON else CommandKind.OUTPUT_OFF, n)); pending = null }) },
                dismissButton = { PremiumButton("لغو", "لغو", { pending = null }) }
            )
        }
    }
}

@Composable
fun SirenScreen(model: AppModel, nav: NavHostController) {
    val device = active(model) ?: return
    val snap by model.snapshot(device.id).collectAsStateWithLifecycle(ir.g1z4.controlpro.domain.Snapshot.unknown(device.id))
    var ask by remember { mutableStateOf(false) }
    Page("آژیر", nav) {
        Medallion(Icons3d.siren, "آژیر", 88.dp, pulse = snap.siren == SirenState.SOUNDING)
        Spacer(Modifier.height(8.dp))
        Body(when (snap.siren) {
            SirenState.SOUNDING -> "آخرین گزارش: آژیر"
            SirenState.QUIET -> "آخرین گزارش: قطع آژیر"
            SirenState.UNKNOWN -> "وضعیت آژیر نامشخص است."
        })
        Unsupported("شروع آژیر از پیامک در پروتکل منتشرشده وجود ندارد. فشار دوباره دکمه A ریموت آژیر لحظه‌ای است و از اپلیکیشن ساخته نمی‌شود.")
        Spacer(Modifier.height(8.dp))
        Body("قطع آژیر جداگانه نیست. غیرفعال‌سازی، همان مسیر دکمه B ریموت است و سیستم را هم خاموش می‌کند.")
        PremiumButton("غیرفعال‌سازی برای قطع آلارم", "غیرفعال‌سازی", { ask = true }, Modifier.fillMaxWidth(), danger = true)
        Spacer(Modifier.height(8.dp))
        PremiumButton("توقف شماره‌گیری", "توقف شماره‌گیری", { model.send(device.id, model.command(CommandKind.STOP_DIAL)) }, Modifier.fillMaxWidth())
        Body("توقف شماره‌گیری آژیر را قطع نمی‌کند.", muted = true)
        if (ask) AlertDialog(onDismissRequest = { ask = false }, confirmButton = { PremiumButton("تأیید", "تأیید", { model.send(device.id, model.command(CommandKind.DISARM)); ask = false }) }, dismissButton = { PremiumButton("لغو", "لغو", { ask = false }) }, title = { Text("غیرفعال شود؟", fontFamily = Vazir) }, text = { Text("این فرمان خروجی غیرفعال‌سازی را پالس می‌کند.", fontFamily = Vazir) })
    }
}

@Composable
fun RemotesScreen(model: AppModel, nav: NavHostController) {
    val device = active(model) ?: return
    val remotes by model.remotes(device.id).collectAsStateWithLifecycle(emptyList())
    Page("ریموت‌ها", nav) {
        Unsupported("تعریف و حذف ریموت فقط با دکمه LRN روی پنل انجام می‌شود. فرمان پیامکی برای آن منتشر نشده است.")
        Body("جایگاه‌ها وضعیت ست‌شدن را نشان نمی‌دهند، مگر نام ریموت در یک پیامک واقعی دیده شود.", muted = true)
        remotes.forEach { r ->
            GlassCard(Modifier.padding(vertical = 4.dp)) {
                Body("جایگاه ${FaFormat.digits(r.slot)} · ${r.localName}")
                Body(if (r.lastSeenAt == null) "آخرین مشاهده: نامشخص" else "دیده شده در پیامک: ${FaFormat.dateTime(r.lastSeenAt)}", muted = true)
            }
        }
        Body("نام‌گذاری روی سخت‌افزار فقط با Classic SETUP است. نام این صفحه محلی است.", muted = true)
    }
}

@Composable
fun CommScreen(model: AppModel, nav: NavHostController) {
    val device = active(model) ?: return
    var code by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf(false) }
    var store by remember { mutableStateOf(false) }
    Page("تنظیمات ارتباطی", nav) {
        Unsupported("تغییر شماره‌های مدیر، متن تحریک و نام زون روی دستگاه از این برنامه ارسال نمی‌شود. آن‌ها روی صفحه‌کلید G1 یا Classic SETUP هستند.")
        PremiumButton("تست ارتباط / استعلام", "تست ارتباط", { model.send(device.id, model.command(CommandKind.QUERY_IO)); model.scan(device.id) }, Modifier.fillMaxWidth())
        PremiumButton("درخواست شارژ", "شارژ", { model.send(device.id, model.command(CommandKind.QUERY_CREDIT)) }, Modifier.fillMaxWidth())
        PremiumButton("ذخیره فرمول موجودی", "ذخیره فرمول", { store = true }, Modifier.fillMaxWidth())
        Body("فرمول فعلی: ${FaFormat.digits(device.balanceUssd)}. متن تأیید این فرمان منتشر نشده و بدون پاسخ مطابق، موفق ثبت نمی‌شود.", muted = true)
        Field(code) { code = it.filter { ch -> ch.isDigit() } }
        PremiumButton("افزایش اعتبار", "افزایش اعتبار", { ask = true }, Modifier.fillMaxWidth())
        if (device.optionalPanelQuery) {
            PremiumButton("استعلام اختیاری *22", "استعلام اختیاری", { model.send(device.id, model.command(CommandKind.QUERY_PANEL, star = device.panelQueryStar)) }, Modifier.fillMaxWidth())
        }
        Body("دستور *22 در جدول رسمی G1 نیست و فرمان پیش‌فرض وضعیت نیست.", muted = true)
        if (ask) AlertDialog(onDismissRequest = { ask = false }, confirmButton = { PremiumButton("تأیید", "تأیید", { model.send(device.id, model.command(CommandKind.RECHARGE, extra = code, variant = device.rechargeVariant)); ask = false }) }, dismissButton = { PremiumButton("لغو", "لغو", { ask = false }) }, title = { Text("کد شارژ ارسال شود؟", fontFamily = Vazir) }, text = { Text("کد در گزارش‌ها پوشیده می‌شود. موفقیت فقط با پاسخ واقعی.", fontFamily = Vazir) })
        if (store) AlertDialog(onDismissRequest = { store = false }, confirmButton = { PremiumButton("تأیید", "تأیید", { model.send(device.id, model.command(CommandKind.STORE_USSD, extra = device.balanceUssd)); store = false }) }, dismissButton = { PremiumButton("لغو", "لغو", { store = false }) }, title = { Text("فرمول موجودی ذخیره شود؟", fontFamily = Vazir) }, text = { Text(FaFormat.digits(device.balanceUssd), fontFamily = Vazir) })
    }
}

@Composable
fun SettingsScreen(model: AppModel, nav: NavHostController) {
    val settings by model.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var taps by remember { mutableStateOf(0) }
    var devAsk by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) writeText(context, uri, model.exportJson())
    }
    Page("تنظیمات", nav, showBack = false) {
        Medallion(Icons3d.settings, "تنظیمات", 56.dp)
        Spacer(Modifier.height(8.dp))
        SectionTitle("ظاهر")
        ThemeId.entries.forEach { theme ->
            PremiumButton(themeLabel(theme), theme.name, {
                model.updateSetting { model.edit { it[SettingsStore.Keys.theme] = theme.name } }
            }, Modifier.fillMaxWidth().padding(bottom = 6.dp))
        }
        SectionTitle("امنیت")
        PremiumButton(if (settings.biometric) "اثر انگشت روشن" else "اثر انگشت خاموش", "بیومتریک", {
            model.updateSetting { model.edit { it[SettingsStore.Keys.biometric] = !settings.biometric } }
        }, Modifier.fillMaxWidth())
        PremiumButton(if (settings.sounds) "صدا روشن" else "صدا خاموش", "صدا", {
            model.updateSetting { model.edit { it[SettingsStore.Keys.sounds] = !settings.sounds } }
        }, Modifier.fillMaxWidth())
        SectionTitle("اعلان‌ها")
        Toggle(model, "آلارم", settings.notifyAlarm, SettingsStore.Keys.nAlarm)
        Toggle(model, "زون", settings.notifyZone, SettingsStore.Keys.nZone)
        Toggle(model, "برق", settings.notifyPower, SettingsStore.Keys.nPower)
        Toggle(model, "باتری", settings.notifyBattery, SettingsStore.Keys.nBattery)
        Toggle(model, "ارتباط", settings.notifyConnection, SettingsStore.Keys.nConn)
        Toggle(model, "فعال شدن", settings.notifyArm, SettingsStore.Keys.nArm)
        PremiumButton("خروجی‌ها", "خروجی‌ها", { nav.navigate("outputs") }, Modifier.fillMaxWidth())
        PremiumButton("ریموت‌ها", "ریموت‌ها", { nav.navigate("remotes") }, Modifier.fillMaxWidth())
        PremiumButton("ارتباط G1", "ارتباط", { nav.navigate("comm") }, Modifier.fillMaxWidth())
        PremiumButton("مرکز هشدار", "هشدارها", { nav.navigate("alerts") }, Modifier.fillMaxWidth())
        PremiumButton("پروتکل", "پروتکل", { nav.navigate("protocol") }, Modifier.fillMaxWidth())
        PremiumButton("خروجی گزارش محلی", "خروجی", { export.launch("g1z4-backup.json") }, Modifier.fillMaxWidth())
        Body("نسخه ۱.۰.۰", muted = true)
        PremiumButton("درباره", "درباره", {
            taps += 1
            if (taps >= 7) devAsk = true
        }, Modifier.fillMaxWidth())
        if (devAsk && ir.g1z4.controlpro.BuildConfig.DEBUG) AlertDialog(onDismissRequest = { devAsk = false }, confirmButton = {
            PremiumButton("فعال‌سازی آزمایشی", "آزمایش", {
                model.updateSetting { model.edit { it[SettingsStore.Keys.developer] = !settings.developer } }
                devAsk = false
            })
        }, dismissButton = { PremiumButton("لغو", "لغو", { devAsk = false }) }, title = { Text("حالت آزمایشی", fontFamily = Vazir) }, text = { Text("پاسخ‌ها ساختگی‌اند و به‌عنوان سخت‌افزار واقعی ثبت نمی‌شوند.", fontFamily = Vazir) })
    }
}

@Composable
private fun Toggle(model: AppModel, label: String, value: Boolean, key: androidx.datastore.preferences.core.Preferences.Key<Boolean>) {
    PremiumButton("$label: ${if (value) "روشن" else "خاموش"}", label, {
        model.updateSetting { model.edit { it[key] = !value } }
    }, Modifier.fillMaxWidth().padding(bottom = 6.dp))
}

@Composable
fun ProtocolScreen(nav: NavHostController) {
    Page("پروتکل", nav) {
        Body("فرمان‌ها فقط موارد مستند G1 هستند: 00 توقف شماره‌گیری، 01 وضعیت ورودی/خروجی، 11 تا 40 کنترل خروجی، 80 شارژ، 81 فرمول، 99 افزایش اعتبار.")
        Spacer(Modifier.height(8.dp))
        Body("فعال‌سازی یعنی پالس خروجی ۲ اگر سیم A به OUT2 وصل باشد. غیرفعال‌سازی پالس خروجی ۳ است. نیمه‌فعال، بایپس، یادگیری ریموت و دانلود ۲۰۰ رویداد پشتیبانی نمی‌شود.")
        Body("این برنامه محصول رسمی شرکت کلاسیک نیست.", muted = true)
    }
}

@Composable
private fun Page(title: String, nav: NavHostController, showBack: Boolean = true, content: @Composable () -> Unit) {
    Backdrop {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 24.dp)) {
            if (showBack) PremiumButton("بازگشت", "بازگشت", { nav.popBackStack() })
            Spacer(Modifier.height(8.dp))
            Headline(title)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, secret: Boolean = false) {
    val p = LocalPalette.current
    BasicTextField(
        value,
        onChange,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = androidx.compose.ui.text.TextStyle(color = p.ink, fontFamily = Vazir, fontSize = 16.sp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(p.surface, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(14.dp)
    )
}

@Composable
private fun active(model: AppModel): Device? {
    val devices by model.devices.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    return devices.firstOrNull { it.id == settings.activeDeviceId } ?: devices.firstOrNull()
}

private fun labelOf(arm: ArmState, siren: SirenState): String = when {
    siren == SirenState.SOUNDING -> "هشدار"
    arm == ArmState.ARMED -> "فعال"
    arm == ArmState.DISARMED -> "غیرفعال"
    arm == ArmState.PART_SET -> "نیمه‌فعال"
    else -> "نامشخص"
}

@Composable
private fun colorOf(arm: ArmState, siren: SirenState): Color {
    val p = LocalPalette.current
    return when {
        siren == SirenState.SOUNDING -> p.alarm
        arm == ArmState.ARMED -> p.ok
        arm == ArmState.DISARMED -> p.unknown
        arm == ArmState.PART_SET -> p.info
        else -> p.unknown
    }
}

@Composable
private fun linkColor(phase: LinkPhase): Color {
    val p = LocalPalette.current
    return when (phase) {
        LinkPhase.CONFIRMED -> p.ok
        LinkPhase.WAITING, LinkPhase.SENDING, LinkPhase.QUEUED -> p.warn
        LinkPhase.FAILED, LinkPhase.REJECTED, LinkPhase.PERMISSION_DENIED, LinkPhase.UNCONFIRMED -> p.alarm
        LinkPhase.IDLE -> p.unknown
    }
}

private fun linkLabel(phase: LinkPhase) = when (phase) {
    LinkPhase.CONFIRMED -> "پاسخ دریافت شد"
    LinkPhase.WAITING, LinkPhase.SENDING, LinkPhase.QUEUED -> "در انتظار پاسخ"
    LinkPhase.UNCONFIRMED -> "تأیید نشد"
    LinkPhase.REJECTED -> "فرمان رد شد"
    LinkPhase.FAILED -> "ارتباط ناموفق"
    LinkPhase.PERMISSION_DENIED -> "مجوز پیامک نیست"
    LinkPhase.IDLE -> "هنوز استعلام نشده"
}

private fun themeLabel(id: ThemeId) = when (id) {
    ThemeId.LUXURY_DARK -> "Luxury Dark"
    ThemeId.MIDNIGHT -> "Midnight Security"
    ThemeId.LUXURY_LIGHT -> "Luxury Light"
    ThemeId.PURE_MINIMAL -> "Pure Minimal"
}
