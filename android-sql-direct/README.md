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

## تاریخچهٔ نسخه‌های ۰۰ (v4 → v5) — مهم
* **v4** روی سرور این خطا را داد: `Msg 102 ... Incorrect syntax near '<invisible>'`
  علت: v3 فایل را **UTF-8 با BOM** ذخیره کرده بود و SSMS آن بایت نامرئی را داخل اولین
  batch می‌خواند. اکنون هر دو فایل **ASCII خالص و بدون BOM** هستند و ابزار
  `tools/verify_tsql.py` وجود BOM و هر کاراکتر غیر-ASCII (حتی در کامنت) را رد می‌کند.
* **v5 (فعلی)**: تمام خروجی ممیزی حالا با `PRINT` در تب **Messages** چاپ می‌شود
  (به‌صورت خط‌های pipe-separated با برچسب بخش، مثل `02|CUSTOMERS|SHMO|nvarchar|30|NOT NULL|PK`).
  دیگر لازم نیست از grid کپی کنید: کل audit در Messages است.
  (`PRINT` حداکثر ۴۰۰۰ کاراکتر یونیکد در هر فراخوانی می‌پذیرد → متن‌ها در تکه‌های
  ۴۰۰۰ کاراکتری چاپ می‌شوند.)
* **یک باگ واقعی دیگر قبل از اجرای شما گرفته شد**: در §04 یک `)` جا افتاده بود
  (final depth = 1) که روی سرور دقیقاً `Msg 102` می‌داد؛ به ابزار یک چک
  «توازن پرانتزها» و «توازن BEGIN/END و TRY/CATCH» اضافه شد و اسکریپت اصلاح شد.
* نتیجهٔ آخرین اجرای ابزار: `15/15` و `2/2` batch سالم، هر سه دستور داینامیک
  بازسازی و parse شدند، بدون BOM، ASCII خالص → **ALL CHECKS PASSED**.

## فاز ۲ — اتصال UI به لایهٔ SQL (انجام‌شده، 2026-09-18)
یک patch کامل و آماده برای apply روی ریپازیتوری `Companymeelano/viz`:
`patches/phase2-sql-ui-wiring.patch` (۶ فایل، ۶۲۸ خط افزودن)

| فایل | تغییر |
|---|---|
| `app/build.gradle.kts` | افزودن `com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre8` |
| `app/proguard-rules.pro` | keep برای `com.microsoft.sqlserver.jdbc.**` + dontwarn |
| `data/sql/SqlConnectionManager.kt` | **جدید** — pool، retry، transaction، StateFlow، پیام فارسی خطا |
| `data/local/SecureDbStore.kt` | **جدید** — ذخیرهٔ رمزنگاری‌شدهٔ پیکربندی (AES-GCM + Keystore) |
| `VizitorViewModel.kt` | `dbState`/`dbTesting`/`dbConfig` + `testDbConnection()`/`refreshDbState()`/`disconnectDb()`/`clearDbConfig()` |
| `ui/screens/SettingsScreen.kt` | بخش جدید «اتصال مستقیم SQL Server»: فیلدهای سرور/پورت/دیتابیس/کاربر/رمز + دکمهٔ «تست اتصال SQL» + نمایش زندهٔ وضعیت |

نکات پیاده‌سازی:
* مقادیر پیش‌فرض فرم از **ممیزی واقعی** پر شده‌اند: `192.168.1.150`، پورت `1433`،
  دیتابیس `Meelano`، کاربر `vizitor_android`.
* پیکربندی **فقط پس از اتصال موفق** ذخیره می‌شود (نه صرفاً با زدن دکمه).
* رمز عبور با `PasswordVisualTransformation` نمایش داده می‌شود، هرگز Log نمی‌شود و
  فقط رمزنگاری‌شده در Keystore دستگاه می‌ماند.
* هیچ عنصر UI قبلی حذف/بازطراحی نشد؛ فقط یک بخش به صفحهٔ تنظیمات اضافه شد.

وضعیت: **کد نوشته شده ولی کامپایل نشده** (در sandbox نه Android SDK هست و نه JDK) —
اعتبارسنجی انجام‌شده: تعادل ساختاری هر ۴ فایل Kotlin، بررسی وجود همهٔ symbolهای
ارجاع‌داده‌شده، و تطابق امضاهای `SqlConnectionManager`/`SecureDbStore` با مصرف‌کننده‌ها.

