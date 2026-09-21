/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تاریخ/ساعت شمسی برای جدول dbo.Visit
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  ستون‌های DateCreated/TimeCreated جدول Visit در خودِ ERP به‌صورت رشتهٔ
 *  شمسی نگه داشته می‌شوند (قالب هم‌سان تاریخ‌های فاکتور: 1405/06/27).
 *  اینجا با تقویم رسمیِ خودِ پلتفرم اندروید (ICU / PersianCalendar) تاریخ
 *  شمسی می‌سازیم — الگوریتم دست‌نویس نداریم و از خطای تبدیل جلوگیری می‌شود.
 *  ساعت به قالب ۲۴ ساعتهٔ HH:mm:ss است.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * تاریخ و ساعت جاری به دو رشتهٔ قابل درج در dbo.Visit:
 *  • [dateText] : yyyy/MM/dd شمسی (مثلاً 1405/06/27)
 *  • [timeText] : HH:mm:ss (مثلاً 13:05:42)
 */
object VisitDate {

    data class Now(
        val dateText: String,
        val timeText: String,
    )

    fun now(): Now {
        return try {
            val greg = java.util.GregorianCalendar.getInstance()
            val persian = android.icu.util.PersianCalendar.getInstance()
            persian.set(greg.time)
            val y = persian.get(android.icu.util.PersianCalendar.YEAR)
            val m = persian.get(android.icu.util.PersianCalendar.MONTH) + 1
            val d = persian.get(android.icu.util.PersianCalendar.DAY_OF_MONTH)
            val hh = greg.get(java.util.GregorianCalendar.HOUR_OF_DAY)
            val mi = greg.get(java.util.GregorianCalendar.MINUTE)
            val ss = greg.get(java.util.GregorianCalendar.SECOND)
            Now(
                dateText = String.format(Locale.US, "%04d/%02d/%02d", y, m, d),
                timeText = String.format(Locale.US, "%02d:%02d:%02d", hh, mi, ss),
            )
        } catch (_: Throwable) {
            // جایگزین امن (هرگز نباید رخ دهد): قالب میلادی همان ستون‌ها
            val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
            val s = fmt.format(Date())
            Now(dateText = s.take(10), timeText = s.substring(11))
        }
    }

    /** فقط تاریخ شمسی امروز (برای شمارش «تعداد ویزیت امروز»). */
    fun todayJalali(): String = now().dateText
}
