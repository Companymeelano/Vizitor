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

/* ── ۲) USER داخل دیتابیس + نقش فقط‌خواندنی ──────────────────────────────── */
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'vizitor_android')
    CREATE USER vizitor_android FOR LOGIN vizitor_android;
GO

ALTER ROLE db_datareader ADD MEMBER vizitor_android;
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

   ⚠️ نکتهٔ مهم: اگر SQL Server «ایнсنس نامدار» (Named Instance) است،
   پورت 1433 ممکن نیست گوش بدهد. برای فهمیدن پورت واقعی، در CMD:
       NETSTAT -ANO | FINDSTR LISTENING | FINDSTR 143
   اگر هیچ خط 1433 ندیدید: SQL Server Configuration Manager →
   SQL Server Network Configuration → Protocols for <instance> →
   TCP/IP → Enabled → Properties → IP Addresses → IPAll → TCP Port = 1433
   (سپس Services را ری‌استارت کنید). این یک تغییر ایمن است — فقط پورت
   گوش‌دادن این اینستنس را روی 1433 ثابت می‌کند و چیزی از بین نمی‌رود.
   ═══════════════════════════════════════════════════════════════════════════ */