## تاریخچهٔ `sql/06_login_probe.sql`
* **v1** → سرور این خطا را داد: `Msg 156, Level 15, State 1, Line 82 — Incorrect syntax
  near the keyword 'user'`. علت: `FROM EMS.user` — کلمهٔ `USER` **کلیدواژهٔ رزرو T-SQL** است
  و باید `[EMS].[user]` نوشته شود. چون کل بدنهٔ اسکریپت در **یک batch** بود، این خطای
  سینتکس همهٔ بخش‌های L1..L6 را از بین برد (فقط سرصفحه چاپ شد).
* **v2 (فعلی)** → هر بخش `GO` جداگانه دارد و همه در یک **جدول موقت** (`#o`) جمع شده و
  در انتها یک‌جا چاپ می‌شوند؛ پس خطای یک بخش، بقیه را از بین نمی‌برد. نام `[EMS].[user]`
  براکت‌گذاری شد، بخش `L6` (همهٔ ستون‌های شبه‌رمز در کل دیتابیس) و `L7` (اشیاء با نام
  login/pass/confirm/auth) اضافه شدند.
* ابزار `tools/verify_tsql.py` حالا **چک کلیدواژه‌های رزرو** دارد: هر نام شیئی که
  براکت‌گذاری نشده باشد و کلیدواژهٔ رزرو باشد → FAIL (خودآزما: همان `EMS.user` قدیمی تشخیص داده شد).

## تاریخچهٔ `sql/01_setup_vizitor_user.sql`
* نام دیتابیس به یک متغیر در بالای فایل منتقل شد (`DECLARE @dbName = N'Meelano'`) و
  ساخت کاربر/نقش با dynamic SQL انجام می‌شود تا مستقل از دیتابیسِ بازِ پنجرهٔ SSMS باشد.
* اگر نام دیتابیس اشتباه باشد، فقط یک پیام واضح چاپ می‌شود و **هیچ تغییری روی سرور انجام نمی‌شود**.
* یک بخش جدید `01_LOGINMODE_CHECK` نشان می‌دهد سرور در حالت احراز هویت ترکیبی
  (LoginMode = 2) هست یا نه — چون ورود کاربر SQL بدون آن ممکن نیست.
* عضویت نقش idempotent است و ردیف `02_ROLE_CHECK` صحت آن را نشان می‌دهد.

## فایل‌های جدید پس از اجرای v5 روی سرور (خروجی واقعی رسید)
| فایل | توضیح |
|---|---|
| `docs/VERIFIED-SCHEMA-Meelano.md` | **اسکیمای تأییدشدهٔ واقعی**: سرور، schemaها، ستون‌های جدول‌های کلیدی، پاسخ ۵ مورد VERIFY، تعداد ردیف‌ها، فلؤ نوشتن با SPها |
| `sql/02_fill_gaps.sql` | ممیزی دوم با **خروجی کوچک**: پورت واقعی TCP + LoginMode، روش تأیید رمز (`PWDCOMPARE`)، بدنهٔ توابع کوتاه لاگین، محتوای جدول‌های تنظیمات و نمونه‌داده‌ها، تعریف viewهای مورد نیاز |
| `sql/03_dump_proc_bodies.sql` | بدنهٔ کامل ۴ پروسیجر کلیدی (`add_sail_pish`, `AddInvoice`, `new_cust`, `FixMojodi`) — ترجیحاً با `sqlcmd -o file` تا فایل ضمیمه شود |
| `tools/verify_tsql.py` | ابزار چک استاتیک (parse هر batch، بازسازی SQL داینامیک، توازن پرانتز/BEGIN-END، رد BOM و غیر-ASCII) |

### یافته‌های کلیدی ممیزی واقعی (2026-09-18)
* سرور SQL Server **2014 Enterprise**، instance پیش‌فرض، دیتابیس **Meelano**،
  collation `SQL_Latin1_General_CP1256_CI_AS` (case-insensitive).
* ماژول موبایل در schema جداگانهٔ **`Hamrah`** است (`Visit`, `TabletCustomer`,
  `PishDaryaft*`, `Device*`) و **همه خالی‌اند (0 ردیف)** → تأیید قطعی «اولین استقرار».
* `new_cust` یک **STORED PROCEDURE** است نه جدول (به همین دلیل در 07 به‌عنوان MISSING آمد).
* **لاگین**: `dbo.sys_users.user_password` از نوع `varbinary(50)` است → رمز در اپ
  بازسازی نمی‌شود؛ تأیید سمت SQL با `PWDCOMPARE` انجام می‌شود (اسکریپت 02 این را probe می‌کند).
