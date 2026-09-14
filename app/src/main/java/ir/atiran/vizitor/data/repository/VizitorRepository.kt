/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | مخزن اصلی داده (Offline-First Repository)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  استراتژی: خواندن همیشه از Room، نوشتن ابتدا در Room و سپس سینک با
 *  سرور توسط SyncWorker — اپ در بی‌شبکه‌ترین شرایط هم کامل کار می‌کند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.repository

import android.content.Context
import android.util.Base64
import ir.atiran.vizitor.data.local.AppDatabase
import ir.atiran.vizitor.data.local.CartItemEntity
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.data.local.InvoiceItemEntity
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.data.local.SalMaliHistoryEntity
import ir.atiran.vizitor.data.local.SeedData
import ir.atiran.vizitor.data.local.TopProduct
import ir.atiran.vizitor.data.remote.InvoiceHeaderRequest
import ir.atiran.vizitor.data.remote.InvoiceLineRequest
import ir.atiran.vizitor.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class VizitorRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val settings = SettingsRepository(context)

    val products = db.products().observeAll()
    val customers = db.customers().observeAll()
    val followUpCustomers = db.customers().observeNeedsFollowUp()
    val cartItems = db.cart().observeAll()
    val invoices = db.invoices().observeAll()
    val pendingCount = db.invoices().observePendingCount()
    val config = settings.config

    /** جمع ناخالص سبد. */
    val cartTotal: Flow<Long> = db.cart().observeTotal()

    /** تارگت فروش روزانه (ریال) — در حالت واقعی از سرور می‌آید. */
    val dailyTarget: Long = 250_000_000L

    /** پورسانت لحظه‌ای: ۲٫۵٪ فروش امروز. */
    val todayCommission: Flow<Long> = db.invoices()
        .observeSalesSince(startOfToday())
        .map { it * 25 / 1000 }

    val todaySales: Flow<Long> = db.invoices().observeSalesSince(startOfToday())

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ── راه‌اندازی اولیه (دمو) ──────────────────────────────────────────────
    suspend fun ensureSeeded() {
        if (db.products().getAll().isEmpty()) db.products().upsertAll(SeedData.products)
        if (db.customers().getAll().isEmpty()) db.customers().upsertAll(SeedData.customers)
        if (db.salMali().forCustomer(1).isEmpty()) db.salMali().upsertAll(SeedData.salMali)
    }

    // ── سبد خرید ────────────────────────────────────────────────────────────
    suspend fun addToCart(product: ProductEntity, qty: Double = 1.0) {
        val current = db.cart().getAll().firstOrNull { it.productId == product.id }
        val newQty = ((current?.quantity ?: 0.0) + qty).coerceAtMost(product.stock)
        if (newQty <= 0) return
        db.cart().upsert(
            CartItemEntity(product.id, product.name, product.price, newQty, product.stock)
        )
    }

    suspend fun decrementCart(productId: Int) {
        val item = db.cart().getAll().firstOrNull { it.productId == productId } ?: return
        if (item.quantity <= 1) db.cart().deleteByProduct(productId)
        else db.cart().upsert(item.copy(quantity = item.quantity - 1))
    }

    suspend fun removeFromCart(productId: Int) = db.cart().deleteByProduct(productId)

    suspend fun findByBarcode(code: String): ProductEntity? = db.products().findByBarcode(code)

    /**
     * محاسبه خودکار کسورات:
     *  — خرید بالای ۵۰ میلیون ریال: ۳٪ تخفیف حجمی
     *  — تسویه نقدی (اعلامی کاربر): ۲٪ دیگر
     */
    fun computeDiscount(gross: Long, cashSettlement: Boolean): Long {
        var discount = 0L
        if (gross >= 50_000_000L) discount += gross * 3 / 100
        if (cashSettlement) discount += gross * 2 / 100
        return discount
    }

    /** صدور فاکتور: ابتدا ذخیره محلی (آفلاین-اول)، سپس تلاش برای ارسال آنی. */
    suspend fun issueInvoice(
        customer: CustomerEntity?,
        signaturePng: ByteArray?,
        cashSettlement: Boolean
    ): InvoiceEntity {
        val items = db.cart().getAll()
        require(items.isNotEmpty()) { "سبد سفارش خالی است" }
        val gross = items.sumOf { it.quantity.toLong() * it.unitPrice }
        val discount = computeDiscount(gross, cashSettlement)
        val sig = signaturePng?.let {
            Base64.encodeToString(it, Base64.NO_WRAP)
        }
        val headerId = db.invoices().insertHeader(
            InvoiceEntity(
                customerId = customer?.id ?: 0,
                customerName = customer?.name ?: "مشتری متفرقه",
                grossAmount = gross,
                discount = discount,
                finalAmount = gross - discount,
                signatureBase64 = sig
            )
        )
        db.invoices().insertItems(
            items.map {
                InvoiceItemEntity(
                    invoiceId = headerId,
                    productId = it.productId,
                    productName = it.productName,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    lineTotal = (it.quantity * it.unitPrice).toLong()
                )
            }
        )
        // کسر خوش‌بینانه موجودی زنده
        items.forEach {
            val p = db.products().getById(it.productId)
            if (p != null) db.products().updateStock(p.id, (p.stock - it.quantity).coerceAtLeast(0.0))
        }
        db.cart().clear()
        return db.invoices().getById(headerId)!!
    }

    // ── همگام‌سازی با سرور آتیران ────────────────────────────────────────────
    suspend fun syncAll(): SyncReport {
        val cfg = settings.config.first()
        val api = RetrofitClient.buildApi(cfg.baseUrl)
        var pushed = 0; var pulled = 0; val errors = mutableListOf<String>()

        // ۱) ارسال فاکتورهای در انتظار
        db.invoices().getPending().forEach { invoice ->
            try {
                val items = db.invoices().getItems(invoice.id).map {
                    InvoiceLineRequest(it.productId, it.quantity, it.unitPrice, it.lineTotal)
                }
                val res = api.submitInvoice(
                    cfg.apiKey,
                    InvoiceHeaderRequest(
                        invoice.customerId, invoice.grossAmount, invoice.discount,
                        invoice.finalAmount, invoice.signatureBase64, items
                    )
                )
                if (res.success && res.data != null) {
                    db.invoices().updateHeader(
                        invoice.copy(status = InvoiceStatus.SYNCED, serverId = res.data.invoiceNo)
                    )
                    pushed++
                } else {
                    db.invoices().updateHeader(invoice.copy(status = InvoiceStatus.FAILED))
                    errors += res.message ?: "خطای نامشخص سرور"
                }
            } catch (e: Exception) {
                db.invoices().updateHeader(invoice.copy(status = InvoiceStatus.FAILED))
                errors += (e.message ?: "خطای شبکه")
            }
        }

        // ۲) دریافت کاتالوگ و مشتریان
        try {
            val cat = api.getCatalog(cfg.apiKey)
            if (cat.success && cat.data != null) {
                db.products().upsertAll(cat.data.map {
                    ProductEntity(it.id, it.code, it.name, it.groupName, it.price, it.stock, isVip = it.isVip)
                })
                pulled += cat.data.size
            }
        } catch (e: Exception) { errors += "کاتالوگ: ${e.message}" }

        try {
            val cus = api.getCustomers(cfg.apiKey)
            if (cus.success && cus.data != null) {
                db.customers().upsertAll(cus.data.map {
                    CustomerEntity(
                        it.id, it.code, it.name, it.groupName, it.city, it.address, it.phone,
                        it.lat, it.lng, it.creditOk, it.isVip, it.lastPurchaseDays, it.dropPercent
                    )
                })
                pulled += cus.data.size
            }
        } catch (e: Exception) { errors += "مشتریان: ${e.message}" }

        settings.markSynced()
        return SyncReport(pushed, pulled, errors)
    }

    suspend fun fetchSalMaliFor(customerId: Int): List<SalMaliHistoryEntity> {
        val cfg = settings.config.first()
        return try {
            val api = RetrofitClient.buildApi(cfg.baseUrl)
            val res = api.getSalMali(cfg.apiKey, customerId = customerId)
            if (res.success && res.data != null) {
                val rows = res.data.map {
                    SalMaliHistoryEntity(customerId = it.customerId, productName = it.productName, totalQty = it.totalQty, yearMonth = it.yearMonth)
                }
                db.salMali().clearForCustomer(customerId)
                db.salMali().upsertAll(rows)
                rows
            } else db.salMali().forCustomer(customerId)
        } catch (e: Exception) {
            db.salMali().forCustomer(customerId) // بازگشت به نسخه کش‌شده آفلاین
        }
    }

    suspend fun salMaliFor(customerId: Int): List<SalMaliHistoryEntity> =
        db.salMali().forCustomer(customerId)

    suspend fun topProducts(): List<TopProduct> = db.invoices().topProducts()

    suspend fun itemsFor(invoiceId: Long): List<InvoiceItemEntity> =
        db.invoices().getItems(invoiceId)

    suspend fun saveConfig(config: ServerConfig) = settings.save(config)

    suspend fun testConnection(): Result<String> {
        val cfg = settings.config.first()
        return try {
            val api = RetrofitClient.buildApi(cfg.baseUrl)
            val res = api.ping(cfg.apiKey)
            if (res.success) Result.success(res.data?.get("db") ?: "OK")
            else Result.failure(RuntimeException(res.message ?: "پاسخ نامعتبر"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SyncReport(val pushedInvoices: Int, val pulledRecords: Int, val errors: List<String>) {
    val summary: String
        get() = "ارسال فاکتور: $pushedInvoices | رکورد دریافتی: $pulledRecords" +
                if (errors.isEmpty()) " | بدون خطا ✅" else " | خطاها: ${errors.joinToString("، ")}"
}
