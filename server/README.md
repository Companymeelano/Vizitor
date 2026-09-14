# Developed by Milano Technical Team, Milad Yaghoobi

## نصب وب‌سرویس آتیران ویزیتور (PHP 8.3 + SQL Server)

### ۱) پیش‌نیازها
- **PHP 8.3** با یکی از درایورهای زیر:
  - ویندوز: `pdo_sqlsrv` (نصب با `pecl install sqlsrv` یا فعال‌سازی `php_sqlsrv.dll` در `php.ini`)
  - لینوکس: `pdo_dblib` + فری‌تی‌دی‌اس:
    ```bash
    sudo apt install freetds-dev php8.3-dev
    sudo pecl install pdo_dblib
    echo "extension=pdo_dblib.so" | sudo tee /etc/php/8.3/mods-available/pdo_dblib.ini
    sudo phpenmod pdo_dblib
    ```
- دسترسی شبکه از وب‌سرور به **پورت 1433** سرور SQL.

### ۲) استقرار فایل‌ها
```
/var/www/vizitor/
├── api.php
└── config.php
```
> فقط همین پوشه را زیر دامنه/ساب‌دامنه اختصاصی قرار دهید و دسترسی نوشتن را بگیرید.

### ۳) پیکربندی
در `config.php` مقادیر زیر را تغییر دهید:
- `DB_HOST`, `DB_PORT` (پیش‌فرض 1433), `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `API_KEY` — باید دقیقاً با کلید واردشده در اپ (تب گزارشات ← پیکربندی سرور) یکسان باشد.
- نام جداول (`TBL_*`) را در صورت تفاوت با دیتابیس آتیران تنظیم کنید.

### ۴) کاربر دیتابیس (اصل کمترین دسترسی)
```sql
CREATE LOGIN vizitor_api WITH PASSWORD = 'CHANGE_ME_STRONG_PASSWORD';
USE AtiranAccounting;
CREATE USER vizitor_api FOR LOGIN vizitor_api;
GRANT SELECT ON dbo.Products   TO vizitor_api;
GRANT SELECT ON dbo.CUSTOMERS  TO vizitor_api;
GRANT SELECT ON dbo.CustGroup  TO vizitor_api;
GRANT SELECT ON dbo.sal_mali   TO vizitor_api;
GRANT SELECT, INSERT, UPDATE ON dbo.SalesHeader TO vizitor_api;
GRANT SELECT, INSERT ON dbo.SalesLines TO vizitor_api;
GRANT UPDATE ON dbo.Products   TO vizitor_api; -- کاهش موجودی
```

### ۵) تست سریع
```bash
curl -H "X-Api-Key: ATIRAN-CHANGE-ME" "http://SERVER:8080/vizitor/api.php?action=ping"
```
