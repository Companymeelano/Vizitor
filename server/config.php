<?php
/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | پیکربندی سرور (config.php)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  اتصال به دیتابیس حسابداری آتیران روی SQL Server — پورت 1433
 * ═══════════════════════════════════════════════════════════════════════════
 */

declare(strict_types=1);

// ── تنظیمات اتصال SQL Server ────────────────────────────────────────────────
const DB_HOST     = '37.143.148.14';    // سرور اصلی میلانو (SQL Server روی همین ماشین است؛ localhost همکار می‌کند)
const DB_PORT     = 1433;               // پورت استاندارد SQL Server
const DB_NAME     = 'Meelano';          // دیتابیس میلانو
const DB_USER     = 'AdminAn';           // کاربر SQL Server میلانو
const DB_PASSWORD = 'St@R2022$';       // ⚠️ محرمانه — دسترسی فایل را محدود کنید (IIS: جلوگیری از دانلود config.php)

// ── کلید احراز هویت اپلیکیشن (باید با کلید داخل اپ یکسان باشد) ────────────
const API_KEY = 'MILANO-VIZITOR-2026';

// ── نام جداول دیتابیس حسابداری آتیران ─────────────────────────────────────
const TBL_PRODUCTS     = 'Products';     // کالای کاتالوگ
const TBL_CUSTOMERS    = 'CUSTOMERS';    // مشتریان
const TBL_CUST_GROUP   = 'CustGroup';    // گروه‌های مشتری
const TBL_SALES_HEADER = 'SalesHeader';  // هدر فاکتور
const TBL_SALES_LINES  = 'SalesLines';   // اقلام فاکتور
const TBL_SAL_MALI     = 'sal_mali';     // تاریخچه فروش سال مالی

// ── کد ویزیتور/بازاریاب پیش‌فرض برای درج در فاکتورها ──────────────────────
const SALESMAN_CODE = 'VST-001';

/**
 * ساخت PDO برای SQL Server — پشتیبانی از درایورهای:
 *  1) pdo_sqlsrv  (ویندوز / رسمی مایکروسافت)
 *  2) pdo_dblib   (لینوکس با FreeTDS)
 */
function create_pdo(): PDO
{
    $host = DB_HOST . ':' . DB_PORT;

    if (extension_loaded('pdo_sqlsrv')) {
        $dsn = sprintf(
            'sqlsrv:Server=%s,%d;Database=%s;ConnectionPooling=1;Encrypt=0;TrustServerCertificate=1',
            DB_HOST, DB_PORT, DB_NAME
        );
    } elseif (extension_loaded('pdo_dblib')) {
        $dsn = sprintf('dblib:host=%s;dbname=%s;charset=UTF-8', $host, DB_NAME);
    } else {
        throw new RuntimeException(
            'هیچ درایور SQL Server نصب نیست. یکی از افزونه‌های pdo_sqlsrv یا pdo_dblib را نصب کنید.'
        );
    }

    $pdo = new PDO($dsn, DB_USER, DB_PASSWORD, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);

    if (!extension_loaded('pdo_sqlsrv')) {
        $pdo->exec("SET QUOTED_IDENTIFIER ON");
    }

    return $pdo;
}
