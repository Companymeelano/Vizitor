/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | داده نمونه اولیه (Demo Seed)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  فقط در اولین اجرا (حالت دمو) تا زمان اتصال به سرور آتیران کاشته می‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

object SeedData {

    val products = listOf(
        ProductEntity(1, "6260101000011", "روغن موتور ۴ لیتری اسپید", "روانکار", 1_850_000, 42.0, "🛢️"),
        ProductEntity(2, "6260101000028", "فیلتر روغن پراید یورو4", "فیلتر", 320_000, 118.0, "🧰"),
        ProductEntity(3, "6260101000035", "فیلتر هوا سمند ملی", "فیلتر", 285_000, 96.0, "🌬️"),
        ProductEntity(4, "6260101000042", "لنت ترمز جلو پارس", "ترمز", 940_000, 63.0, "🛑"),
        ProductEntity(5, "6260101000059", "تسمه تایم پژو 405", "تسمه", 760_000, 55.0, "⚙️"),
        ProductEntity(6, "6260101000066", "ضدیخ سبز ۴ لیتری", "خنک‌کننده", 420_000, 140.0, "🧪"),
        ProductEntity(7, "6260101000073", "باتری ۶۶ آمپر اتمی", "برق", 4_250_000, 22.0, "🔋", isVip = true),
        ProductEntity(8, "6260101000080", "لاستیک ۱۸۵/۶۵R14 بارز", "لاستیک", 3_180_000, 48.0, "🛞", isVip = true),
        ProductEntity(9, "6260101000097", "شمع سوزنی EF7", "برق", 510_000, 87.0, "⚡"),
        ProductEntity(10, "6260101000103", "دیسک و صفحه والئو ۲۰۶", "کلاچ", 5_600_000, 15.0, "🔩", isVip = true)
    )

    val customers = listOf(
        CustomerEntity(1, "C-1001", "لوازم یدکی برادران رحیمی", "عمده", "تهران", "خیابان ملت، پلاک ۱۲", "09121112233", 35.6892, 51.4310, true, true, 3, 5),
        CustomerEntity(2, "C-1002", "فروشگاه اتولند کرج", "نیمه‌عمده", "کرج", "بلوار طالقانی، نبش ۸", "09123445566", 35.8327, 50.9915, true, false, 12, 30),
        CustomerEntity(3, "C-1003", "مکانیکی استاد اکبر", "خرده", "تهران", "چراغ‌برق، کوچه لاله", "09354556677", 35.6762, 51.4430, false, false, 25, 55),
        CustomerEntity(4, "C-1004", "پخش قطعات آریا", "عمده", "قزوین", "خیابان امام، مجتمع تجاری آریا", "09127778899", 36.2680, 50.0040, true, true, 6, 10),
        CustomerEntity(5, "C-1005", "یدکی سعادت", "خرده", "تهران", "سعادت‌آباد، سرو غربی", "09191234567", 35.7760, 51.3700, false, false, 40, 70),
        CustomerEntity(6, "C-1006", "بازرگانی مهرگان", "عمده", "اصفهان", "خیابان چهارباغ بالا", "09131112244", 32.6546, 51.6680, true, false, 15, 35)
    )

    val salMali = listOf(
        SalMaliHistoryEntity(customerId = 1, productName = "روغن موتور ۴ لیتری اسپید", totalQty = 96.0, yearMonth = "1404-04"),
        SalMaliHistoryEntity(customerId = 1, productName = "فیلتر روغن پراید یورو4", totalQty = 120.0, yearMonth = "1404-04"),
        SalMaliHistoryEntity(customerId = 1, productName = "فیلتر هوا سمند ملی", totalQty = 64.0, yearMonth = "1404-05"),
        SalMaliHistoryEntity(customerId = 2, productName = "لنت ترمز جلو پارس", totalQty = 30.0, yearMonth = "1404-03"),
        SalMaliHistoryEntity(customerId = 2, productName = "تسمه تایم پژو 405", totalQty = 18.0, yearMonth = "1404-04"),
        SalMaliHistoryEntity(customerId = 4, productName = "باتری ۶۶ آمپر اتمی", totalQty = 12.0, yearMonth = "1404-05"),
        SalMaliHistoryEntity(customerId = 4, productName = "لاستیک ۱۸۵/۶۵R14 بارز", totalQty = 24.0, yearMonth = "1404-05"),
        SalMaliHistoryEntity(customerId = 3, productName = "شمع سوزنی EF7", totalQty = 8.0, yearMonth = "1404-02")
    )
}