* هیچ `vwVizitor*` روی سرور وجود ندارد (لایهٔ PHP نصب نشده).
* پروسیجرهای موجود برای مسیر نوشتن: `dbo.add_sail_pish` (۲۵ پارامتر)،
  `dbo.AddInvoice` (۳۹ پارامتر + `@shpish`)، `dbo.FixMojodi`، `dbo.new_cust` (۵۰ پارامتر).
* قیمت‌گذاری تأیید شد: `CUSTOMERS.group_rdf` → `custgroup.price` (تیر ۱..۵) →
  `forosh_price.forosh1..forosh5` (+ `mp*, pv*`).
* جدول‌های `sailfact_pish` / `subsailfact_pish` **صفر ردیف** → هیچ پیش‌فاکتوری ثبت نشده است.

## فاز ۳ — لایهٔ دادهٔ SQL (شروع) + محافظ «بدون نام حدسی»
| فایل | توضیح |
|---|---|
| `MeelanoDataSource.kt` | **جدید** — منبع دادهٔ فقط‌خواندنی روی Meelano: لاگین با `PWDCOMPARE`، کالا + ۵ سطح قیمت، قیمت به‌ازای تیر، موجودی انبارها، مشتریان مجاز (`sys_cus`)، گروه‌های مشتری با تیر قیمت، هویت ویزیتور، اطلاعات دیتابیس — همه با SQL پارامتری و `queryTimeout` |
| `docs/schema/meelano-columns.tsv` | ۲۰ جدول و ۴۴۱ ستون **تأییدشدهٔ** استخراج‌شده از خروجی واقعی ممیزی (ماشین‌خوان) |
| `tools/check_sql_columns.py` | محافظ: هر SQL در فایل‌های Kotlin/‌SQL را با اسکیمای تأییدشده مقایسه می‌کند و هر ستون ناشناخته را FAIL می‌دهد (با تست خودآزما: تزریق ستون جعلی → شناسایی شد) |
| `sql/05_gaps_small.sql` | ممیزی کوچک (خروجی ~۴۰ خط / ۲.۵KB): ستون‌های `sys_kal`/`sys_anb`، LoginMode، معنای واقعی ستون‌های `active`، تست `PWDCOMPARE`، نمونه‌های کوچک |
| `tools/run_audit.bat` | روی سرور با یک دوبار-کلیک همهٔ ممیزی‌ها را در فایل‌های `out_*.txt` می‌نویسد (دور زدنِ بریدگی تب Messages) |

**یافتهٔ قطعی پورت:** `sys.dm_tcp_listener_states` → `0.0.0.0:1433` و `[::]:1433` (ONLINE).
آن `1434` که در رجیستری دیده شد مربوط به **DAC** (`AdminConnection\Tcp`) است، نه اپ.
پس اتصال اپ: **`192.168.1.150:1433` → دیتابیس `Meelano`**.

## یافته‌های مهم ممیزی بخش ۵ (۲۰۲۶-۰۹-۱۸)
* **باگ واقعی در کد:** ستون‌های `char(1)` این ERP مقدار **`'t'`** دارند نه `'1'`.
  فیلتر `active = '1'` بی‌صدا صفر ردیف برمی‌گرداند. اصلاح شد و محافظ
  `tools/check_sql_columns.py` حالا مقادیر ثابت را هم با
  `docs/schema/meelano-values.tsv` تطبیق می‌دهد (`ACTIVE_CHAR = "t"`).
* **اتصال کاربر به ویزیتور از `sys_vis` است، نه `visitors.UserID`** (که NULL است):
  `sys_users.user_id → sys_vis.UserID → sys_vis.shvis → visitors.vis_rdf`.
* ستون‌های `sys_kal`/`sys_anb`/`sys_use`/`sys_wor`/`systems` تأیید و به فایل اسکیما اضافه شدند.
* دادهٔ واقعی: ۹ گروه مشتری (همه با تیر قیمت ۱)، ۷ مشتری، ۱ ویزیتور، ۱ انبار، ۱ مسیر.
* **ورود هنوز حل نشده:** `DATALENGTH(user_password) = 1` بایت → هش SQL Server نیست و
  `PWDCOMPARE` منطقی نیست. اسکریپت `sql/06_login_probe.sql` شواهد لازم را جمع می‌کند
  (طبقه‌بندی آن بایت، جدول‌های کاندید رمز، و بدنهٔ توابع `SetUserpass`/`GetUser`).

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
