# Vizitor Android → SQL Server مستقیم — آثار کار (mirror)

این پوشه **آینهٔ کار روی ریپازیتوری هم‌خانوادهٔ `Companymeelano/viz`** است
(شاخهٔ محلی `arena/sql-direct-audit`). چون access push به آن ریپازیتوری برای
این agent وجود ندارد، نسخهٔ مطمئن از آثار در همین جایی که session روی آن
ردیابی می‌شود نگهداری می‌شود.

## فایل‌ها
| فایل | توضیح |
|---|---|
| `AUDIT-CURRENT-ARCHITECTURE.md` | ممیزی کامل معماری فعلی + معماری هدف + نقشهٔ مهاجرت + افتراقات VERIFY |
| `ATIRAN-SCHEMA-EXTRACTED.md` | ۵۵ جدول/نمای Atiran با تمام ستون‌ها/PK/FK (استخراج‌شده از مدل EF واقعی ERP) |
| `sql/00_audit_atiran2.sql` | اسکریپت **فقط‌خواندنی** ممیزی سرور واقعی — **نسخهٔ v2** (خروجی را به agent بفرستید) |
| `sql/01_setup_vizitor_user.sql` | آماده‌سازی غیرتلفیقی سرور: کاربر فقط‌خواندنی `vizitor_android` + فایروال فقط LAN |
| `SqlConnectionManager.kt` | فاز ۲: pool + validation + retry + transaction + StateFlow + پیام فارسی خطا |
| `SecureDbStore.kt` | فاز ۲: ذخیرهٔ امن اطلاعات اتصال (AES-GCM + Android Keystore) |

## تاریخچهٔ `sql/00_audit_atiran2.sql`
* **v1** → روی سرور خطا داد: `Msg 102, Level 15, State 1, Line 142 — Incorrect syntax near '@pwSql'.`
  علت: کل فایل یک batch واحد بود (بدون `GO`) و همان یک دستور `EXEC sp_executesql STUFF(...)`
  خطای parse می‌داد → طبق قانون SQL Server هیچ‌کدام از بخش‌های اسکریپت اجرا نشد و خروجی خالی ماند.
* **v2** → هر بخش با `GO` جدا شد (خطای یک بخش بقیه را از کار نمی‌اندازد)، بخش
  «الگوی پسورد» با حلقهٔ `WHILE` + `EXEC (@sql)` بازنویسی شد، جدول‌های ناموجود در دیتابیس
  «رد» می‌شوند نه اینکه کرش کنند، و بخش‌های جدید اضافه شد (شبکه/پورت، کشف نام اشیاء).
* **v3 (فعلی)** → دو اصلاح بعد از اجرای واقعی روی سرور:
  - `Msg 208 Invalid object name 'wanted'` در بخش ۰۷: در T-SQL یک CTE فقط تا پایان
    **همان یک دستور** بعدی زنده است؛ دستور دوم که به `wanted` ارجاع می‌داد آن را نمی‌دید.
    الان از table variable (`@wanted`) استفاده می‌شود.
  - **نام دیتابیس واقعی روی سرور: `Meelano`** (نه Atiran2). چک نام سخت‌گیرانه حذف شد:
    اسکریپت هر دیتابیسی که به آن وصل باشید را ممیزی می‌کند، نامش را چاپ می‌کند و
    وجود `dbo.CUSTOMERS` را probe می‌کند تا مطمئن شویم دیتابیس درست است.
  - همهٔ مقایسه‌های نام اشیاء با `COLLATE Latin1_General_CI_AS` → روی دیتابیس با
    collation حساس به بزرگی/کوچکی حروف هم درست کار می‌کند.
* دو باگ پنهان v1 هم با مستندات رسمی مایکروسافت (نه حدس) پیدا و رفع شد:
  `sys.index_columns` ستون `is_primary_key` ندارد (باید به `sys.indexes` join شود) و
  `sys.parameters` ستون `PARAMETER_NAME` ندارد (نام ستون `name` است).
* اعتبارسنجی: هر ۱۵ batch با پارسر T-SQL (sqlglot) parse شد + SQL داینامیک تولیدشده
  شبیه‌سازی و parse شد. (`sqlfluff` در این sandbox حتی روی `SELECT 1 AS c;` خطا می‌دهد → استفاده نشد.)
* محتوای اجرایی فایل کاملاً ASCII است؛ متن فارسی فقط داخل کامنت‌هاست (بی‌اثر روی اجرا).

## تاریخچهٔ `sql/01_setup_vizitor_user.sql`
* نام دیتابیس به یک متغیر در بالای فایل منتقل شد (`DECLARE @dbName = N'Meelano'`) و
  ساخت کاربر/نقش با dynamic SQL انجام می‌شود تا مستقل از دیتابیسِ بازِ پنجرهٔ SSMS باشد.
* اگر نام دیتابیس اشتباه باشد، فقط یک پیام واضح چاپ می‌شود و **هیچ تغییری روی سرور انجام نمی‌شود**.
* یک بخش جدید `01_LOGINMODE_CHECK` نشان می‌دهد سرور در حالت احراز هویت ترکیبی
  (LoginMode = 2) هست یا نه — چون ورود کاربر SQL بدون آن ممکن نیست.
* عضویت نقش idempotent است و ردیف `02_ROLE_CHECK` صحت آن را نشان می‌دهد.

## تغییرات لازم در `viz` (برای re-apply روی ریپازیتوری اصلی)
`app/build.gradle.kts` → افزودن:
```kotlin
implementation("com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre8")
```
`app/proguard-rules.pro` → افزودن:
```
-keep class com.microsoft.sqlserver.jdbc.** { *; }
-keepclassmembers class com.microsoft.sqlserver.jdbc.** { *; }
```
