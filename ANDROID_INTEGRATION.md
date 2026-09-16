# اتصال هوشمند برنامهٔ اندروید به سرور (راهنمای یکپارچگی)

این سند، راهنمای کامل ساخت **بخش «اتصال به سرور» در تنظیمات برنامهٔ اندروید** است — به‌گونه‌ای که کاربر
**با یک یا دو کلیک** (فقط وارد کردن آدرس سرور) به سرور Vizitor — اعم از اینکه API مستقیم، از طریق **IIS**،
و دیتابیس **SQL Server** باشد — وصل شود و **داده‌های لحظه‌ای** دریافت کند.

> نکتهٔ معماری: از دید برنامهٔ اندروید، سرور چه مستقیم باشد چه از طریق IIS، **یک آدرس واحد** است:
> `http://آدرس-سرور/api` (یا با پورت). IIS فقط یک پروکسی معکوس شفاف در مسیر است؛ API و دیتابیس
> (SQL Server) پشت آن دقیقاً مثل حالت مستقیم کار می‌کنند.

---

## ۱. چرا «چند کلیک» می‌شود؟

سرور Vizitor **خودپیکربند** است: اپ فقط آدرس را می‌پرسد و همهٔ چیزها را از سرور می‌خواند:

| چیزی که اپ باید بداند | منبع |
|---|---|
| آدرس کامل API | `api_url` در پاسخ `/api/config` |
| نسخهٔ سرور | `version` |
| فعال است یا نه؟ | `activated` |
| لحظه‌ای (SSE) را پشتیبانی می‌کند؟ | `realtime` + `features` شامل `"sse"` |
| حالت دیتابیس | `db_engine` + `/api/health` |

### پاسخ واقعی `GET /api/config` (بدون احراز)

```json
{
  "name": "Vizitor",
  "version": "1.1.0",
  "api_url": "http://217.20.30.40/api",
  "activated": false,
  "db_engine": "sqlserver",
  "server_time": "2026-09-16 01:40:15",
  "realtime": true,
  "sse_path": "/api/events",
  "features": ["self_config", "sse", "incremental_sync"]
}
```

**قانون اتصال هوشمند:** اپ آدرس ورودی کاربر را می‌گیرد و به‌ترتیب این‌ها را امتحان می‌کند
(هر کدام با مهلت ۴ ثانیه):

```
http://<host>/api/config
http://<host>:8080/api/config        (اختیاری — اگر نصب روی پورت غیرمعمول باشد)
https://<host>/api/config            (فقط اگر کاربر HTTPS را انتخاب کند)
```

اولین پاسخی که `api_url` معتبر بدهد، **خودش آدرس نهایی** است — حتی اگر کاربر فقط IP را زده باشد.
اپ `api_url` پاسخ را ذخیره می‌کند و دیگر نیازی به IP/پورت ندارد.

---

## ۲. جریان کامل (۵ قدم کاربر)

```
┌────────────────────────────────────────────────────────────────────┐
│ ۱. صفحهٔ «اتصال به سرور»: یک فیلد (آدرس/IP/دامنه) + دکمهٔ «اتصال» │
│ ۲. اپ: GET /api/config → موفق: ذخیره + نمایش نام/نسخه/وضعیت      │
│    → ناموفق: پیام واضح (سرور پیدا نشد / آدرس را بررسی کنید)       │
│ ۳. اگر activated=false → صفحهٔ کد فعال‌سازی → POST /api/activate  │
│ ۴. ورود ادمین (یک‌بار) → POST /api/login → ذخیرهٔ امن توکن        │
│ ۵. صفحهٔ اصلی: GET /api/visitors + جریان لحظه‌ای /api/events      │
└────────────────────────────────────────────────────────────────────┘
```

---

## ۳. همهٔ Endpoints و فرمت دقیق JSON

همهٔ پاسخ‌ها JSON/UTF-8. احراز داده‌ها: هدر `Authorization: Bearer <token>`.

