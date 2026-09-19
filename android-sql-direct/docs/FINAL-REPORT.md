# گزارش نهایی — مهاجرت اپ ویزیتور به SQL Server مستقیم

نسخهٔ این سند: 2026-09-18 · وضعیت کلی: **پیاده‌سازی انجام شده؛ دو مقدار ERP و یک تأیید
دیتابیس باقی است** (بند ۱۳).

علامت‌ها: ✅ کامل · 🟡 انجام شده و منتظر تأیید داده است · ⛔ تا رسیدن یک مقدار واقعی متوقف

---

## ۱) فایل‌های جدید / تغییرکرده

| فایل | نقش |
|---|---|
| `app/src/main/java/.../data/sql/SqlConnectionManager.kt` | استخر اتصال + وضعیت اتصال + تراکنش |
| `app/src/main/java/.../data/sql/MeelanoDataSource.kt` | همهٔ کوئری‌ها (خواندن + نوشتن) |
| `app/src/main/java/.../data/local/SecureDbStore.kt` | ذخیرهٔ امن تنظیمات اتصال |
| `app/src/main/java/.../ui/screens/SettingsScreen.kt` | صفحهٔ تنظیمات SQL (بدون تغییر ظاهر) |
| `app/src/main/java/.../VizitorViewModel.kt` | اتصال به لایهٔ جدید |
| `app/build.gradle.kts`, `app/proguard-rules.pro` | درایور JDBC + قواعد نگه‌داشتن کلاس‌ها |
| `patches/phase2-sql-ui-wiring.patch` | همین مجموعه به‌صورت یک patch قابل اعمال روی شاخهٔ `2331c90` |

## ۲) معماری

```
UI (Compose) → ViewModel → Repository → MeelanoDataSource → SqlConnectionManager → TDS 1433 → Meelano
```

* هیچ منطق تجاری در UI نیست؛ صفحه‌ها همان صفحه‌های فعلی‌اند (بدون بازطراحی).
* هر تابع `suspend` و روی `Dispatchers.IO` — هیچ کوئری‌ای روی ترد UI اجرا نمی‌شود.
* استخر اتصال با سقف ۴ و اعتبارسنجی ۵ ثانیه؛ بازگشت اتصال در `finally` (بدون leak).
* همهٔ مقادیر پارامتری؛ هیچ رشته‌ای به SQL چسبانده نمی‌شود.

## ۳) جدول‌ها (خواندن)

`dbo.sys_users`, `dbo.sys_vis`, `dbo.visitors`, `dbo.sys_cus`, `dbo.CUSTOMERS`,
`dbo.custgroup`, `dbo.sys_anb`, `dbo.inventory`, `dbo.inventory_anbars`, `dbo.anbars`,
`dbo.forosh_price`, `dbo.kagroup`, `dbo.VW_InventoryAnbars`, `sailfact_pish`,
`subsailfact_pish`, `cust_act`, `sal_mali`.

## ۴) Stored Procedure‌ها

| SP | وضعیت |
|---|---|
| `dbo.add_sail_pish` | ✅ فراخوانی پیاده شده (۲۶ پارامتر، موضعی، `@id_en` خروجی) |
| `dbo.Edit_sail_pish` | 🟡 خوانده و مستند شده؛ در کد فقط برای «کنار گذاشتن پیش‌فاکتور نیمه‌کاره» از الگویش استفاده شده |
| `dbo.new_cust` | ⛔ فراخوانی نوشته نشده — چند مقدار امضایش (مثل `@act_bedbes`, `@sh_i_m`) باید از یک ثبت واقعی گرفته شود |
| `dbo.FixManCustomer`, `dbo.AddInvoice`, `dbo.FixMojodi`, `dbo.UpdateMojodiInventory*` | 🟡 خوانده و مستند؛ در مسیر پیش‌فاکتور لازم نیستند، در فاکتورشدن لازم‌اند |

## ۵) توابع

