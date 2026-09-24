package ir.g1z4.controlpro.protocol

import java.util.Calendar
import java.util.TimeZone

object FaFormat {
    private val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun digits(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            if (ch in '0'..'9') append(persian[ch - '0']) else append(ch)
        }
    }

    fun digits(value: Int): String = digits(value.toString())

    fun clock(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = epochMs }
        val h = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = c.get(Calendar.MINUTE).toString().padStart(2, '0')
        return digits("$h:$m")
    }

    fun dateTime(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = epochMs }
        val j = Jalali.fromGregorian(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
        return digits("${j.year}/${j.month.toString().padStart(2, '0')}/${j.day.toString().padStart(2, '0')} ${clock(epochMs, zone)}")
    }

    fun dayTitle(epochMs: Long, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = epochMs }
        val n = Calendar.getInstance(zone).apply { timeInMillis = now }
        val sameDay = c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "امروز"
        n.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = c.get(Calendar.YEAR) == n.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR)
        if (yesterday) return "دیروز"
        val j = Jalali.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        return digits("${j.day} ${Jalali.monthName(j.month)} ${j.year}")
    }
}
