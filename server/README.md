# Developed by Milano Technical Team, Milad Yaghoobi

## راهنمای استقرار وب‌سرویس ویزیتور روی سرور میلانو (Windows Server + IIS)

### راه‌سرور واقعی
- وب‌سرویس: `http://37.143.148.14:8731/server/api.php`
- دیتابیس: SQL Server روی همان ماشین — دیتابیس `Meelano` با کاربر `AdminAn`

---

## ⚡ راه‌اندازی خودکار با یک دستور (توصیه‌شده) — Setup-VizitorServer.ps1

اسکریپت **`Setup-VizitorServer.ps1`** (همین پوشه) تمام ۱۲ بخش زیر را خودش **بررسی** می‌کند؛ هر بخش سالم ← **تیک سبز** و عبور، هر بخش معیوب ← **اصلاح خودکار** و سپس تیک:

1. فایل‌های `api.php` / `config.php` (خواندن خودکار کاربر/رمز/کلید از config.php)
2. نصب/فعال‌سازی **IIS + FastCGI/CGI**
3. نصب/بررسی **PHP 8.3 NTS** (دانلود خودکار از windows.php.net)
4. نصب **Microsoft ODBC Driver 17/18** (با winget)
5. نصب افزونه‌های **php_sqlsrv / php_pdo_sqlsrv** (دانلود خودکار متناسب با نسخه PHP از گیت‌هاب مایکروسافت)
6. ساخت/اصلاح **سایت IIS روی پورت 8731** + کپی فایل‌ها در `server/` + ثبت FastCGI + مسدودسازی دانلود `config.php` (web.config)
7. قانون **فایروال** برای پورت 8731
8. SQL Server: فعال‌سازی **TCP/IP** روی پورت 1433
9. SQL Server: فعال‌سازی **Mixed Mode Authentication**
10. تست ورود به `Meelano` با کاربر config (در صورت نیاز، ساخت/فعال‌سازی لاگین به‌صورت خودکار)
11. **تست سرتاسری**: ping با کلید ✔ / رد درخواست بدون کلید (۴۰۱) ✔ / مسدود بودن config.php ✔ + گزارش تعداد رکورد جداول
12. بررسی دسترس‌پذیری `http://37.143.148.14:8731` از بیرون

### نحوه اجرا (روی خود سرور 37.143.148.14)
```powershell
# روش ۱: راست‌کلیک روی Setup-VizitorServer.ps1 ← Run with PowerShell (خودش مدیر می‌شود)

# روش ۲: پاورشل Administrator
powershell -ExecutionPolicy Bypass -File .\Setup-VizitorServer.ps1

# با پارامترهای سفارشی:
.\Setup-VizitorServer.ps1 -WebPort 8731 -SiteRoot C:\inetpub\VizitorAPI -PhpRoot C:\php\php-8.3-nts

# اجرای آفلاین (بدون دانلود اینترنتی):
.\Setup-VizitorServer.ps1 -SkipDownloads
```
- اسکریپت **ایمن و قابل اجرای مجدد** است؛ خروجی کامل در `Setup-VizitorServer.log` ذخیره می‌شود.

### ⚠️ درباره ری‌استارت سرویس SQL (مهم برای ساعت کاری!)
اگر تنظیمات SQL (TCP/IP یا Mixed Mode) نیاز به تغییر داشته باشد، سرویس SQL Server باید یک‌بار ری‌استارت شود:
- اسکریپت **قبل از ری‌استارت از شما سؤال می‌گیرد** ([y/N]) و سرویس‌های وابسته (SQL Agent، سرویس‌های حسابداری) را به‌خاطر می‌سپرد و **دوباره بالا می‌آورد**.
- در ساعت اوج کاری: `.\Setup-VizitorServer.ps1 -NoSqlRestart` اجرا کنید و در فرصت مناسب خودتان از services.msc سرویس SQL Server را Restart کنید.
- اجرای مجدد اسکریپت پس از اعمال تنظیمات، **دیگر ری‌استارتی نمی‌خواهد** و فقط تیک می‌زند.

### 🚑 اگر سرویس SQL استپ ماند (بازیابی فوری)
```powershell
Start-Service MSSQLSERVER
Start-Service SQLSERVERAGENT   # اگر نصب است
Get-Service *SQL* | Format-Table Name, Status
Test-NetConnection 127.0.0.1 -Port 1433
```
سپس نرم‌افزار حسابداری را دوباره باز کنید؛ کلاینت‌ها خودکار وصل می‌شوند.

