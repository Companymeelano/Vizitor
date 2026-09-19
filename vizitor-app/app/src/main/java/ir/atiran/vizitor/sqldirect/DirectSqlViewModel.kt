/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | پل ارتباطی اپ با SQL Server (پورت ۱۴۳۳)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  این ViewModel همان کاری را می‌کند که نصب‌کنندهٔ ویندوز آماده کرده است:
 *    ۱) کارت اتصال نصب‌کننده (vizitor://c?… یا android-connect.json) را می‌خواند
 *    ۲) سرور/پورت/دیتابیس/کاربر را با رمز رمزنگاری‌شده ذخیره می‌کند
 *    ۳) با درایور JDBC به SQL Server روی پورت ۱۴۳۳ وصل می‌شود
 *    ۴) کاربر را از dbo.sys_users احراز هویت می‌کند (همان جدول کاربران ERP)
 *    ۵) ویزیتورها را با ستون‌های واقعی dbo.visitors می‌خواند
 *
 *  هیچ کاری روی رشتهٔ UI انجام نمی‌شود (همه با Dispatchers.IO داخل
 *  SqlConnectionManager) و رمز عبور هرگز در لاگ/گزارش نمی‌آید.
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

/** وضعیت کامل صفحهٔ اتصال مستقیم. */
data class DirectUiState(
    val cardText: String = "",
    val host: String = "",
    val publicHost: String = "",
    val port: String = "1433",
    val database: String = "",
    val user: String = "",
    val password: String = "",
    val usePublicHost: Boolean = false,
    val savingAllowed: Boolean = false,
    val busy: Boolean = false,
    val status: String = "",
    val statusKind: Int = 0,          // ۰=اطلاع، ۱=موفق، ۲=خطا
    val databases: List<String> = emptyList(),
    val serverInfo: String = "",
    val visitors: List<DirectVisitorRow> = emptyList(),
    val columns: List<DirectColumn> = emptyList(),
    val visitorCount: Int = 0,
    val loggedInUser: String = "",
    val loggedInUserId: Int? = null,
    val allowedCustomers: Int = 0,
    val allowedProducts: Int = 0,
    val allowedWarehouses: Int = 0,
    val visitorsOfUser: Int = 0,
)

class DirectSqlViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DirectUiState())
    val state: StateFlow<DirectUiState> = _state.asStateFlow()

    private val data get() = MeelanoDataSource(SqlConnectionManager)

    init {
        SecureDbStore.init(app)
        // اگر نصب‌کننده یا اجرای قبلی، تنظیمات را ذخیره کرده باشد، فرم پیش‌پر می‌شود
        SecureDbStore.load()?.let { s ->
            // loadAddresses: Triple(hostExternal, hostLocal, useExternal) — ترتیب دقیقاً همین است
            val (external, local, useExternal) = SecureDbStore.loadAddresses()
            _state.update {
                it.copy(
                    host = local.ifBlank { s.host },
                    publicHost = external.ifBlank { if (local.isNotBlank()) s.host else "" },
                    usePublicHost = useExternal && external.isNotBlank(),
                    port = s.port.toString(),
                    database = s.database,
                    user = s.username,
                    password = s.password,
                    status = "تنظیمات ذخیره‌شدهٔ قبلی بارگذاری شد (${s.masked()})",
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
    fun onUser(v: String) = _state.update { it.copy(user = v.trim()) }
    fun onPassword(v: String) = _state.update { it.copy(password = v) }
    fun onUsePublicHost(v: Boolean) = _state.update { it.copy(usePublicHost = v) }

    /** خواندن کارت اتصال نصب‌کننده (QR / android-connect.json / متن کارت). */
    fun applyCard() {
        val text = _state.value.cardText
        val card = ConnectCards.parse(text)
        if (card == null) {
            _state.update {
                it.copy(
                    status = "کارت خوانده نشد. متن کارت را کامل بچسبانید (باید با vizitor:// شروع شود) " +
                        "یا فایل android-connect.json را بچسبانید.",
                    statusKind = 2,
                )
            }
            return
        }
        _state.update {
            it.copy(
                host = card.hostLan.ifBlank { it.host },
                publicHost = card.hostPublic,
                usePublicHost = card.hostLan.isBlank() && card.hostPublic.isNotBlank(),
                port = card.port.toString(),
                database = card.database.ifBlank { it.database },
                user = card.login.ifBlank { it.user },
                status = "کارت اتصال خوانده شد (${card.source}) — " +
                    "نام کاربری و رمز خودتان را وارد کنید و «تست اتصال» را بزنید.",
                statusKind = 1,
            )
        }
    }

    // ── اتصال و تست ────────────────────────────────────────────────────────
    private fun currentSettings(database: String? = null) = DbSettings(
        host = activeHost(),
        port = _state.value.port.toIntOrNull() ?: 1433,
        database = database ?: _state.value.database,
        username = _state.value.user,
        password = _state.value.password,
        useEncryption = false,
        trustServerCert = true,
    )

    private fun activeHost(): String =
        if (_state.value.usePublicHost && _state.value.publicHost.isNotBlank()) _state.value.publicHost
        else _state.value.host

    /** تست اتصال + گرفتن فهرست دیتابیس‌ها (تا کاربر دیتابیس را انتخاب یا دستی تایپ کند). */
    fun testConnection() {
        val s = _state.value
        if (activeHost().isBlank() || s.user.isBlank()) {
            _state.update { it.copy(status = "آدرس سرور و نام کاربری را وارد کنید.", statusKind = 2) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال اتصال به ${activeHost()}:${s.port} …", statusKind = 0) }
            val probe = currentSettings(database = s.database.ifBlank { "master" })
            val res = SqlConnectionManager.listDatabases(probe)
            res.onSuccess { list ->
                _state.update {
                    it.copy(
                        busy = false,
                        databases = list,
                        status = "اتصال برقرار است ✅ — ${list.size} دیتابیس پیدا شد. " +
                            "دیتابیس حسابداری را انتخاب کنید یا نامش را دستی تایپ کنید.",
                        statusKind = 1,
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        busy = false,
                        status = "اتصال ناموفق ❌ — ${e.message ?: e.javaClass.simpleName}\n" +
                            "بررسی کنید SQL Server روشن است، سرویس روی پورت ${s.port} گوش می‌دهد " +
                            "و فایروال اجازه می‌دهد (نصب‌کننده این‌ها را آماده می‌کند).",
                        statusKind = 2,
                    )
                }
            }
        }
    }

    /** ذخیرهٔ رمزنگاری‌شدهٔ تنظیمات (رمز هرگز به‌صورت متن ذخیره نمی‌شود). */
    fun saveSettings() {
        val s = _state.value
        if (activeHost().isBlank() || s.database.isBlank() || s.user.isBlank()) {
            _state.update { it.copy(status = "آدرس سرور، دیتابیس و نام کاربری لازم است.", statusKind = 2) }
            return
        }
        SecureDbStore.save(currentSettings())
        SecureDbStore.saveAddresses(
            hostExternal = s.publicHost,
            hostLocal = s.host,
            useExternal = s.usePublicHost && s.publicHost.isNotBlank(),
        )
        _state.update {
            it.copy(
                status = "تنظیمات با AES-GCM + Android Keystore ذخیره شد ✅ " +
                    "(سرور ${it.host} یا ${it.publicHost.ifBlank { it.host }} — پورت ${it.port})",
                statusKind = 1,
            )
        }
    }

    /** اتصال کامل: ورود از dbo.sys_users و خواندن ویزیتورها با ستون‌های واقعی. */
    fun connectAndLoad() {
        val s = _state.value
        if (activeHost().isBlank() || s.database.isBlank() || s.user.isBlank() || s.password.isBlank()) {
            _state.update {
                it.copy(
                    status = "برای ورود، آدرس سرور، دیتابیس، نام کاربری و رمز لازم است.",
                    statusKind = 2,
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال اتصال به دیتابیس ${s.database} …", statusKind = 0) }
            val settings = currentSettings()
            if (!SqlConnectionManager.connect(settings)) {
                _state.update {
                    it.copy(
                        busy = false,
                        status = "اتصال به ${settings.masked()} برقرار نشد ❌ — پیام دقیق در وضعیت اتصال آمده است.",
                        statusKind = 2,
                    )
                }
                return@launch
            }

            // ۱) مشخصات سرور و دیتابیس
            val info = runCatching { data.databaseInfo() }.getOrNull()
            _state.update {
                it.copy(
                    serverInfo = info?.let { t -> "دیتابیس ${t.first} — نسخهٔ SQL Server ${t.second} — ${t.third} مشتری" }
                        .orEmpty(),
                )
            }

            // ۲) احراز هویت از جدول واقعی کاربران ERP
            val row = runCatching { data.login(s.user, s.password) }.getOrNull()
            if (row == null) {
                _state.update {
                    it.copy(
                        busy = false,
                        status = "کاربر «${s.user}» در دیتابیس ${settings.database} پیدا نشد یا رمز/وضعیت حساب درست نیست ❌\n" +
                            "ورود فقط با جدول dbo.sys_users انجام می‌شود (حساب باید فعال و بدون قفل باشد).",
                        statusKind = 2,
                    )
                }
                return@launch
            }
            if (row.locked) {
                _state.update {
                    it.copy(busy = false, status = "حساب «${row.username}» قفل است 🔒 — از ERP بازش کنید.", statusKind = 2)
                }
                return@launch
            }

            // ۳) ویزیتورهای همین کاربر + ستون‌های واقعی جدول
            val visitors = runCatching { VisitorRepository.visitorsForUser(row.userId, row.companyId) }
                .getOrElse { emptyList() }
            val identity = runCatching { data.visitorIdentity(row.userId, row.companyId) }.getOrNull()
            val columns = runCatching { VisitorRepository.visitorsColumns() }.getOrElse { emptyList() }
            val total = runCatching { VisitorRepository.visitorCount() }.getOrDefault(0)

            _state.update { st ->
                st.copy(
                    busy = false,
                    loggedInUser = row.username,
                    loggedInUserId = row.userId,
                    visitors = visitors,
                    visitorsOfUser = visitors.size,
                    columns = columns,
                    visitorCount = total,
                    allowedCustomers = identity?.allowedCustomers ?: 0,
                    allowedProducts = identity?.allowedProducts ?: 0,
                    allowedWarehouses = identity?.allowedWarehouses ?: 0,
                    status = "ورود موفق ✅ ${row.fullName.ifBlank { row.username }} — " +
                        "${visitors.size} ویزیتور زیرمجموعهٔ شما، ${columns.size} ستون از جدول dbo.visitors، " +
                        "$total ویزیتور در کل ERP.",
                    statusKind = 1,
                )
            }
            saveSettings()
        }
    }

    /** فقط ویزیتورها را دوباره بخوان (cache نمی‌شود؛ همیشه از سرور). */
    fun refreshVisitors() {
        val uid = _state.value.loggedInUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val visitors = runCatching { VisitorRepository.visitorsForUser(uid) }.getOrElse { emptyList() }
            _state.update {
                it.copy(busy = false, visitors = visitors, visitorsOfUser = visitors.size,
                    status = "فهرست ویزیتورها به‌روز شد (${visitors.size} رکورد)", statusKind = 1)
            }
        }
    }

    fun disconnect() {
        SqlConnectionManager.disconnect()
        _state.update {
            it.copy(
                status = "اتصال قطع شد. (تنظیمات ذخیره‌شده دست‌نخورده مانده است)",
                statusKind = 0, visitors = emptyList(), columns = emptyList(),
                loggedInUser = "", loggedInUserId = null, serverInfo = "",
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
