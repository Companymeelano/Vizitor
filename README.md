# Vizitor — سامانه مدیریت بازدیدکنندگان

**Vizitor** یک سامانهٔ سروری برای ثبت و مدیریت بازدیدکنندگان است که با **برنامهٔ اندروید** همراه کار می‌کند. این مخزن شامل نصب‌کنندهٔ هوشمند سرور، API، و دیتابیس است.

## اجزای پروژه

| مسیر | توضیح |
|---|---|
| `install.sh` | **نصب‌کنندهٔ هوشمند (Linux)** — تشخیص خودکار سرور، پرسش اطلاعات ضروری، نصب، بازرسی و تعمیر خودکار |
| `install.ps1` | **نصب‌کنندهٔ هوشمند (Windows)** — همان رویه برای ویندوز با پشتیبانی SQL Server |
| `api/server.py` | سرور API (فقط کتابخانهٔ استاندارد Python 3) |
| `api/db.py` | لایهٔ دیتابیس (SQLite یا MySQL/MariaDB یا **Microsoft SQL Server**) |
| `api/seed.py` | درج اولیهٔ داده‌ها (ادمین، کد فعال‌سازی، تنظیمات) |
| `api/provision_sqlserver.py` | آماده‌سازی **غیرتلفیقی** SQL Server (فقط CREATE در صورت نبود؛ بدون DROP/ALTER) |
| `database/schema_mysql.sql` | اسکیم دیتابیس MySQL/MariaDB |
| `database/schema_sqlite.sql` | اسکیم دیتابیس SQLite |
| `database/schema_sqlserver.sql` | اسکیم دیتابیس Microsoft SQL Server (T-SQL، ای‌دی‌ام‌پتنت) |
| `INSTALL.md` | راهنمای کامل نصب و پیکربندی (Linux + Windows) |

## نصب سریع

**Linux:**

```bash
# روی سرور، این پوشه را کپی کنید و:
sudo bash install.sh
```

**Windows (با Microsoft SQL Server):**

```powershell
# روی سرور ویندوزی، در PowerShell با دسترسی Administrator:
powershell -ExecutionPolicy Bypass -File install.ps1
```

نصب‌کننده به‌طور خودکار:
- ✅ سیستم‌عامل، پکیج‌منیجر، Python و دیتابیس را تشخیص می‌دهد و کمبودها را نصب می‌کند
- ✅ **آی‌پی سرور (عمومی + داخلی)** را شناسایی و آدرس API را بر اساس آن می‌سازد
- ✅ از شما می‌پرسد: **آدرس/IP، پورت، مشخصات دیتابیس، حساب ادمین، کد فعال‌سازی**
- ✅ همهٔ موارد را در `config.json` جایگذاری و سرویس را راه‌اندازی می‌کند
- ✅ سپس **همه‌چیز را چک** می‌کند (API، دیتابیس، فعال‌سازی، لاگین) و در صورت مشکل،
  **تنظیمات را بازبینی/تعمیر کرده و دوباره چک** می‌کند (تا ۴ دور)
- ✅ در پایان، **آدرس دقیق API برای ورود در برنامهٔ اندروید** را نمایش می‌دهد

## اتصال برنامهٔ اندروید

در بخش «اتصال API» برنامهٔ اندروید، آدرسی که نصب‌کننده در انتها نمایش داده را وارد کنید:

```
http://آدرس-سرور:پورت/api
```

برنامه می‌تواند با دریافت `http://آدرس-سرور:پورت/api/config` تنظیمات و وضعیت فعال‌سازی را خودکار بخواند.

## API (خلاصه)

| روش | مسیر | توضیح |
|---|---|---|
| GET | `/api/ping` | زنده بودن (بدون احراز) |
| GET | `/api/config` | تنظیمات عمومی برای اپ (آدرس، وضعیت فعال‌سازی) |
| GET | `/api/health` | گزارش کامل سلامت (دیتابیس، فعال‌سازی، uptime) |
| POST | `/api/activate` | فعال‌سازی با کد `{"code": "..."}` |
| POST | `/api/login` | ورود ادمین `{"username","password"}` → توکن |
| GET/POST | `/api/visitors` | فهرست / ثبت بازدیدکننده (با توکن `Authorization: Bearer ...`) |

## حالت‌های نصب‌کننده

**Linux:**

```bash
sudo bash install.sh                 # نصب تعاملی (پیشنهادی)
sudo bash install.sh --auto          # بدون سؤال، با مقادیر هوشمند
sudo bash install.sh --recheck       # فقط بازرسی و تعمیر خودکار نصب موجود
sudo bash install.sh --port 9000     # اجبار پورت
sudo bash install.sh --ip 1.2.3.4    # اجبار آدرس
```

**Windows:**

```powershell
powershell -ExecutionPolicy Bypass -File install.ps1           # نصب تعاملی (پیشنهادی)
powershell -ExecutionPolicy Bypass -File install.ps1 -Auto     # بدون سؤال، با مقادیر هوشمند
powershell -ExecutionPolicy Bypass -File install.ps1 -Recheck  # فقط بازرسی و تعمیر خودکار
powershell -ExecutionPolicy Bypass -File install.ps1 -Port 9000 -Ip 1.2.3.4
```

## پشتیبانی Microsoft SQL Server (بدون آسیب)

نصب‌کنندهٔ ویندوز می‌تواند موتور دیتابیس را روی **SQL Server** قرار دهد. دسترسی به دیتابیس
**کاملاً دقیق و غیرتلفیقی** است:

- 🛡️ فقط `CREATE` در صورت **نبود** شی (دیتابیس، لاگین، یوزر، جداول) — هیچ `DROP` یا `ALTER` روی شی موجود
- 🛡️ اگر لاگین قبلاً وجود داشته باشد، **گذرواژه‌اش دست‌نخورده می‌ماند**
- 🛡️ اختیارات فقط روی همان دیتابیس هدف: `db_datareader` + `db_datawriter` + `db_ddladmin`
- 🛡️ تغییر **هیچ** تنظیم سراسری SQL Server (پورت، sa، دیتابیس‌های دیگر، ...) انجام نمی‌شود
- 🔄 اسکریپت ای‌دی‌ام‌پتنت است: اجرای مکرر آن بی‌خطر است

جزئیات کامل، عیب‌یابی و مدیریت سرویس در [INSTALL.md](INSTALL.md) آمده است.