- پایان اجرا جدول خلاصه با تیک/ضربدر هر بخش + تنظیمات دقیق موردنیاز اپ نمایش داده می‌شود.
- اسکریپت باید **روی خود سرور میلانو** و روی ویندوز ۱۱/سرور اجرا شود (پاورشل ۵.۱ به بالا).

---

## 🛠 استقرار دستی (اگر اسکریپت را اجرا نکردید)

### ۱) نصب PHP 8.3 روی IIS
1. دانلود **PHP 8.3 (NTS x64)** از windows.php.net — استخراج در `C:\php\`
2. نصب **Microsoft Drivers for PHP for SQL Server (v5.12+)** و قراردادن `php_sqlsrv.dll` و `php_pdo_sqlsrv.dll` در `C:\php\ext\`
3. در `C:\php\php.ini` فعال کنید:
   ```ini
   extension_dir = "C:\php\ext"
   extension=php_sqlsrv.dll
   extension=php_pdo_sqlsrv.dll
   ```
4. در IIS ← Handler Mappings ← `*.php` را به همین PHP map کنید (FastCGI).

### ۲) مشاهده تست
قبل از گذاشتن فایل‌ها:
```
C:\php\php.exe -m | findstr sqlsrv
```
پاسخ `pdo_sqlsrv` یعنی درایور سالم است.

### ۳) استقرار فایل‌ها
این دو فایل را در ریشه سایت IIS که به پورت 8731 متصل است قرار دهید:
```
C:\inetpub\wwwroot\ (یا پوشهٔ اختصاصی سایت ویزیتور)
├── api.php
└── config.php
```
⚠️ در IIS تنظیم کنید که `config.php` قابل دانلود نباشد.
(Request Filtering ← فایل config.php ← Deny)

### ۴) فایروال ویندوز
پورت **8731** را باز کنید (Inbound Rules ← New Rule ← TCP:8731 ← Allow).
پورت 1433 الزاماً **فقط داخل سرور** فعال می‌شود (بیرون از شبکه بسته بماند).

### ۵) تنظیم SQL Server (در همان سرور)
1. SQL Server Configuration Manager ← SQL Server Network Configuration ←
   **TCP/IP = Enabled** (IPAll: TCP Port = 1433) ← سرویس SQL Server را restart کنید.
2. رو‌ش Authentication را **Mixed Mode** کنید (از روی SSMS ← Server Properties ← Security).
3. صحت کاربر `AdminAn` با رمز `St@R2022$` (یا کاربر برنامه مد نظر) را بررسی کنید.

### ۶) تست نهایی (از هر دستگاه روی شبکه)
```bash
# تست اتصال
curl -H "X-Api-Key: MILANO-VIZITOR-2026" "http://37.143.148.14:8731/server/api.php?action=ping"

# کاتالوگ
curl -H "X-Api-Key: MILANO-VIZITOR-2026" "http://37.143.148.14:8731/server/api.php?action=catalog" | head

# مشتریان
curl -H "X-Api-Key: MILANO-VIZITOR-2026" "http://37.143.148.14:8731/server/api.php?action=customers" | head
```

### ۷) خطاهای رایج و راه‌حل
| کد | پیام | راه‌حل |
|---|---|---|
| 401 | کلید API نامعتبر | `API_KEY` در `config.php` باید دقیقاً `MILANO-VIZITOR-2026` و مطابق تنظیمات اپ باشد |
| 500 | هیچ درایور SQL Server نصب نیست | مرحله ۱ و ۲ (نصب php_sqlsrv) |
| — | Connect failed / timeout | مرحله ۵.۱ (فعال‌سی TCP/IP در SQL Server) و فایروال ویندوز برای 8731 |
| 403 | اعتبار مشتری قرمز | مشتری در دیتابیس `CreditOk=0` است |

### ۸) امنیت (مرحلهٔ بعد)
پس از پایان تست، یک کاربر SQL کم‌صلاحیت بسازید (`README` نسخه قدیمی راهنمای دقیقش را دارد)
و `config.php` را از حالت AdminAn به آن کاربر تغییر دهید تا حداقل دسترسی صادر گردد.
