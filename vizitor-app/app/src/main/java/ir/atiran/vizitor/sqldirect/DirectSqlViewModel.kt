/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | پل بین صفحهٔ «اتصال مستقیم SQL» و لایهٔ داده
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  دو مرحلهٔ اعتبارنامه — دقیقاً مثل خودِ ERP و مثل برنامهٔ آزمایش‌شدهٔ Direct:
 *    ۱) اعتبارنامهٔ اتصال دیتابیس: کاربر محدود SQL که نصب‌کننده می‌سازد
 *       (vizitor_android) یا هر کاربر SQL دیگری → فقط برای باز کردن کانکشن.
 *    ۲) ورود ویزیتور: نام کاربری و کلمهٔ عبور خودِ ویزیتور در جدول واقعی
 *       dbo.sys_users → تعیین نقش، دسترسی و ویزیتور زیرمجموعه.
 *  هیچ‌کدام از این دو، هرگز در متن/گزارش چاپ نمی‌شوند؛ رمز مرحلهٔ ۱ فقط
 *  رمزنگاری‌شده (AES-GCM + Android Keystore) روی گوشی ذخیره می‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** وضعیت کامل صفحهٔ «اتصال مستقیم SQL». */
data class DirectUiState(
    // کارت اتصال نصب‌کننده
    val cardText: String = "",
    // سرور و دیتابیس
    val host: String = "",
    val publicHost: String = "",
    val port: String = "1433",
    val database: String = "",
    val usePublicHost: Boolean = false,
    val useEncryption: Boolean = true,
    // اعتبارنامهٔ اتصال (کاربر محدود SQL)
    val dbUser: String = "",
    val dbPassword: String = "",
    // ورود ویزیتور (dbo.sys_users)
    val erpUser: String = "",
    val erpPassword: String = "",
    // وضعیت
    val busy: Boolean = false,
    val connected: Boolean = false,
    val loggedIn: Boolean = false,
    val status: String = "",
    val statusKind: Int = 0,          // ۰=اطلاع، ۱=موفق، ۲=خطا
    val databases: List<String> = emptyList(),
    val serverInfo: String = "",
    val health: Map<String, Boolean> = emptyMap(),
    val visitors: List<DirectVisitorRow> = emptyList(),
    val columns: List<DirectColumn> = emptyList(),
    val visitorCount: Int = 0,
    val loggedInUser: String = "",
    val loggedInName: String = "",
    val loggedInUserId: Int? = null,
    val loggedInCompanyId: Int? = null,
    val allowedCustomers: Int = 0,
    val allowedProducts: Int = 0,
    val allowedWarehouses: Int = 0,
    val visitorsOfUser: Int = 0,
    // عیب‌یابی
    val diag: List<DiagRow> = emptyList(),
    val diagText: String = "",
    val driverLabel: String = "",
)

class DirectSqlViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DirectUiState())
    val state: StateFlow<DirectUiState> = _state.asStateFlow()

    private val data get() = MeelanoDataSource(SqlConnectionManager)

    init {
        SecureDbStore.init(app)
        // اگر اجرای قبلی (یا نصب‌کننده) چیزی ذخیره کرده باشد، فرم پیش‌پر می‌شود
        val saved = SecureDbStore.load()
        if (saved != null) {
            val (external, local, useExternal) = SecureDbStore.loadAddresses()
            _state.update {
                it.copy(
                    host = local.ifBlank { saved.host },
                    publicHost = external,
                    usePublicHost = useExternal && external.isNotBlank(),
                    port = saved.port.toString(),
                    database = saved.database,
                    dbUser = saved.username,
                    dbPassword = saved.password,
                    useEncryption = saved.useEncryption,
                    status = "تنظیمات ذخیره‌شدهٔ قبلی بارگذاری شد (${saved.masked()}) — " +
                        "برای دیدن ویزیتورها، نام کاربری و کلمهٔ عبور خود را وارد کنید.",
                    statusKind = 1,
                )
            }
        }
    }

    // ── فرم ────────────────────────────────────────────────────────────────
    fun onCardText(v: String) = _state.update { it.copy(cardText = v) }
    fun onHost(v: String) = _state.update { it.copy(host = v.trim()) }
    fun onPublicHost(v: String) = _state.update { it.copy(publicHost = v.trim()) }
    fun onPort(v: String) = _state.update { it.copy(port = v.filter { ch -> ch.isDigit() }) }
    fun onDatabase(v: String) = _state.update { it.copy(database = v.trim()) }
    fun onDbUser(v: String) = _state.update { it.copy(dbUser = v.trim()) }
    fun onDbPassword(v: String) = _state.update { it.copy(dbPassword = v) }
    fun onErpUser(v: String) = _state.update { it.copy(erpUser = v.trim()) }
    fun onErpPassword(v: String) = _state.update { it.copy(erpPassword = v) }
    fun onUsePublicHost(v: Boolean) = _state.update { it.copy(usePublicHost = v) }
    fun onUseEncryption(v: Boolean) = _state.update { it.copy(useEncryption = v) }

    /** خواندن کارت اتصال نصب‌کننده (متن، QR یا android-connect.json). */
    fun applyCard() {
        val card = ConnectCards.parse(_state.value.cardText)
        if (card == null) {
            _state.update {
                it.copy(
                    status = "کارت خوانده نشد. متن کارت را کامل بچسبانید (باید با vizitor:// شروع شود) " +
                        "یا محتوای فایل android-connect.json را بچسبانید.",
                    statusKind = 2,
                )
            }
            return
        }
        _state.update {
            it.copy(
                host = card.hostLan.ifBlank { it.host },
                publicHost = card.hostPublic.ifBlank { it.publicHost },
                usePublicHost = card.hostLan.isBlank() && card.hostPublic.isNotBlank(),
                port = card.port.toString(),
                database = card.database.ifBlank { it.database },
                dbUser = card.login.ifBlank { it.dbUser },
                status = "کارت اتصال خوانده شد (${card.source}) ✅ — " +
                    "اگر کارت آی‌پی اختصاصی دارد و بیرون از شبکه هستید، کلید «اتصال از بیرون» را روشن کنید، " +
                    "سپس رمز کاربر محدود دیتابیس را وارد کنید و «تست اتصال» را بزنید.",
                statusKind = 1,
            )
        }
    }

    // ── اتصال ──────────────────────────────────────────────────────────────
    private fun activeHost(): String =
        if (_state.value.usePublicHost && _state.value.publicHost.isNotBlank()) _state.value.publicHost
        else _state.value.host

    /**
     * اگر نشانی فعال از این شبکه در دسترس نباشد ولی نشانی دیگر (داخلی/اختصاصی) باز باشد،
     * خودکار همان انتخاب می‌شود. علت: کاربر بیرون از فروشگاه، آی‌پی داخلی را در فرم دارد و
     * پورت هم از بیرون «سبز» است، ولی اتصال از آی‌پی داخلی هرگز برقرار نمی‌شود.
     * خروجی: توضیح فارسی برای نمایش (یا رشتهٔ خالی اگر تغییری لازم نبود).
     */
    private fun preferReachableHost(): String {
        val st = _state.value
        val port = st.port.toIntOrNull() ?: 1433
        val current = sanitizeHost(if (st.usePublicHost) st.publicHost else st.host)
        val otherIsPublic = !st.usePublicHost
        val other = sanitizeHost(if (otherIsPublic) st.publicHost else st.host)
        if (current.isBlank() || other.isBlank() || other == current) return ""
        if (SqlDiagnostics.tcpReachable(current, port)) return ""      // نشانی فعلی سالم است
        if (!SqlDiagnostics.tcpReachable(other, port)) return ""       // هیچ‌کدام باز نیست؛ پیام خطا خودش گویاست
        _state.update { it.copy(usePublicHost = otherIsPublic) }
        return "نشانی «$current» از این شبکه در دسترس نبود؛ به‌طور خودکار از «$other» استفاده شد."
    }

    private fun settings(database: String? = null): DbSettings {
        val s = _state.value
        return DbSettings(
            host = activeHost(),
            port = s.port.toIntOrNull() ?: 1433,
            database = database ?: s.database,
            username = s.dbUser,
            password = s.dbPassword,
            useEncryption = s.useEncryption,
            trustServerCert = true,
        )
    }

    /** گام نصب‌کننده: تست اتصال + فهرست دیتابیس‌ها (برای انتخاب یا تایپ دستی). */
    fun fetchDatabases() {
        val s = _state.value
        if (activeHost().isBlank() || s.dbUser.isBlank() || s.dbPassword.isBlank()) {
            _state.update {
                it.copy(
                    status = "آدرس سرور، نام کاربری و رمز کاربر دیتابیس لازم است " +
                        "(کارت اتصال نام کاربری را پر می‌کند؛ رمز در کارت نیست).",
                    statusKind = 2,
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(busy = true, status = "در حال اتصال به ${activeHost()}:${s.port} …", statusKind = 0)
            }
            val probe = settings(database = "master")
            SqlConnectionManager.listDatabases(probe).fold(
                onSuccess = { list ->
                    _state.update {
                        it.copy(
                            busy = false,
                            databases = list,
                            status = "اتصال برقرار است ✅ — ${list.size} دیتابیس پیدا شد. " +
                                "دیتابیس حسابداری را انتخاب کنید یا نامش را دستی تایپ کنید.",
                            statusKind = 1,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            busy = false,
                            status = "اتصال ناموفق ❌ — ${e.message ?: e.javaClass.simpleName}\n" +
                                "بررسی کنید SQL Server روشن است، سرویس روی پورت ${s.port} گوش می‌دهد " +
                                "و فایروال اجازه می‌دهد (نصب‌کننده این‌ها را آماده می‌کند). " +
                                "اگر سرور قدیمی است و پیام مربوط به TLS بود، کلید «رمزنگاری TLS» را خاموش کنید.",
                            statusKind = 2,
                        )
                    }
                },
            )
        }
    }

    /** اتصال به دیتابیس + ذخیرهٔ رمزنگاری‌شدهٔ تنظیمات. */
    fun connectDatabase() {
        val s = _state.value
        if (activeHost().isBlank() || s.database.isBlank() || s.dbUser.isBlank() || s.dbPassword.isBlank()) {
            _state.update {
                it.copy(
                    status = "آدرس سرور، نام دیتابیس، نام کاربری و رمز کاربر دیتابیس لازم است.",
                    statusKind = 2,
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(busy = true, status = "در حال بررسی دسترسی و اتصال به دیتابیس ${s.database} …", statusKind = 0)
            }
            val hostNote = preferReachableHost()
            val cfg = settings()
            if (!SqlConnectionManager.connect(cfg)) {
                val message = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                _state.update {
                    it.copy(
                        busy = false,
                        connected = false,
                        status = "اتصال برقرار نشد ❌ — ${message ?: "خطای نامشخص"}\n" +
                            "نام دیتابیس را دقیق بنویسید (قابل تایپ دستی است) و دسترسی کاربر را بررسی کنید.",
                        statusKind = 2,
                    )
                }
                return@launch
            }
            saveSettings()
            val info = runCatching { data.databaseInfo() }.getOrNull()
            val health = runCatching { data.preInvoiceHealth() }.getOrElse { emptyMap() }
            _state.update {
                it.copy(
                    busy = false,
                    connected = true,
                    driverLabel = DirectSql.displayName(SqlConnectionManager.activeDriver),
                    serverInfo = info?.let { t ->
                        "دیتابیس ${t.first} — نسخهٔ SQL Server ${t.second} — ${t.third} مشتری"
                    }.orEmpty(),
                    health = health,
                    status = (if (hostNote.isNotBlank()) hostNote + "\n" else "") +
                        "اتصال برقرار و تنظیمات ذخیره شد ✅ — " +
                        "حالا نام کاربری و کلمهٔ عبور خودتان را وارد کنید و «ورود و بارگذاری ویزیتورها» را بزنید.",
                    statusKind = 1,
                )
            }
        }
    }

    /** ذخیرهٔ رمزنگاری‌شده (رمز هرگز به‌صورت متن ذخیره نمی‌شود). */
    fun saveSettings() {
        val s = _state.value
        if (activeHost().isBlank() || s.database.isBlank() || s.dbUser.isBlank()) return
        SecureDbStore.save(settings())
        SecureDbStore.saveAddresses(
            hostExternal = s.publicHost,
            hostLocal = s.host,
            useExternal = s.usePublicHost && s.publicHost.isNotBlank(),
        )
    }

    /** گام نهایی: ورود از dbo.sys_users و خواندن جدول/ستون‌های ویزیتورها. */
    fun loginAndLoad() {
        val s = _state.value
        if (s.erpUser.isBlank() || s.erpPassword.isBlank()) {
            _state.update {
                it.copy(status = "نام کاربری و کلمهٔ عبور خود را (حساب ویزیتور در سامانه) وارد کنید.", statusKind = 2)
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال ورود به سامانه …", statusKind = 0) }

            // اگر هنوز کانکشن باز نشده، اول وصل شو (بعد از بستن اپ، همان‌جا وصل می‌شود)
            if (!_state.value.connected) {
                preferReachableHost()
                val cfg = settings()
                if (!SqlConnectionManager.connect(cfg)) {
                    val message = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                    _state.update {
                        it.copy(
                            busy = false,
                            status = "اتصال به دیتابیس برقرار نشد ❌ — ${message ?: "خطای نامشخص"}\n" +
                                "بخش «سرور و دیتابیس» را کامل کنید.",
                            statusKind = 2,
                        )
                    }
                    return@launch
                }
                saveSettings()
                _state.update { it.copy(connected = true) }
            }

            val row = runCatching { data.login(s.erpUser, s.erpPassword) }.getOrNull()
            if (row == null) {
                _state.update {
                    it.copy(
                        busy = false,
                        loggedIn = false,
                        status = "نام کاربری یا کلمهٔ عبور درست نیست ❌ — " +
                            "ورود فقط با جدول واقعی کاربران سامانه (dbo.sys_users) انجام می‌شود.",
                        statusKind = 2,
                    )
                }
                return@launch
            }
            if (!row.active) {
                _state.update {
                    it.copy(
                        busy = false,
                        loggedIn = false,
                        status = "حساب «${row.username}» در سامانه غیرفعال است (sys_users.active = 0).",
                        statusKind = 2,
                    )
                }
                return@launch
            }
            if (row.locked) {
                _state.update {
                    it.copy(
                        busy = false,
                        loggedIn = false,
                        status = "حساب «${row.username}» قفل است 🔒 — از سامانه بازش کنید.",
                        statusKind = 2,
                    )
                }
                return@launch
            }

            // ویزیتورهای همین کاربر + ستون‌های واقعی جدول + دامنهٔ دسترسی
            val visitors = runCatching { VisitorRepository.visitorsForUser(row.userId, row.companyId) }
                .getOrElse { emptyList() }
            val identity = runCatching { data.visitorIdentity(row.userId, row.companyId) }.getOrNull()
            val columns = runCatching { VisitorRepository.visitorsColumns() }.getOrElse { emptyList() }
            val total = runCatching { VisitorRepository.visitorCount() }.getOrDefault(0)

            _state.update { st ->
                st.copy(
                    busy = false,
                    loggedIn = true,
                    loggedInUser = row.username,
                    loggedInName = row.fullName,
                    loggedInUserId = row.userId,
                    loggedInCompanyId = row.companyId,
                    visitors = visitors,
                    visitorsOfUser = visitors.size,
                    columns = columns,
                    visitorCount = total,
                    allowedCustomers = identity?.allowedCustomers ?: 0,
                    allowedProducts = identity?.allowedProducts ?: 0,
                    allowedWarehouses = identity?.allowedWarehouses ?: 0,
                    status = "ورود موفق ✅ ${row.fullName.ifBlank { row.username }} — " +
                        "${visitors.size} ویزیتور زیرمجموعهٔ شما، ${columns.size} ستون از جدول dbo.visitors، " +
                        "$total ویزیتور در کل سامانه.",
                    statusKind = 1,
                )
            }
        }
    }

    /** فقط ویزیتورها را دوباره بخوان (همیشه از سرور؛ بدون نمایش دادهٔ کهنه). */
    fun refreshVisitors() {
        val uid = _state.value.loggedInUserId ?: return
        val company = _state.value.loggedInCompanyId
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val visitors = runCatching { VisitorRepository.visitorsForUser(uid, company) }.getOrElse { emptyList() }
            _state.update {
                it.copy(
                    busy = false,
                    visitors = visitors,
                    visitorsOfUser = visitors.size,
                    status = "فهرست ویزیتورها به‌روز شد (${visitors.size} رکورد)",
                    statusKind = 1,
                )
            }
        }
    }

    /**
     * عیب‌یابی گام‌به‌گام: نشانی → پورت (هر دو نشانی) → درایورها → ورود واقعی →
     * دسترسی به دیتابیس حسابداری. نتیجه هم به‌صورت سطرهای ✔/✖ در UI می‌آید و هم
     * به‌صورت متن قابل‌کپی (بدون رمز) تا برای پشتیبانی فرستاده شود.
     */
    fun runDiagnostics() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال عیب‌یابی اتصال …", statusKind = 0) }
            val cfg = settings()
            val rows = ArrayList<DiagRow>()
            try {
                rows += SqlDiagnostics.run(cfg)
            } catch (t: Throwable) {
                rows += DiagRow("عیب‌یابی", false, SqlConnectionManager.describeThrowable(t))
            }

            // هر دو نشانی (داخلی و اختصاصی) از همین گوشی آزمایش می‌شوند تا معلوم شود
            // الان کدام‌یک در دسترس است — همان چیزی که «ping.eu سبز» نمی‌گوید.
            val hosts = ArrayList<String>()
            if (s.host.isNotBlank()) hosts += s.host
            if (s.publicHost.isNotBlank() && sanitizeHost(s.publicHost) != sanitizeHost(s.host)) hosts += s.publicHost
            hosts.forEach { h ->
                rows += SqlDiagnostics.tcpCheck("دسترسی به نشانی $h", sanitizeHost(h), s.port.toIntOrNull() ?: 1433)
            }

            val report = SqlDiagnostics.buildReport(cfg, rows)
            val failed = rows.count { !it.ok }
            _state.update {
                it.copy(
                    busy = false,
                    diag = rows,
                    diagText = report,
                    status = if (failed == 0)
                        "عیب‌یابی تمام شد ✅ — همهٔ ${rows.size} بررسی موفق بودند. اگر باز هم ورود نشد، دکمهٔ «ورود و بارگذاری ویزیتورها» را بزنید."
                    else
                        "عیب‌یابی تمام شد — $failed مورد از ${rows.size} بررسی ناموفق بود. متن قرمز/زرد پایین دقیقاً می‌گوید کجا گیر کرده است.",
                    statusKind = if (failed == 0) 1 else 2,
                )
            }
        }
    }

    /** پیام کوچک پس از کپی گزارش عیب‌یابی در کلیپ‌بورد. */
    fun noteCopied() = _state.update {
        it.copy(status = "گزارش عیب‌یابی کپی شد — می‌توانید آن را برای پشتیبانی بفرستید (هیچ رمزی داخل آن نیست).", statusKind = 1)
    }

    fun disconnect() {
        SqlConnectionManager.disconnect()
        _state.update {
            it.copy(
                status = "اتصال قطع شد. (تنظیمات ذخیره‌شده دست‌نخورده مانده است)",
                statusKind = 0,
                connected = false, loggedIn = false,
                visitors = emptyList(), columns = emptyList(),
                loggedInUser = "", loggedInName = "", loggedInUserId = null, loggedInCompanyId = null,
                serverInfo = "", health = emptyMap(),
            )
        }
    }

    /** پاک کردن کامل تنظیمات/رمز ذخیره‌شده روی همین گوشی. */
    fun clearStored() {
        SecureDbStore.clear()
        SqlConnectionManager.disconnect()
        _state.value = DirectUiState(status = "تنظیمات و رمز ذخیره‌شده پاک شد.", statusKind = 0)
    }
}
