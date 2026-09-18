/* ═══════════════════════════════════════════════════════════════════════════
   Vizitor — مرحله ۱: آماده‌سازی سرور (غیرتلفیقی، فقط در صورت نبود)
   ═══════════════════════════════════════════════════════════════════════════
   اجرا با حساب مدیر (sa یا sysadmin) در SSMS، روی سرور 192.168.1.150.
   هیچ چیزی حذف یا تغییر نمی‌کند؛ فقط آیتم‌های کم‌موجود ساخته می‌شوند.

   ۱) یک کاربر SQL جداگانه برای اپ Vizitor می‌سازد (vizitor_android) —
      هرگز از AdminAn برای اپ استفاده نکنید (اصل کمترین دسترسی).
   ۲) دسترسی فقط‌خواندنی روی دیتابیس (db_datareader).
      دسترسی نوشتن (ثبت پیش‌فاکتور) پس از مرحله audit و تأیید جداول/SPها
      و فقط روی همان جداول معین، اضافه می‌شود.
   ۳) قانون فایروال فقط برای شبکهٔ داخلی (192.168.1.0/24) — نه اینترنت!
   ═══════════════════════════════════════════════════════════════════════════ */

/* ── ۱) LOGIN سروری (فقط اگر نباشد) ─────────────────────────────────────── */
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'vizitor_android')
BEGIN
    CREATE LOGIN vizitor_android
        WITH PASSWORD = 'CHANGE_THIS_TO_A_STRONG_PASSWORD!2024',
             DEFAULT_DATABASE = [Atiran2],
             CHECK_POLICY = ON,
             CHECK_EXPIRATION = OFF;
END
GO

/* ── ۲) ادامهٔ کار داخل دیتابیس Atiran2 ────────────────────────────────────
   مهم: اگر این خط نباشد و اتصال شما روی master باز باشد، کاربر اشتباهاً
   در master ساخته می‌شود. اگر دیتابیس نباشد، خودِ همین خط با پیام واضح
   خطا می‌دهد (چیزی خراب نمی‌شود). */
USE [Atiran2];
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'vizitor_android')
    CREATE USER vizitor_android FOR LOGIN vizitor_android;
GO

/* عضویت در نقش فقط‌خواندنی — idempotent: اجرای دوبارهٔ اسکریپت خطا نمی‌دهد */
IF NOT EXISTS (SELECT 1
                 FROM sys.database_role_members rm
                 JOIN sys.database_principals rp ON rp.principal_id = rm.role_principal_id
                 JOIN sys.database_principals mp ON mp.principal_id = rm.member_principal_id
                WHERE rp.name = N'db_datareader'
                  AND mp.name = N'vizitor_android')
    ALTER ROLE db_datareader ADD MEMBER vizitor_android;
GO

/* ── ۳) تأیید نهایی: باید یک ردیف با role_name = db_datareader ببینید ───── */
SELECT '02_ROLE_CHECK' AS section,
       DB_NAME() AS database_name,
       mp.name   AS member_name,
       rp.name   AS role_name
FROM sys.database_role_members rm
JOIN sys.database_principals rp ON rp.principal_id = rm.role_principal_id
JOIN sys.database_principals mp ON mp.principal_id = rm.member_principal_id
WHERE mp.name = N'vizitor_android';
GO

/* پیام یادآوری (در نتیجهٔ کوئری نمایش داده می‌شود):
   ★ پس از اجرا، رمز را با ALTER LOGIN vizitor_android WITH PASSWORD = '...'
     عوض کنید و همان رمز جدید را در اپ وارد کنید. رمز AdminAn را دست نزنید. */
SELECT 'DONE: login vizitor_android ready (read-only on Atiran2). Change its password now.' AS note;

/* ═══════════════════════════════════════════════════════════════════════════
   ۴) فایروال ویندوز — از CMD/PowerShell روی سرور (بدون آسیب، idempotent):
   ═══════════════════════════════════════════════════════════════════════════
   NETSH ADVFIREWALL FIREWALL DELETE RULE NAME="Vizitor SQL (LAN)"
   NETSH ADVFIREWALL FIREWALL ADD RULE NAME="Vizitor SQL (LAN)" ^
       DIR=IN ACTION=ALLOW PROTOCOL=TCP LOCALPORT=1433 REMOTEIP=192.168.1.0/24

   ⚠️ نکتهٔ مهم: اگر SQL Server «اینستنس نامدار» (Named Instance) است،
   پورت 1433 ممکن نیست گوش بدهد. برای فهمیدن پورت واقعی، در CMD:
       NETSTAT -ANO | FINDSTR LISTENING | FINDSTR 143
   اگر هیچ خط 1433 ندیدید: SQL Server Configuration Manager →
   SQL Server Network Configuration → Protocols for <instance> →
   TCP/IP → Enabled → Properties → IP Addresses → IPAll → TCP Port = 1433
   (سپس Services را ری‌استارت کنید). این یک تغییر ایمن است — فقط پورت
   گوش‌دادن این اینستنس را روی 1433 ثابت می‌کند و چیزی از بین نمی‌رود.
   ═══════════════════════════════════════════════════════════════════════════ */