| روش | مسیر | احراز | توضیح |
|---|---|---|---|
| GET | `/api/ping` | نه | `{"status":"ok","time":1726411215.2}` |
| GET | `/api/config` | نه | خودپیکربندی (مثل نمونهٔ بالا) |
| GET | `/api/health` | نه | گزارش کامل سلامت (دیتابیس، فعال‌سازی، uptime) |
| GET | `/api/activate` | نه | `{"activated": true}` |
| POST | `/api/activate` | نه | `{"code":"..."}` → `{"ok":true,"activated":true}` یا ۴۰۳ `invalid_code` |
| POST | `/api/login` | نه | `{"username":"admin","password":"..."}` → `{"ok":true,"token":"<64 hex>","user":{...}}` |
| GET | `/api/visitors` | Bearer + فعال | `{"ok":true,"count":2,"visitors":[...]}` (جدیدترین‌ها، حداکثر ۱۰۰) |
| POST | `/api/visitors` | Bearer + فعال | `{"name":"...","phone":"...","purpose":"...","host_name":"..."}` → ۲۰۱ `{"ok":true,"id":7,"event_seq":12}` |
| GET | `/api/visitors/since?after_id=N&limit=100` | Bearer + فعال | همگام‌سازی افزونه‌ای (فقط رکوردهای جدیدتر از N) |
| GET | `/api/events` | Bearer + فعال | **جریان لحظه‌ای Server-Sent Events** |

### خطایهای رایج

| کد | بدنه | معنی / واکنش اپ |
|---|---|---|
| 401 | `{"error":"unauthorized"}` | توکن نامعتبر — دوباره لاگین |
| 402 | `{"error":"not_activated"}` | کد فعال‌سازی لازم است — صفحهٔ ۳ |
| 403 | `{"ok":false,"error":"invalid_code"}` | کد فعال‌سازی اشتباه |
| 500 | `{"error":"db_unavailable",...}` | دیتابیس سرور مشکل دارد — پیام به کاربر |

### نمونهٔ `GET /api/visitors/since?after_id=1`

```json
{
  "ok": true,
  "count": 1,
  "visitors": [
    {"id": 2, "name": "سارا احمدی", "phone": "09123456789", "purpose": "ویزیت", "host_name": "", "created_at": "2026-09-16 01:41:22"}
  ],
  "max_id": 2,
  "has_more": false
}
```

`max_id` را ذخیره کنید — این «مرجع همگام‌سازی» شماست.

---

## ۴. دریافت اطلاعات لحظه‌ای (SSE)

سرور `1.1.0` به‌بالا **Server-Sent Events** دارد: اتصال واحد، فقط در حالت صعودی، و خودبه‌خود
از پشت IIS/ARR هم بدون تنظیم خاصی کار می‌کند.

### فرمت جریان

هدرهای پاسخ: `Content-Type: text/event-stream`، `Cache-Control: no-cache`، `Connection: close`.

```
data: {"type": "hello", "version": "1.1.0", "activated": true, "last_visitor_id": 42, "retry_ms": 5000}

data: {"seq": 11, "type": "visitor.created", "time": "2026-09-16 01:45:02", "data": {"id": 43, "name": "...", "phone": "...", "purpose": "...", "host_name": "...", "created_at": "..."}}

data: {"seq": 12, "type": "activation.changed", "time": "2026-09-16 01:46:10", "data": {"activated": true}}

: keepalive
```

- `hello` — رویداد اول؛ با `last_visitor_id` می‌توانید دقیقاً همگام شوید (بعداً `since?after_id=`).
- `visitor.created` — بلافاصله بعد از ثبت هر بازدیدکننده (همان رکورد).
- `activation.changed` — تغییر وضعیت فعال‌سازی.
- `: keepalive` — کامنت هر ۱۵ ثانیه (جریان زنده نگه می‌دارد؛ نادیده بگیرید).
- `retry_ms: 5000` — باز INTERVAL پیشنهادی؛ OkHttp/EventSource خودش به‌صورت خودکار با این مقدار reconnect می‌کند.

### استراتژی مطمئن «LIVE + RESYNC»

```
SSE وصل است؟ ──بله──► رویداد visitor.created → اضافه به لیست (اگر id را ندیده‌اید)
      │
     نه (قطع شد)
      ▼
با backoff دوباره وصل شو (EventSource خودکار: 5s، 10s، 20s، ...)
      ▼
به‌محض وصل شدن: GET /api/visitors/since?after_id=<آخرین id دیده‌شده>
   → رکوردهای گم‌شده را می‌گیرید (پلیرهای حین قطع) → سپس جریان زنده ادامه پیدا می‌کند
```

