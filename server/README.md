# Developed by Milano Technical Team, Milad Yaghoobi

## راهنمای استقرار وب‌سرویس ویزیتور روی سرور میلانو (Windows Server + IIS)

### راه‌سرور واقعی
- وب‌سرویس: `http://37.143.148.14:8731/server/api.php`
- دیتابیس: SQL Server روی همان ماشین — دیتابیس `Meelano` با کاربر `AdminAn`

---

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