`dbo.IsAccountingSystemStarted()`, `dbo.what_date()`, `dbo.cal_gain()`, `dbo.dif_date()`,
`dbo.CalcDetailsPishfactor()` — همه فقط مستند شده‌اند (سرور خودش صدایشان می‌زند؛ اپ
نیازی به فراخوانی مستقیم ندارد).

## ۶) نگاشت API → SQL

| اندپوینت فعلی | معادل SQL | وضعیت |
|---|---|---|
| `POST action=login` | `SELECT ... FROM dbo.sys_users WHERE user_name=? AND CONVERT(varchar(50),user_password)=? AND active=1` | ✅ |
| `GET action=ping` | `SELECT DB_NAME(), SERVERPROPERTY('ProductVersion'), COUNT(*) FROM dbo.CUSTOMERS` | ✅ |
| `GET action=catalog` | `inventory ⋈ forosh_price` + `dbo.VW_InventoryAnbars` | ✅ |
| `GET action=customers` | `sys_cus ⋈ CUSTOMERS ⋈ custgroup` | ✅ |
| `GET action=sal_mali` | `dbo.sal_mali` | 🟡 ستون‌های گزارش قدیمی باید با همین جدول تطبیق داده شود |
| `POST action=submit_invoice` | `add_sail_pish` + `INSERT dbo.subsailtemp_pish (mod=1)` | 🟡 کد کامل؛ منتظر ۵ مقدار ERP (بند ۱۳) |
| `POST action=submit_customer` | `dbo.new_cust` | ⛔ منتظر مقادیر واقعی |

## ۷) تنظیمات SQL Server

* SQL 2014، کامپتیبیلیتی ۱۲۰، کالِیشن فارسی؛ TDS روی `0.0.0.0:1433` (تأییدشده از رجیستری سرور).
* توصیه: TLS روی 1433 (یا `encrypt=true;trustServerCertificate=false` بعد از نصب گواهی) — در حال حاضر اتصال داخلی LAN است.
* `max degree of parallelism` و سایز حافظه دست‌نخورده می‌ماند (اپ فقط SELECT/EXEC سبک می‌زند).

## ۸) فایروال

* فقط پورت **1433 TCP** برای رنج LAN (مثلاً `192.168.1.0/24`) باز شود — نه اینترنت.
* دستور NETSH در انتهای `sql/01_setup_vizitor_user.sql` چاپ می‌شود.
* دسترسی از بیرون فروشگاه فقط با VPN (توصیه) یا port-forward موقت؛ هر دو خارج از v1.

## ۹) تنظیمات اندروید

* صفحهٔ تنظیمات: IP/DB/کاربر/رمز SQL (ذخیرهٔ امن، بدون هیچ رمزی در کد).
* مقدار پیش‌فرض: `192.168.1.150` / `1433` / دیتابیس `Meelano`.
* کاربر SQL اختصاصی `vizitor_android` (بدون استفاده از `AdminAn`).
* نمایش وضعیت اتصال + دکمهٔ تست اتصال + پیام خطای روشن در قطعی شبکه.

## ۱۰) دسترسی‌های SQL

`db_datareader` روی دیتابیس + (پس از تأیید نهایی) فقط این‌ها:
`GRANT EXECUTE ON dbo.add_sail_pish TO vizitor_android` و
`GRANT INSERT ON dbo.subsailtemp_pish TO vizitor_android`.
هیچ UPDATE/DELETE/DDL ای به کاربر اپ داده نمی‌شود.

## ۱۱) فهرست خطاها و رفتار اپ

