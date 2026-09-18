چطور این ممیزی‌ها را اجرا کنیم (Vizitor / Meelano)
==================================================

این پوشه باید شامل این فایل‌ها باشد:
    run_audit.bat
    02_fill_gaps.sql
    03_dump_proc_bodies.sql
    03b_bodies_file.sql
    04_port_check.sql
    05_gaps_small.sql

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