این ترکیب، دادهٔ لحظه‌ای را **بدون هیچ ضرری** (no-loss) به اپ می‌رساند — حتی از پشت IIS.

> **نسخه‌های قدیمی‌تر بدون SSE:** اگر `features` شامل `"sse"` نبود (یا اپ شما SSE ندارد)،
> هر ۱۰–۱۵ ثانیه `GET /api/visitors/since?after_id=<max_id>` را بخوانید — نتیجهٔ یکسان.

---

## ۵. کد آمادهٔ Kotlin (OkHttp + okhttp-sse)

**وابستگی (build.gradle):**
```gradle
implementation "com.squareup.okhttp3:okhttp:4.12.0"
implementation "com.squareup.okhttp3:okhttp-sse:4.12.0"
implementation "com.google.code.gson:gson:2.10.1"
```

```kotlin
package com.vizitor

import okhttp3.*
import okhttp3.sse.*
import okio.ByteString.Companion.encodeUtf8
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** یکپارچگی Vizitor: خودپیکربندی + احراز + داده + لحظه‌ای */
object VizitorApi {

    data class ServerConfig(
        val name: String,
        val version: String,
        val api_url: String,
        val activated: Boolean,
        val db_engine: String,
        val realtime: Boolean,
        val features: List<String>,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // برای SSE: بدون مهلت خواندن
        .build()

    private val gson = com.google.gson.Gson()
    private var token: String? = null
    private var baseUrl: String? = null     // مثال: "http://217.20.30.40/api"

    // ---------------- مرحله ۲: اتصال هوشمند (چند کلیک) ----------------
    /** فقط آدرس کاربر را می‌گیرد؛ همه‌چیز را خودش کشف می‌کند. */
    fun probe(hostInput: String): ServerConfig? {
        val candidates = buildList {
            val h = hostInput.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
            add("http://$h/api/config")
            add("http://$h:8080/api/config")
        }
        for (url in candidates) {
            try {
                val req = Request.Builder().url(url).get().build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body!!.string())
                        val cfg = ServerConfig(
                            name = json.optString("name", "Vizitor"),
                            version = json.optString("version"),
                            api_url = json.optString("api_url", url.removeSuffix("/config")),
                            activated = json.optBoolean("activated"),
                            db_engine = json.optString("db_engine"),
                            realtime = json.optBoolean("realtime"),
                            features = json.optJSONArray("features")?.let {
                                (0 until it.length()).map { i -> it.getString(i) }
                            } ?: emptyList(),
                        )
                        baseUrl = cfg.api_url
                        return cfg
                    }
                }
            } catch (_: Exception) { /* کاندیدای بعدی */ }
        }
        return null
    }

    // ---------------- مرحله ۳: فعال‌سازی ----------------
    fun activate(code: String): Boolean =
        post("/activate", JSONObject().put("code", code)).optBoolean("ok")

    // ---------------- مرحله ۴: لاگین ----------------
    fun login(username: String, password: String): Boolean {
        val r = post("/login", JSONObject().put("username", username).put("password", password))
        return if (r.optBoolean("ok")) { token = r.optString("token"); true } else false
    }

    fun requireToken(): String =
        checkNotNull(token) { "ابتلا لاگین کنید" }

    // ---------------- مرحله ۵: داده ----------------
    fun fetchVisitors(): List<Visitor> {
        val r = get("/visitors")
        val arr = r.optJSONArray("visitors") ?: return emptyList()
        return (0 until arr.length()).map { parseVisitor(arr.getJSONObject(it)) }
    }

    /** همگام‌سازی افزونه‌ای — فقط رکوردهای جدیدتر از lastId */
    fun fetchSince(lastId: Long): SyncResult {
        val r = get("/visitors/since?after_id=$lastId")
        val arr = r.optJSONArray("visitors") ?: return SyncResult(emptyList(), lastId, false)
        val list = (0 until arr.length()).map { parseVisitor(arr.getJSONObject(it)) }
        return SyncResult(list, r.optLong("max_id", lastId), r.optBoolean("has_more"))
    }

    // ---------------- لحظه‌ای: SSE ----------------
    fun startRealtime(listener: RealtimeListener) {
        val req = Request.Builder()
            .url("$baseUrl${'/events'}")
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer ${requireToken()}")
            .build()
        EventSources.createFactory(client).newEventSource(req, object : EventSourceListener() {
            override fun onOpen(es: EventSource, response: Response) { /* زنده شد */ }
            override fun onEvent(es: EventSource, id: String?, type: String?, data: String?) {
                when (type) {
                    "hello" -> listener.onHello(JSONObject(data).optLong("last_visitor_id"))
                    "visitor.created" -> listener.onVisitor(parseVisitor(JSONObject(data)))
                    "activation.changed" -> listener.onActivated(JSONObject(data).optBoolean("activated"))
                }
            }
            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                listener.onDisconnected()   // EventSource خودکار با retry=5s وصل می‌شود؛
            }                                // بعد از وصل مجدد onHello می‌آید → آنجا resync کنید
        })
    }

    private interface RealtimeListenerInternal
    fun interface RealtimeListener {
        fun onHello(lastVisitorId: Long)               // ← اینجا fetchSince(lastVisitorId) بزنید
        fun onVisitor(v: Visitor)                      // ← مستقیم به UI push کنید
        fun onActivated(activated: Boolean)
        fun onDisconnected()
    }

    // ---------------- ابزارهای داخلی ----------------
    private fun get(path: String): JSONObject {
        val req = Request.Builder().url(baseUrl + path)
            .header("Authorization", "Bearer ${requireToken()}").get().build()
        return client.newCall(req).execute().use {
            check(it.isSuccessful) { "HTTP ${it.code}" }
            JSONObject(it.body!!.string())
        }
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder().url(baseUrl + path)
            .post(body.toString().encodeUtf8().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return client.newCall(req).execute().use {
            // حتی 4xx بدنهٔ JSON دارد؛ می‌خوانیم
            JSONObject(it.body!!.string())
        }
    }

    private fun parseVisitor(o: JSONObject) = Visitor(
        id = o.optLong("id"),
        name = o.optString("name"),
        phone = o.optString("phone"),
        purpose = o.optString("purpose"),
        hostName = o.optString("host_name"),
        createdAt = o.optString("created_at"),
    )
}

data class Visitor(
    val id: Long,
    val name: String,
    val phone: String,
    val purpose: String,
    val hostName: String,
    val createdAt: String,
)

data class SyncResult(val visitors: List<Visitor>, val maxId: Long, val hasMore: Boolean)
```

