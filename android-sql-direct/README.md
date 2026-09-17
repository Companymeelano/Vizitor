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
| `sql/00_audit_atiran2.sql` | اسکریت **فقط‌خواندنی** برای اجرا روی سرور واقعی (خروجی را به agent بفرستید) |
| `sql/01_setup_vizitor_user.sql` | آماده‌سازی غیرتلفیقی سرور: کاربر فقط‌خواندنی `vizitor_android` + فایروال فقط LAN |
| `SqlConnectionManager.kt` | فاز ۲: pool + validation + retry + transaction + StateFlow + پیام فارسی خطا |
| `SecureDbStore.kt` | فاز ۲: ذخیرهٔ امن اطلاعات اتصال (AES-GCM + Android Keystore) |

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
