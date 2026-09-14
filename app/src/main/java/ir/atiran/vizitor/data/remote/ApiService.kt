/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | سرویس شبکه (Retrofit Interface)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تمامی اندپوینت‌ها به فایل api.php سمت سرور متصل هستند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface VizitorApiService {

    /** تست اتصال به سرور. */
    @GET("api.php")
    suspend fun ping(
        @Header("X-Api-Key") apiKey: String,
        @Query("action") action: String = "ping"
    ): ApiEnvelope<Map<String, String>>

    /** دریافت کاتالوگ کالا (Live Stock). */
    @GET("api.php")
    suspend fun getCatalog(
        @Header("X-Api-Key") apiKey: String,
        @Query("action") action: String = "catalog",
        @Query("since") since: Long = 0
    ): ApiEnvelope<List<ProductDto>>

    /** دریافت لیست مشتریان + گروه‌های مشتری (CustGroup). */
    @GET("api.php")
    suspend fun getCustomers(
        @Header("X-Api-Key") apiKey: String,
        @Query("action") action: String = "customers",
        @Query("since") since: Long = 0
    ): ApiEnvelope<List<CustomerDto>>

    /** تاریخچه سال مالی برای تحلیل هوش مصنوعی. */
    @GET("api.php")
    suspend fun getSalMali(
        @Header("X-Api-Key") apiKey: String,
        @Query("action") action: String = "sal_mali",
        @Query("customer_id") customerId: Int
    ): ApiEnvelope<List<SalMaliRowDto>>

    /** صدور فاکتور — تراکنش اتمیک روی SalesHeader + SalesLines. */
    @POST("api.php?action=submit_invoice")
    suspend fun submitInvoice(
        @Header("X-Api-Key") apiKey: String,
        @Body invoice: InvoiceHeaderRequest
    ): ApiEnvelope<SubmitInvoiceResponse>
}