### صفحهٔ «اتصال به سرور» (Compose) — واقعاً یک فیلد و یک دکمه

```kotlin
@Composable
fun ConnectScreen(onConnected: (VizitorApi.ServerConfig) -> Unit) {
    var host by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        // اگر آدرس قبلی ذخیره شده، یک‌کلیک: مستقیم وصل شو
        val saved = SecureStore.serverUrl() ?: return@LaunchedEffect
        status = "در حال اتصال..."
        if (VizitorApi.probe(saved) == null) SecureStore.clear() else onConnected(auto = true)
    }
    Column(Modifier.padding(16.dp)) {
        Text("سرور Vizitor (IP یا دامنه)")
        OutlinedTextField(host, { host = it }, label = { Text("مثال: 217.20.30.40") })
        Button(
            enabled = host.isNotBlank(),
            onClick = {
                status = "در حال اتصال..."
                scope.launch(Dispatchers.IO) {
                    val cfg = VizitorApi.probe(host)
                    withContext(Dispatchers.Main) {
                        if (cfg == null) status = "❌ سرور پیدا نشد — آدرس و اینترنت سرور را بررسی کنید"
                        else {
                            SecureStore.saveServer(host)
                            onConnected(cfg)   // → مرحلهٔ فعال‌سازی/لاگین
                        }
                    }
                }
            }
        ) { Text("اتصال") }
        if (status.isNotEmpty()) Spacer(8.dp) else {}
        if (status.isNotEmpty()) Text(status)
    }
}
```

### ذخیرهٔ امن (EncryptedSharedPreferences)

