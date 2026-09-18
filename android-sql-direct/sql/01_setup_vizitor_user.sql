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

/* ── ۰) نام دیتابیس ERP ───────────────────────────────────────────────────
   ↓↓↓ فقط اگر نام دیتابیس روی سرور شما چیز دیگری است، همین یک خط را عوض کنید.
   (طبق خروجی ممیزی، نام فعلی روی سرور «Meelano» است.) */
DECLARE @dbName SYSNAME = N'Meelano';

IF DB_ID(@dbName) IS NULL
    PRINT N'*** database [' + @dbName + N'] was NOT found on this server.'
        + N' Set the correct name in the DECLARE @dbName line at the top of this file'
        + N' and run the script again. No change was made on the server. ***';
GO

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

/* ── ۱ب) LOGIN باید در حالت احراز هویت ترکیبی مجاز باشد ────────────────────
   اگر LoginMode = 1 باشد (فقط Windows)، ورود کاربر SQL ممکن نیست؛ نسخهٔ
   فعلی مقدار را فقط گزارش می‌کند و چیزی را تغییر نمی‌دهد. */
SELECT '01_LOGINMODE_CHECK' AS section,
       CAST(value_data AS NVARCHAR(16)) AS login_mode,
       CASE WHEN CAST(value_data AS NVARCHAR(16)) = N'2'
            THEN N'OK: mixed mode (SQL logins allowed)'
            ELSE N'ATTENTION: only Windows authentication is enabled -> enable mixed mode in SSMS > Server Properties > Security' END AS note
FROM sys.dm_server_registry
WHERE value_name = N'LoginMode';
GO

/* ── ۲) کاربر دیتابیس + نقش فقط‌خواندنی، داخل دیتابیس ERP ─────────────────
   از dynamic SQL استفاده می‌شود تا هر دیتابیسی که در کادر بالای فایل
   نوشته شده، مستقیماً هدف قرار گیرد — دیگر مهم نیست پنجرهٔ SSMS روی
   master باز است یا روی خود دیتابیس. همهٔ کارها idempotent است. */
DECLARE @dbName SYSNAME = N'Meelano';   -- ← فقط این خط (اگر نام دیتابیس فرق دارد)

IF DB_ID(@dbName) IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(MAX) =
        N'USE ' + QUOTENAME(@dbName) + N';
          IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N''vizitor_android'')
              CREATE USER vizitor_android FOR LOGIN vizitor_android;
          IF NOT EXISTS (SELECT 1
                           FROM sys.database_role_members rm
                           JOIN sys.database_principals rp ON rp.principal_id = rm.role_principal_id
                           JOIN sys.database_principals mp ON mp.principal_id = rm.member_principal_id
                          WHERE rp.name = N''db_datareader''
                            AND mp.name = N''vizitor_android'')
              ALTER ROLE db_datareader ADD MEMBER vizitor_android;
          SELECT N''02_ROLE_CHECK'' AS section, DB_NAME() AS database_name,
                 mp.name AS member_name, rp.name AS role_name
            FROM sys.database_role_members rm
            JOIN sys.database_principals rp ON rp.principal_id = rm.role_principal_id
            JOIN sys.database_principals mp ON mp.principal_id = rm.member_principal_id
           WHERE mp.name = N''vizitor_android'';';
    BEGIN TRY
        EXEC (@sql);
    END TRY
    BEGIN CATCH
        PRINT N'Creating the database user/role FAILED: ' + ERROR_MESSAGE();
    END CATCH
END
ELSE
    PRINT N'Database [' + @dbName + N'] was not found, so part 2 was skipped.'
        + N' Set the correct name in DECLARE @dbName.';
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
