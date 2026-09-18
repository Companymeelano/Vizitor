چطور این ممیزی‌ها را اجرا کنیم (Vizitor / Meelano)
==================================================

این پوشه باید شامل این فایل‌ها باشد (اگر پوشه وجود ندارد، اول بسازش:
    New-Item -ItemType Directory -Force C:\vizitor_audit):
    run_audit.bat
    02_fill_gaps.sql
    03_dump_proc_bodies.sql
    03b_bodies_file.sql
    04_port_check.sql
    05_gaps_small.sql
    08_which_db.sql

راه ۱ (پیشنهادی — همه‌چیز با یک بار):
    ۱) فایل‌ها را در یک پوشه بگذار، مثلا  C:\vizitor_audit\
    ۲) در File Explorer روی  run_audit.bat  راست‌کلیک کن → "Run as administrator"
    ۳) صبر کن تا تمام شود؛ در همین پوشه این فایل‌ها ساخته می‌شوند:
         out_02_gaps.txt ، out_03_bodies.txt ، out_04_port.txt ، out_05_small.txt
    ۴) همان چهار فایل .txt را بفرست.

    ⚠️ مهم: متن فایل run_audit.bat را داخل PowerShell یا SSMS کپی نکن.
       این فایل باید اجرا شود (دابل‌کلیک یا Run as administrator)، نه کپی.
       اگر متنش را در PowerShell بچسبانی، خطاهایی مثل
       «REM: The term 'REM' is not recognized» می‌گیری: بی‌خطرند ولی هیچ کاری
       هم انجام نمی‌دهند.

راه ۲ (اگر run_audit.bat گفت sqlcmd پیدا نشد — بدون نیاز به sqlcmd):
    ۱) در SSMS فایل  03b_bodies_file.sql  را باز کن (روی دیتابیس Meelano)
    ۲) *قبل از* F5 کلیدهای  Ctrl+Shift+F  را بزن
       (Query → Results To → Results to File)
    ۳) F5 را بزن؛ SSMS نام فایل می‌پرسد →  out_03_bodies.txt  بگذار
    ۴) همین کار را برای  05_gaps_small.sql  تکرار کن →  out_05_small.txt

راه ۳ (اگر با همان PowerShell راحت‌تری — این‌ها دستور واقعی‌اند و کپی‌شان اشکالی ندارد):
    Get-Command sqlcmd                 # اول ببین sqlcmd نصب است یا نه
    cd C:\vizitor_audit
    sqlcmd -S localhost -d Meelano -E -i 03_dump_proc_bodies.sql -o out_03_bodies.txt -y 0 -W
    sqlcmd -S localhost -d Meelano -E -i 05_gaps_small.sql       -o out_05_small.txt  -y 0 -W
    sqlcmd -S localhost -d Meelano -E -i 02_fill_gaps.sql        -o out_02_gaps.txt   -y 0 -W

هیچ‌کدام از این اسکریپت‌ها چیزی در دیتابیس نمی‌نویسند (فقط خواندن).
فایل‌های .sql را فقط در SSMS اجرا کن.

راه ۴ (کاملاً بدون فایل — وقتی نه پوشه داری و نه فایل sql؛ فقط sqlcmd نصب است)
    اول ببین sqlcmd هست:      Get-Command sqlcmd
    بعد این یک خط را کپی کن (کل خط را یکجا؛ پوشه را می‌سازد و برای هر پروسیجر
    یک فایل جدا می‌نویسد):

    New-Item -ItemType Directory -Force C:\vizitor_audit | Out-Null; foreach ($p in 'add_sail_pish','AddInvoice','new_cust','FixMojodi') { sqlcmd -S localhost -d Meelano -E -h -1 -Q "SET NOCOUNT ON; SELECT m.definition FROM sys.sql_modules m JOIN sys.objects o ON o.object_id = m.object_id WHERE o.name = '$p'" -o "C:\vizitor_audit\body_$p.txt" -y 0 -W }

    نتیجه:
        C:\vizitor_audit\body_add_sail_pish.txt
        C:\vizitor_audit\body_AddInvoice.txt
        C:\vizitor_audit\body_new_cust.txt
        C:\vizitor_audit\body_FixMojodi.txt
    کنترل کن که خالی نباشند:
        Get-ChildItem C:\vizitor_audit | Select-Object Name, Length

    نکته: خطای  Sqlcmd: 'xxx.sql': Invalid filename  یعنی sqlcmd فایل را در
    «پوشهٔ جاری» می‌گردد (مثلاً C:\Windows\System32). یا اول  cd  کن به پوشهٔ
    فایل‌ها، یا مسیر کامل بده، یا از همین راه ۴ استفاده کن که فایل لازم ندارد.

راه ۵ (اسکریپت 08 — تعیین این‌که کدام دیتابیس «زنده» است؛ خیلی مهم)
    آن ستون‌هایی که از سه جدول فرستادی از دیتابیس  Atiran14050603  بود، ولی همهٔ
    بررسی‌های قبلی (ورود، تعداد رکوردها، بدنهٔ پروسیجرها) در دیتابیس  Meelano  انجام
    شده. اپ باید همان جایی بنویسد که خود ERP می‌نویسد؛ پس این باید با شاهد روشن شود.

    روش:
    ۱) 08_which_db.sql  را در SSMS باز کن.
    ۲) در نوار بالا («Available Databases») دیتابیس  Meelano  را انتخاب کن و F5.
    ۳) خروجی را بفرست (چند جدول کوچک است).
    ۴) بعد همان فایل را با دیتابیس  Atiran14050603  انتخاب‌شده هم اجرا کن و بفرست.
    ستون  last_write  نشان می‌دهد آخرین بار چه زمانی در آن دیتابیس نوشته شده؛
    دیتابیس زنده تاریخ امروز را دارد.
