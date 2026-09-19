# اتصال مستقیم اپ ویزیتور به SQL Server (پورت ۱۴۳۳) — نسخهٔ نهایی

این پوشه، **پروژهٔ کامل اندروید آتیران ویزیتور** (نسخهٔ ۲٫۱۳٫۵ — آخرین سورس موجود در مخزن)
است که امکانات زیر به آن اضافه شده تا با **نصب‌کنندهٔ ویندوز `Vizitor-Setup-1.0.0.exe`**
هماهنگ کار کند:

| افزوده | فایل‌ها |
|---|---|
| لایهٔ اتصال مستقیم JDBC روی پورت ۱۴۳۳ | `app/src/main/java/ir/atiran/vizitor/sqldirect/{DirectSql,SqlConnectionManager}.kt` |
| خواندن جدول و ستون‌های ویزیتورها | `.../sqldirect/VisitorRepository.kt` |
| ورود با جدول واقعی کاربران ERP | `.../sqldirect/MeelanoDataSource.kt` (تابع `login` → `dbo.sys_users`) |
| ذخیرهٔ رمزنگاری‌شدهٔ تنظیمات (AES-GCM + Keystore) | `.../sqldirect/SecureDbStore.kt` |
| خواندن کارت اتصال نصب‌کننده (`vizitor://c?…`) | `.../sqldirect/ConnectCards.kt` |
| پل بین اپ و دیتابیس | `.../sqldirect/DirectSqlViewModel.kt` |
| صفحهٔ «اتصال مستقیم SQL» | `app/src/main/java/ir/atiran/vizitor/ui/screens/DirectSqlScreen.kt` |
| ورود به آن صفحه از تنظیمات | `ui/screens/SettingsScreen.kt` + `ui/navigation/VizitorNavigation.kt` |

## مسیر کار در برنامه

۱. **تنظیمات → اتصال مستقیم به SQL Server → «ورود به صفحهٔ اتصال مستقیم SQL»**
۲. متن کارت اتصال نصب‌کننده را می‌چسبانید (`setup\android-connect.txt` یا محتوای
   `android-connect.json`) و «خواندن کارت اتصال» را می‌زنید.
۳. «تست اتصال و فهرست دیتابیس‌ها» → پورت ۱۴۳۳ بررسی و فهرست `sys.databases` گرفته می‌شود.
۴. دیتابیس حسابداری را از فهرست انتخاب **یا دستی تایپ** می‌کنید.
۵. نام کاربری و رمز خودتان (همان کاربر ویزیتور در `dbo.sys_users`) را می‌زنید و
   «اتصال و بارگذاری ویزیتورها» را می‌زنید.

خروجی: وضعیت اتصال، مشخصات سرور، **فهرست ویزیتورهای زیرمجموعهٔ شما** با ستون‌های واقعی
(`vis_name`, `vis_cell`, `vis_tell1`, `vis_addre`, `VIs_region`, `vis_city`, `active`,
`is_supervisor`, `eteb`, `per_p_d_naghd`, `per_p_d_check`, `TedadFactorMojazMande`) و
شمارش دامنهٔ دسترسی (مشتری/کالا/انبار) و همچنین **فهرست کامل ستون‌های جدول `dbo.visitors`**
که از `sys.columns` خوانده می‌شود (ستون `Password` هرگز خوانده یا نمایش داده نمی‌شود).

## دسترسی‌ها (همان چیزی که نصب‌کننده می‌سازد)

* `db_datareader` برای همهٔ خواندن‌ها (`sys_users`, `sys_vis`, `visitors`, `sys_cus`,
  `sys_kal`, `sys_anb`, `CUSTOMERS`, `inventory`, `forosh_price`, …)
* `EXECUTE` روی `dbo.add_sail_pish` (و پروسیجرهای تأییدشدهٔ دیگر)
* `INSERT` روی `dbo.subsailtemp_pish`
* `DELETE`/`UPDATE` عمداً داده نمی‌شود.

## بیلد

```bash
cd vizitor-app
./gradlew :app:assembleDebug :app:assembleRelease
```

خروجی: `app/build/outputs/apk/release/app-release.apk`

بیلد خودکار در CI: ورک‌فلوی `.github/workflows/build-vizitor-app.yml` (JDK 17، ساخت کلید امضا،
`assembleDebug` + `assembleRelease`، بازرسی APK، ثبت گزارش در `vizitor-app/apk-report.txt` و
انتشار ریلیز با APKها). روی خطا، لاگ در `vizitor-app/build-failure.txt` ثبت می‌شود.

## نکته‌های صادقانه

* آخرین سورس اندروید در مخزن **۲٫۱۳٫۵** است؛ فایل `Vizitor-v2.17.1-release.zip` که روی `main`
  گذاشته شد فقط شامل **APK امضاشده با کلید Debug** بود (بدون سورس)، بنابراین همان نسخه
  قابل تغییر نبود و این اپ از سورس موجود ساخته شده است.
* بخش‌های دیگر اپ (ویترین/سبد/گفتگو/گزارش) همان مسیر قبلی خود را دارند؛ این تغییر
  یک مسیر **موازی و بی‌خطر** برای دادهٔ واقعی ERP اضافه می‌کند و به رابط کاربری فعلی دست نمی‌زند.
* پوشه‌های `server/` (وب‌سرویس PHP) و `worker/` (پراکسی هوش مصنوعی) از شاخهٔ مبدأ آورده
  **نشدند**، چون طبق درخواست کارفرما مسیر محصول «اتصال مستقیم، بدون IIS/API» است.

طراحی و برنامه‌نویسی: **میلاد یقوبی** — گروه نرم‌افزاری **Meelano Studio Design**