| حالت | رفتار |
|---|---|
| قطعی شبکه / خاموشی SQL | پیام فارسی «اتصال به سرور برقرار نیست» + دکمهٔ تلاش مجدد؛ بدون crash |
| رمز/کاربر غلط | «نام کاربری یا رمز نادرست است» (بدون افشای جزئیات) |
| کمی موجودی | پیام روی همان قلم، اجازهٔ ادامه با هشدار (مطابق رفتار فعلی اپ) |
| نبود یک شیء/ستون ERP | `preInvoiceHealth()` پیش از نوشتن، نام دقیق شیء/ستونِ غایب را می‌گوید |
| خطا در میان اقلام | توقف، برگرداندن شمارهٔ پیش‌فاکتور، تلاش مجدد؛ «کنار گذاشتن» فقط با تأیید کارفرما |
| قطعی وسط ثبت | idempotency سمت اپ (`client_invoice_id` در Room) تا ثبت تکراری رخ ندهد |

## ۱۲) تست‌های انجام‌شده

* ✅ `tools/verify_tsql.py` — همهٔ اسکریپت‌های SQL: ALL CHECKS PASSED (شامل تست دامنهٔ متغیر بین GOها).
* ✅ `tools/check_sql_columns.py` — همهٔ کوئری‌های Kotlin با شِمای واقعی مقایسه شد؛ selftest خودش هم INSERT/UPDATE را می‌گیرد. نتیجه: NO UNKNOWN IDENTIFIERS.
* ✅ تست زندهٔ ورود روی سرور (`07_login_verify.sql` → `V1|MATCH`).
* ✅ صحت `add_sail_pish` و تریگرها: از متن واقعی روی سرور (نه حدس).
* ⛔ تست نوشتن واقعی: نیاز به بند ۱۳.

## ۱۳) مواردی که فقط روی سرور واقعی قابل تست‌اند (باز)

1. **یک پیش‌فاکتور واقعی در خودِ ERP بساز و ذخیره کن** → پنج مقدار سربرگ (`ted_rooz`, `ph_kh`,
   `mod_darsad_vis`, `rdf_sarbarg`, `gainall`) از همان سطر خوانده می‌شود و کد فعال می‌شود.
   وضعیت 2026-09-18: دو کوئری نمونه زده شد ولی خروجی فقط «سرستون» بود - یعنی
   `sailfact_pish` و `subsailfact_pish` هر دو **۰ سطر** دارند و هنوز پیش‌فاکتوری در آن
   دیتابیس ذخیره نشده. اسکریپت آمادهٔ این کار: `sql/10_preinvoice_sample.sql`
   (اول خودش شمارهٔ سطرها را می‌گوید، بعد سربرگ و اقلام آخرین پیش‌فاکتور را).
   از همین دو گرید یک نکتهٔ مثبت هم درآمد: سرستون‌های `sailfact_pish` (۵۲ ستون) و
   `subsailfact_pish` (۲۶ ستون، شامل `PerPromotion`) عیناً همان چیزی است که کد انتظار دارد.
2. **تأیید دیتابیس مقصد** با `sql/09_compare_databases.sql` (یا بخش ۶ اسکریپت 08) — تا روشن شود
   `Meelano` همان دیتابیس زنده است یا `Atiran14050603`.
3. تأیید اینکه `Meelano` هم `subsailtemp_pish` و `trig_sst_pish` نسخهٔ یکسان دارد (کوئری در راه ۱۰).
4. تست login و کارت کاتالوگ با کاربر `vizitor_android` روی شبکهٔ واقعی.
5. یک ثبت آزمایشی کامل + کنترل در لیست پیش‌فاکتورهای ERP.

## ۱۴) نصب و تحویل

1. `sql/01_setup_vizitor_user.sql` روی سرور (ساخت کاربر اپ + دسترسی‌ها).
2. اجرای دستور NETSH چاپ‌شده در CMD سرور (باز کردن 1433 فقط برای LAN).
3. نصب اپ و پر کردن صفحهٔ تنظیمات (IP/DB/کاربر/رمز).
4. «تست اتصال» → سبز شدن چراغ وضعیت.
5. ثبت یک پیش‌فاکتور آزمایشی و کنترل آن در ERP.
6. تحویل: همین سند + `docs/write-path/*` + `patches/phase2-sql-ui-wiring.patch`.