```kotlin
object SecureStore {
    private val prefs by lazy {
        androidx.security.crypto.EncryptedSharedPreferences.create(
            "vizitor", MasterKey.Builder(ContextCompat.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    fun saveServer(host: String) = prefs.edit().putString("server", host).apply()
    fun serverUrl(): String? = prefs.getString("server", null)
    fun saveToken(t: String) = prefs.edit().putString("token", t).apply()
    fun token(): String? = prefs.getString("token", null)
    fun clear() = prefs.edit().remove("server").remove("token").apply()
}
```

### ترکیب LIVE + RESYNC در ViewModel

```kotlin
class VisitorsViewModel(private val repo: VizitorRepository) : ViewModel() {
    val visitors = MutableStateFlow<List<Visitor>>(emptyList())
    private var maxId = 0L

    fun start(cfg: VizitorApi.ServerConfig) {
        viewModelScope.launch {
            val initial = withContext(Dispatchers.IO) { repo.fetchVisitors() }
            visitors.value = initial
            maxId = initial.maxOfOrNull { it.id } ?: 0L
        }
        repo.startRealtime(object : VizitorApi.RealtimeListener {
            override fun onHello(lastVisitorId: Long) {
                // وصل مجدد شد → پلیرهای گم‌شده را بگیر
                viewModelScope.launch(Dispatchers.IO) {
                    val resync = repo.fetchSince(maxId)
                    if (resync.visitors.isNotEmpty()) {
                        maxId = resync.maxId
                        visitors.value = (resync.visitors + visitors.value).distinctBy { it.id }.sortedByDescending { it.id }
                    }
                }
            }
            override fun onVisitor(v: Visitor) {
                if (v.id > maxId) {                       // فقط رکورد واقعاً جدید
                    maxId = v.id
                    visitors.value = (listOf(v) + visitors.value).distinctBy { it.id }
                }
            }
            override fun onActivated(activated: Boolean) { /* badge وضعیت */ }
            override fun onDisconnected() { /* آیکون "در حال اتصال مجدد…" */ }
        })
    }
}
```

---

## ۶. نکات اتصال دقیق (IIS + SQL Server + لحظه‌ای)

1. **IIS شفاف است:** آدرس اپ همیشه `api_url` پاسخ `/api/config` است. اگر نصب‌کنندهٔ ویندوز «اتصال از طریق IIS»
   را فعال کرده باشد، آدرس `http://سرور/api` (پورت 80) خواهد بود — SSE و بقیهٔ endpointها بدون تنظیم
   خاصی از پشت پروکسی معکوس IIS (ARR) عبور می‌کنند.
2. **SQL Server شفاف است:** `db_engine: "sqlserver"` فقط در `/api/config` نمایش داده می‌شود؛ رفتار API
   دقیقاً مثل SQLite/MySQL است. اگر دیتابیس موقتاً قطع باشد، `/api/health` وضعیت `db.status="error"` می‌دهد —
   اپ می‌تواند بدون ری‌استارت، با retry بعدی برگردد.
3. **مهلت‌ها:** `/api/config` و `/api/health` همیشه در کمترین زمان پاسخ می‌دهند؛ اما endpointهای
   داده‌ای در اولین اتصال ممکن است با راه‌اندازی دیتابیس کمی دیرتر پاسخ دهند → مهلت خواندن ۱۰ ثانیه کافی است.
4. **توکن:** پسوند `Bearer` دقیقاً همان شکل (`Authorization: Bearer <token>`). اگر ۴۰۱ گرفتید،
   دوباره `POST /api/login` بزنید (سشن‌ها در سرور ماندگارند؛ لاگین دوباره فقط در بعد از ری‌استارت/پاک‌شدن لازم است).
5. **HTTPS:** اگر بعداً دامنه + گواهی روی IIS گذاشتید، فقط آدرس ذخیره‌شده را با `https://` عوض کنید —
   هیچ تغییر دیگری لازم نیست (EventSource و OkHttp هر دو TLS را پشتیبانی می‌کنند).
6. **آزمایش سریع بدون اپ (از ادمین‌پنل ویندوز یا هر ترمینال):**
   ```
   curl http://سرور/api/config
   curl -N http://سرور/api/events -H "Authorization: Bearer <TOKEN>"
   ```
   `-N` یعنی بدون buffer — رویدادها همان لحظه می‌آیند.
